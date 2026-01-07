package com.erp.oms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.oms.entity.StockLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockLogMapper extends BaseMapper<StockLog> {
    // 基础的增删改查已经由 BaseMapper 自动提供了
}
