package com.erp.oms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.oms.dto.demandForecast.SalesHistoryDTO;
import com.erp.oms.dto.demandForecast.SKUForecastVO;
import com.erp.oms.entity.ForecastRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ForecastRecordMapper extends BaseMapper<ForecastRecord> {
    /**
     * 聚合指定 SKU 的历史销量数据
     * 过滤已支付/已完成状态的订单
     */
    @Select("SELECT CAST(o.pay_time AS DATE) as ds, SUM(oi.quantity) as y " +
            "FROM oms_order o " +
            "JOIN oms_order_item oi ON o.id = oi.order_id " +
            "WHERE oi.sku_id = #{skuId} " +
            "AND o.status IN (2, 3) " + //  2:已发货, 3:已完成
            "AND o.is_deleted = 0 " +
            "GROUP BY CAST(o.pay_time AS DATE) " +
            "ORDER BY ds ASC")
    List<SalesHistoryDTO> selectSalesHistory(@Param("skuId") Long skuId);

    /**
     * 根据 SKU ID 获取单个基础信息
     */
    @Select("SELECT s.id as skuId, s.sku_code as skuCode, p.name as productName, " +
            "p.category, s.stock_quantity as currentStock, s.alert_quantity as alertQuantity " +
            "FROM oms_sku s JOIN oms_product p ON s.product_id = p.id " +
            "WHERE s.id = #{skuId} AND s.is_deleted = 0")
    SKUForecastVO selectSkuBaseInfoById(@Param("skuId") Long skuId);

    /**
     * 获取所有需要预测的 SKU 基础信息及其当前库存
     */
    @Select("SELECT s.id as skuId, s.sku_code as skuCode, p.name as productName, " +
            "p.category, s.stock_quantity as currentStock, s.alert_quantity as alertQuantity " +
            "FROM oms_sku s JOIN oms_product p ON s.product_id = p.id " +
            "WHERE s.is_deleted = 0")
    List<SKUForecastVO> selectAllSkuBaseInfo();

}
