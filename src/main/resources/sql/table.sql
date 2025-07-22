CREATE DATABASE `dbchangelog` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

use dbchangelog;

-- 产品表
drop table if exists products;
CREATE TABLE IF NOT EXISTS products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '产品ID，主键自增',
    name VARCHAR(255) NOT NULL COMMENT '产品名称',
    price DECIMAL(10, 2) NOT NULL COMMENT '产品价格',
    stock INT NOT NULL COMMENT '产品库存数量',
    updated_by VARCHAR(32) DEFAULT NULL COMMENT '操作更新的用户或系统标识',
    last_update_time timestamp DEFAULT NULL COMMENT '最后更新时间'
) COMMENT '产品信息表';

-- 日志表
drop table if exists update_logs;
CREATE TABLE IF NOT EXISTS update_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID，主键自增',
    table_name varchar(64) DEFAULT null comment '操作的哪张表',
    primary_id BIGINT DEFAULT NULL COMMENT '操作数据的主键ID',
    command_type varchar(32) DEFAULT null comment '操作命令',
    final_sql varchar(1024) DEFAULT null comment '最终sql',
    params text DEFAULT null comment '参数',
    row_affect int(11) DEFAULT null comment '影响行数',
    update_time timestamp DEFAULT NULL COMMENT '日志记录时间'
) COMMENT '操作日志表';