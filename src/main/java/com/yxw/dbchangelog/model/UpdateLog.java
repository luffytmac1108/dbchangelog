package com.yxw.dbchangelog.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class UpdateLog {
    private Long id;
    private String tableName;
    private Long primaryId;
    private String commandType;
    private String finalSql;
    private String params;
    private Integer rowAffect;
    private Date updateTime;
}