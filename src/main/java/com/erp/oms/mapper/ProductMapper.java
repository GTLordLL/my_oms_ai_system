package com.erp.oms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.oms.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    // 基础 CRUD 由 BaseMapper 提供
}

