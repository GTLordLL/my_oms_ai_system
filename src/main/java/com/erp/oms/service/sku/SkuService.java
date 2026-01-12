package com.erp.oms.service.sku;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.oms.dto.stockManager.PurchaseInboundDTO;
import com.erp.oms.dto.api.PageResultVO;
import com.erp.oms.dto.pageQuery.SkuQueryDTO;
import com.erp.oms.dto.stockManager.RefundInboundDTO;
import com.erp.oms.dto.update.SkuUpdateDTO;
import com.erp.oms.dto.viewObject.SkuVO;
import com.erp.oms.entity.Sku;

import java.util.List;


// 接口
public interface SkuService extends IService<Sku> {
    PageResultVO<SkuVO> pageByQuery(SkuQueryDTO query);
    void updateSku(SkuUpdateDTO dto);
    void safeDeleteSku(Long skuId);
    void handlePurchaseInbound(PurchaseInboundDTO dto);
    void handleRefundInbound(RefundInboundDTO dto);
    List<SkuVO> listByProductId(Long productId);
}

