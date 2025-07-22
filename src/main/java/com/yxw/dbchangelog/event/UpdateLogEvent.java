package com.yxw.dbchangelog.event;

import com.yxw.dbchangelog.model.UpdateLog;
import org.springframework.context.ApplicationEvent;

/**
 * 产品更新日志事件。
 * 携带要记录的日志信息。
 */
public class UpdateLogEvent extends ApplicationEvent {

    private final UpdateLog log;

    public UpdateLogEvent(Object source, UpdateLog log) {
        super(source);
        this.log = log;
    }

    public UpdateLog getLog() {
        return log;
    }

    @Override
    public String toString() {
        return "ProductUpdateLogEvent{" +
                "log=" + log +
                '}';
    }
}
