package com.erp.oms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.oms.dto.viewObject.dashboard.SalesAnalysisVO;
import com.erp.oms.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
    /**
     * 统计热销商品Top 10
    /** 1. 热销商品 (productName, totalQuantity, totalAmount) */
    @Select("SELECT p.name as productName, " + // 别名由 product_name 改为 productName
            "SUM(oi.quantity) as totalQuantity, " +
            "SUM(oi.unit_price * oi.quantity) as totalAmount " +
            "FROM oms_order_item oi " +
            "JOIN oms_sku s ON oi.sku_id = s.id " +
            "JOIN oms_product p ON s.product_id = p.id " +
            "GROUP BY p.id, p.name " +
            "ORDER BY totalQuantity DESC " +
            "LIMIT 10")
    List<SalesAnalysisVO.HotProductVO> getHotProductsTop10();
}