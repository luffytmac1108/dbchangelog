package com.yxw.dbchangelog.aspect;

import com.yxw.dbchangelog.event.UpdateLogEvent;
import com.yxw.dbchangelog.model.UpdateLog;
import com.yxw.dbchangelog.util.JsonUtils;
import com.yxw.dbchangelog.util.TransactionAwareUpdateLogHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * AOP 切面，用于在业务方法事务成功提交后发布更新日志事件。
 * 它从 TransactionAwareUpdateLogHolder 获取在事务过程中收集的日志信息。
 */
@Slf4j
@Aspect
@Component
public class UpdateLogAspect {

    private final ApplicationEventPublisher eventPublisher;

    public UpdateLogAspect(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    // 定义切点：所有带有 @Transactional 注解的服务层方法
    @Pointcut("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void transactionalServiceMethods() {}

    /**
     * 在事务性方法成功返回后执行。
     * 此时事务已经提交（或即将提交），发布日志事件。
     */
    @AfterReturning(pointcut = "transactionalServiceMethods()")
    public void afterTransactionSuccess(JoinPoint joinPoint) {
        log.info("----- Transaction success, update logs will be send as event.");
        // 获取当前事务中收集的所有日志信息
        List<Map<String, Object>> logs = TransactionAwareUpdateLogHolder.getLogs();
        if(logs.isEmpty()){
            return;
        }
        for (Map<String, Object> logInfo : logs) {
            try {
                // 这些数据由 Service 层或 MyBatis 拦截器填充
                Long primaryId = (Long) logInfo.get("primaryId");
                String sqlCommandType = (String) logInfo.get("sqlCommandType");
                String finalSql = (String) logInfo.get("finalSql");
                String tableName = (String) logInfo.get("tableName");
                String parameters = JsonUtils.toJson(logInfo.get("parameters"));
                Integer rowsAffected = (Integer) logInfo.get("rowsAffected");
                UpdateLog updateLog = UpdateLog.builder().primaryId(primaryId).commandType(sqlCommandType)
                        .finalSql(finalSql).tableName(tableName).params(parameters).rowAffect(rowsAffected)
                        .updateTime(new Date()).build();
                // 发布事件，让异步监听器处理日志记录
                eventPublisher.publishEvent(new UpdateLogEvent(this, updateLog));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 清理 ThreadLocal，尽管 TransactionAwareUpdateLogHolder 的 afterCompletion 也会做
        TransactionAwareUpdateLogHolder.clearLogs();
    }

    /**
     * 在事务性方法抛出异常（事务将回滚）后执行。
     */
    @AfterThrowing(pointcut = "transactionalServiceMethods()", throwing = "ex")
    public void afterTransactionFailure(JoinPoint joinPoint, Throwable ex) {
        // 事务回滚，TransactionAwareUpdateLogHolder 中的数据将被清理
        log.info("----- Transaction failed, update logs will be cleared.");
        TransactionAwareUpdateLogHolder.clearLogs();
    }
}