package com.erp.oms.service.sku;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.oms.dto.query.SkuQuery;
import com.erp.oms.entity.Sku;
import com.erp.oms.exception.BizException;
import com.erp.oms.mapper.SkuMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

// 实现类
@Service
public class SkuServiceImpl extends ServiceImpl<SkuMapper, Sku> implements SkuService {
    @Override
    public IPage<Sku> pageByQuery(SkuQuery query) {

        Page<Sku> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<Sku> w = new LambdaQueryWrapper<>();

        if (query.getProductId() != null) {
            w.eq(Sku::getProductId, query.getProductId());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            w.and(q -> q.like(Sku::getSkuCode, query.getKeyword())
                    .or().like(Sku::getSpecs, query.getKeyword()));
        }
        if (Boolean.TRUE.equals(query.getLowStock())) {
            w.apply("stock_quantity <= alert_quantity");
        }

        w.orderByDesc(Sku::getId);

        return this.page(page, w);
    }

    @Override
    public Sku getByIdOrThrow(Long id) {
        Sku sku = this.getById(id);
        if (sku == null) {
            throw BizException.notFound("SKU 不存在");
        }
        return sku;
    }

    @Override
    public Sku create(Sku sku) {
        this.save(sku);
        return sku;
    }

    @Override
    public Sku updateSku(Long id, Sku sku) {
        sku.setId(id);
        if (!this.updateById(sku)) {
            throw BizException.notFound("SKU 不存在");
        }
        return this.getById(id);
    }

    @Override
    public void deleteById(Long id) {
        if (!this.removeById(id)) {
            throw BizException.notFound("SKU 不存在");
        }
    }

}