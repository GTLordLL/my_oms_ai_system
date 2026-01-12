package com.erp.oms.service.order;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.oms.dto.add.OrderCreateDTO;
import com.erp.oms.dto.api.PageResultVO;
import com.erp.oms.dto.pageQuery.OrderQueryDTO;
import com.erp.oms.dto.update.OrderUpdateDTO;
import com.erp.oms.dto.viewObject.CompositeVO.OrderFullVO;
import com.erp.oms.dto.viewObject.OrderVO;
import com.erp.oms.dto.viewObject.WorkStatusVO;
import com.erp.oms.entity.Order;


public interface OrderService extends IService<Order> {
    PageResultVO<OrderVO> pageByQuery(OrderQueryDTO query);
    OrderVO getByOrderId(Long id);
    OrderFullVO getOrderDetails(Long orderId);
    void createManualOrder(OrderCreateDTO dto);
    void updateOrderManual(OrderUpdateDTO dto);
    void closeOrder(Long orderId, String operator);
    void shipOrder(Long orderId, String logisticsNo, String operator);
    void trigSimCustomer(Integer times);
    WorkStatusVO checkWorkflowStatus();
}
