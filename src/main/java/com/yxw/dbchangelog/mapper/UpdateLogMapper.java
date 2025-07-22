package com.yxw.dbchangelog.mapper;

import com.yxw.dbchangelog.model.UpdateLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UpdateLogMapper {
    int insertLog(UpdateLog log);
}
