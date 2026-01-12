package com.erp.oms.service.orderItem;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.oms.dto.api.PageResultVO;
import com.erp.oms.dto.pageQuery.OrderItemQueryDTO;
import com.erp.oms.dto.viewObject.OrderItemVO;
import com.erp.oms.entity.OrderItem;

import java.util.List;

public interface OrderItemService extends IService<OrderItem> {
    PageResultVO<OrderItemVO> pageByQuery(OrderItemQueryDTO query);
    List<OrderItemVO> listByOrderId(Long orderId);
}
