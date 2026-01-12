package com.erp.oms.dto.viewObject.CompositeVO;

import com.erp.oms.dto.viewObject.OrderItemVO;
import com.erp.oms.dto.viewObject.OrderVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

// 订单组合 VO
@EqualsAndHashCode(callSuper = true)
@Data
public class OrderFullVO extends OrderVO {
    private List<OrderItemVO> items; // 关联的订单明细
}