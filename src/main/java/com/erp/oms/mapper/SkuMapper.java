package com.erp.oms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.oms.dto.viewObject.dashboard.StockSupplyChainVO;
import com.erp.oms.entity.Sku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SkuMapper extends BaseMapper<Sku> {

    /**
     * 原子更新库存
     * 这里的逻辑字段名通常对应数据库列名，保持下划线即可
     */
    @Update("UPDATE oms_sku SET stock_quantity = stock_quantity + #{count} " +
            "WHERE id = #{skuId} AND (stock_quantity + #{count} >= 0)")
    int updateStock(@Param("skuId") Long skuId, @Param("count") Integer count);

    /**
     * 库存预警统计
     */
    @Select("SELECT COUNT(*) FROM oms_sku WHERE stock_quantity <= alert_quantity AND is_deleted = 0")
    Integer countStockAlert();

    /**
     * 查询高周转 SKU：按近30天销售出库量排序
     */
    @Select("SELECT s.sku_code as skuCode, s.specs, s.stock_quantity as stockQuantity, " +
            "COALESCE(ABS(SUM(l.change_count)), 0) as salesCount30Days " +
            "FROM oms_sku s " +
            "LEFT JOIN oms_stock_log l ON s.id = l.sku_id " +
            "  AND l.type = 2 " +
            "  AND l.create_time >= CURRENT_DATE - INTERVAL '30 days' " +
            "WHERE s.is_deleted = 0 " +
            "GROUP BY s.id, s.sku_code, s.specs, s.stock_quantity " +
            "ORDER BY salesCount30Days DESC " +
            "LIMIT 10")
    List<StockSupplyChainVO.SkuTurnoverVO> getHighTurnoverSkus();

    /**
     * 查询积压库存：库存量高但近30天销量极低
     */
    @Select("SELECT s.sku_code as skuCode, s.specs, s.stock_quantity as stockQuantity, " +
            "COALESCE(ABS(SUM(l.change_count)), 0) as salesCount30Days " +
            "FROM oms_sku s " +
            "LEFT JOIN oms_stock_log l ON s.id = l.sku_id " +
            "  AND l.type = 2 " +
            "  AND l.create_time >= CURRENT_DATE - INTERVAL '30 days' " +
            "WHERE s.is_deleted = 0 AND s.stock_quantity > 0 " +
            "GROUP BY s.id, s.sku_code, s.specs, s.stock_quantity " +
            "ORDER BY salesCount30Days ASC, s.stock_quantity DESC " +
            "LIMIT 10")
    List<StockSupplyChainVO.SkuTurnoverVO> getStagnantSkus();
}