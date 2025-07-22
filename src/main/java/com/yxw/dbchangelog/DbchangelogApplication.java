package com.yxw.dbchangelog;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement // 开启事务管理
@MapperScan("com.yxw.dbchangelog.mapper") // 扫描 MyBatis Mapper 接口
@EnableAspectJAutoProxy // 启用 Spring AOP 代理
@EnableAsync // 启用 @Async 注解，通常和 AsyncConfig 一起使用
public class DbchangelogApplication {

	public static void main(String[] args) {
		SpringApplication.run(DbchangelogApplication.class, args);
	}
}
