# dbchangelog
利用mytatis的拦截器处理db的update操作，使用ThreadLocal+aop+TransactionSynchronizationManager+TransactionSynchronizationAdapter进行无侵入实现记录变更日志

# db配置
application.yml中已经有db相关的配置，在mysql中执sql文件夹里面的脚本即可

# postman 验证功能
用来验证功能的请求和参数已经放到resource下的postman文件夹中了，导入到postman中即可使用

# 大致功能说明
**TransactionAwareUpdateLogHolder**:
利用ThreadLocal，保存当前线程中的事务要update的表相关内容，可以组装成log

**UpdateLogInterceptorAdvance**:
拦截mybatis的update操作，获取执行的sql和参数，调用TransactionAwareUpdateLogHolder中的addLog的方法暂存需要记录到日志表中的相关数据

**UpdateLogAspect**:
AOP切面，这里织入的是我们的 @Pointcut("@annotation(org.springframework.transaction.annotation.Transactional)") 事物方法，在事务提交或者回滚的时候，从TransactionAwareUpdateLogHolder获取当前线程里面需要写入db的log

详细的逻辑看代码即可，代码里面有比较详细的注释

**只做了一些基本的测试，出问题概不负责，哈哈哈哈哈**
