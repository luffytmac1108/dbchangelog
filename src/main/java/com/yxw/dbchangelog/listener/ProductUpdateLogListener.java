package com.yxw.dbchangelog.listener;

import com.yxw.dbchangelog.event.UpdateLogEvent;
import com.yxw.dbchangelog.mapper.UpdateLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 产品更新日志事件监听器。
 * 异步地将日志保存到数据库。
 * 可以增加重试和更健壮的错误处理。
 */
@Slf4j
@Component
public class ProductUpdateLogListener {

    private final UpdateLogMapper logMapper;

    public ProductUpdateLogListener(UpdateLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    @EventListener // 标记为事件监听器
    @Async         // 使该方法异步执行
    public void handleProductUpdateLogEvent(UpdateLogEvent event) {
        try {
            // 真正执行日志的数据库插入操作
            logMapper.insertLog(event.getLog());
        } catch (Exception e) {
            // 异步日志记录失败的处理：
            // 1. 记录到错误日志文件（例如使用 SLF4J 或 Logback）
            // 2. 将失败的日志事件发送到消息队列的死信队列 (DLQ)，以便后续人工干预或监控
            // 3. 如果启用了 @Retryable，这里是最终失败后的处理
            log.error("[Log Listener] Failed to insert update log, Error: " + e);
            e.printStackTrace();
            // 考虑发送告警通知 (例如邮件、短信)
        }
    }
}
