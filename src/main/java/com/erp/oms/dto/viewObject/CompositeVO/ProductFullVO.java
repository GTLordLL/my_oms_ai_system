package com.erp.oms.dto.viewObject.CompositeVO;

import com.erp.oms.dto.viewObject.ProductVO;
import com.erp.oms.dto.viewObject.SkuVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

// 商品组合 VO
@EqualsAndHashCode(callSuper = true)
@Data
public class ProductFullVO extends ProductVO {
    private List<SkuVO> skus; // 关联的 SKU 列表
}