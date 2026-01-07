package com.erp.oms.service.sku;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.oms.dto.query.SkuQuery;
import com.erp.oms.entity.Sku;


// 接口
public interface SkuService extends IService<Sku> {
    IPage<Sku> pageByQuery(SkuQuery query);
    Sku getByIdOrThrow(Long id);
    Sku create(Sku sku);
    Sku updateSku(Long id, Sku sku);
    void deleteById(Long id);
}

