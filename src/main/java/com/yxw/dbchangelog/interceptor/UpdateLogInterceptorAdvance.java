package com.yxw.dbchangelog.interceptor;

import com.yxw.dbchangelog.util.TransactionAwareUpdateLogHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
})
@Slf4j
@Component
public class UpdateLogInterceptorAdvance implements Interceptor {

    // 正则表达式，用于匹配 UPDATE 语句中的表名
    private static final Pattern UPDATE_TABLE_PATTERN = Pattern.compile("^\\s*UPDATE\\s+(`?)([a-zA-Z0-9_]+)(`?)\\s+SET", Pattern.CASE_INSENSITIVE);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];
        String sqlCommandType = mappedStatement.getSqlCommandType().name();
        // 仅处理 UPDATE 操作
        if (!"UPDATE".equalsIgnoreCase(sqlCommandType)) {
            // 非更新操作直接放行
            return invocation.proceed();
        }
        BoundSql boundSql = mappedStatement.getBoundSql(parameter);
        Configuration configuration = mappedStatement.getConfiguration();
        // --- 获取最终执行的SQL (参数拼接后的SQL) ---
        String finalSql = showSql(configuration, boundSql);
        log.info("----- [MyBatis Interceptor] Final SQL: {}", finalSql);
        // --- 获取 UPDATE 操作的表名 ---
        String tableName = getTableNameFromUpdateSql(boundSql.getSql());
        if (tableName != null) {
            log.info("----- [MyBatis Interceptor] Updated Table Name: {}", tableName);
        } else {
            log.info("----- [MyBatis Interceptor] Could not determine table name for SQL: {}", boundSql.getSql());
        }
        // 执行更新操作,result 是受影响的行数 (Integer)
        Object result = invocation.proceed();
        int rowsAffected = (Integer) result;
        log.info("----- [MyBatis Interceptor] Rows Affected: {}", rowsAffected);

        // 仅在当前存在活动事务时，且更新影响行数大于0，才将信息添加到事务感知的持有者中
        if (rowsAffected > 0) {
            Map<String, Object> logInfo = new HashMap<>();
            logInfo.put("sqlCommandType", sqlCommandType);
            // 将拼接后的SQL放入日志信息
            logInfo.put("finalSql", finalSql);
            // 将表名放入日志信息
            logInfo.put("tableName", tableName);
            // 传递原始参数，AOP可能需要
            logInfo.put("parameters", parameter);
            logInfo.put("rowsAffected", rowsAffected);
            try {
                // 尝试从参数中提取主键，但通用性较差
                if (parameter != null) {
                    Long id = (Long) parameter.getClass().getMethod("getId").invoke(parameter);
                    logInfo.put("primaryId", id);
                }
            } catch (Exception e) {
                log.error("----- [MyBatis Interceptor] Could not extract primary key from parameter for logging.");
            }
            // 添加到事务感知的持有者
            // 注意：Service层添加的业务上下文信息 (oldPrice, newPrice, updatedBy等)
            // 和这里添加的SQL层面的信息会在AOP中合并或分别处理。
            // 这里只添加拦截器能获取到的信息。
            TransactionAwareUpdateLogHolder.addLog(logInfo);
        }
        return result;
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof Executor) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

    @Override
    public void setProperties(Properties properties) {
        // 可选：可以设置一些属性
    }

    /**
     * 格式化参数，获取最终执行的SQL语句
     */
    private String showSql(Configuration configuration, BoundSql boundSql) {
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        // 去除多余空格，并将换行符替换为单个空格，使得SQL在一行显示
        String sql = boundSql.getSql().replaceAll("[\\s]+", " ");

        if (parameterMappings != null && !parameterMappings.isEmpty()) {
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(parameterObject)));
            } else {
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                for (ParameterMapping parameterMapping : parameterMappings) {
                    String propertyName = parameterMapping.getProperty();
                    if (metaObject.hasGetter(propertyName)) {
                        Object obj = metaObject.getValue(propertyName);
                        sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(obj)));
                    } else if (boundSql.hasAdditionalParameter(propertyName)) {
                        Object obj = boundSql.getAdditionalParameter(propertyName);
                        sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(obj)));
                    } else {
                        sql = sql.replaceFirst("\\?", "缺失");
                    }
                }
            }
        }
        return sql;
    }

    /**
     * 获取参数值，并根据类型进行格式化
     */
    private String getParameterValue(Object obj) {
        String value = "";
        if (obj instanceof String) {
            value = "'" + obj.toString() + "'";
        } else if (obj instanceof Date) {
            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.CHINA);
            value = "'" + formatter.format(new Date()) + "'";
        } else if (obj != null) {
            value = obj.toString();
        } else {
            value = "null";
        }
        return value;
    }

    /**
     * 从 UPDATE SQL 语句中提取表名。
     * 适用于简单的 UPDATE table_name SET ... 形式。
     */
    private String getTableNameFromUpdateSql(String sql) {
        Matcher matcher = UPDATE_TABLE_PATTERN.matcher(sql);
        if (matcher.find()) {
            // 捕获组2是表名
            return matcher.group(2);
        }
        return null;
    }
}