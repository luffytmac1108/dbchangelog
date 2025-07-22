package com.yxw.dbchangelog.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 事务感知的更新日志信息持有者。
 * 用于在业务代码或MyBatis拦截器中收集更新数据，并在事务提交后统一处理。
 */
@Slf4j
public class TransactionAwareUpdateLogHolder {

    // 使用 ThreadLocal 存储当前事务的更新日志列表，线程隔离
    // List<Map<String, Object>> 用于存储每条更新操作的上下文信息
    private static final ThreadLocal<List<Map<String, Object>>> TRANSACTION_LOGS = ThreadLocal.withInitial(ArrayList::new);

    // 用于在 TransactionSynchronizationManager 中标记同步器是否已注册的唯一键
    private static final String SYNCHRONIZATION_REGISTERED_KEY = "updateLogSynchronizationRegistered";

    /**
     * 添加一条更新日志信息到当前事务。
     * 应该在事务内部调用。
     * @param logInfo 包含更新数据的Map
     */
    public static void addLog(Map<String, Object> logInfo) {

        /**
         * 简要说明：
         * TransactionSynchronizationManager 的事务绑定：
         * TransactionSynchronizationManager.registerSynchronization() 方法会将 TransactionSynchronizationAdapter 实例绑定到当前线程正在进行的事务上。
         * 这意味着，事务 A 注册的同步器只会在事务 A 完成时触发 afterCompletion。事务 B 注册的同步器只会在事务 B 完成时触发 afterCompletion。
         * if (!TransactionSynchronizationManager.hasResource(SYNCHRONIZATION_REGISTERED_KEY)) 这个判断确保了在同一个事务（即同一个线程）内，TransactionSynchronizationAdapter 只会被注册一次，避免了重复注册的开销。
         * TransactionSynchronizationManager.bindResource() 和 unbindResourceIfPossible() 也是线程和事务绑定的，它们操作的资源也是当前事务的上下文，不会影响其他并行事务。
         */

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            List<Map<String, Object>> logs = TRANSACTION_LOGS.get();
            logs.add(logInfo);

            // 检查是否已为当前事务注册了同步器标志
            // 如果 hasResource(SYNCHRONIZATION_REGISTERED_KEY) 返回 false，说明是第一次注册
            // 如果当前现场操作多个update语句，那么会进入addLog方法多次，但是同步器只会注册一次，避免重复注册
            if (!TransactionSynchronizationManager.hasResource(SYNCHRONIZATION_REGISTERED_KEY)) {
                log.info("----- Registering new TransactionSynchronization for update logs.");
                //注册一个新的事务同步器，这个同步器会在事务完成后负责清理 ThreadLocal。
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCompletion(int status) {
                        // 事务完成后（提交或回滚）清理 ThreadLocal
                        TRANSACTION_LOGS.remove();
                        // 解绑注册标志，确保事务结束时状态被重置，避免内存泄漏
                        TransactionSynchronizationManager.unbindResourceIfPossible(SYNCHRONIZATION_REGISTERED_KEY);
                        log.info("----- TransactionAwareUpdateLogHolder afterCompletion cleaned for transaction status: {}", status);
                    }
                });
                // 绑定一个资源（这里是一个简单的布尔值）作为“已注册”的标志
                TransactionSynchronizationManager.bindResource(SYNCHRONIZATION_REGISTERED_KEY, Boolean.TRUE);
            }
        } else {
            // 如果没有活动事务，这里日志将无法被AOP感知并统一处理
            log.info("----- Warning: addLog called outside of an active transaction. Log info: {}", logInfo);
            // 生产环境中，此处可能抛出 IllegalStateException 强制要求在事务中操作
            // throw new IllegalStateException("addLog must be called within an active transaction.");
        }
    }

    /**
     * 获取当前事务的所有更新日志信息。
     * 只有在事务完成前（例如AOP的@AfterReturning）调用才有效。
     * @return 当前事务的更新日志列表
     */
    public static List<Map<String, Object>> getLogs() {
        return TRANSACTION_LOGS.get();
    }

    /**
     * 清理当前事务的日志信息和注册标志。
     * 通常由 TransactionSynchronizationManager 自动在 afterCompletion 调用，
     * 但如果需要手动强制清理（例如在测试环境中），也可以调用。
     */
    public static void clearLogs() {
        TRANSACTION_LOGS.remove();
        // 确保也解绑注册标志
        TransactionSynchronizationManager.unbindResourceIfPossible(SYNCHRONIZATION_REGISTERED_KEY);
        log.info("----- Manual cleanup of TransactionAwareUpdateLogHolder performed.");
    }
}