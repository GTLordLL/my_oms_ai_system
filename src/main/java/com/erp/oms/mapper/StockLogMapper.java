package com.erp.oms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.oms.dto.viewObject.dashboard.StockSupplyChainVO;
import com.erp.oms.entity.StockLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StockLogMapper extends BaseMapper<StockLog> {

    /** 统计库存变动构成 */
    @Select("SELECT type, SUM(ABS(change_count)) as totalChange " +
            "FROM oms_stock_log " +
            "WHERE create_time >= CURRENT_DATE - INTERVAL '30 days' " +
            "GROUP BY type")
    List<StockSupplyChainVO.StockMovementVO> getStockMovementStats();

    /** * 统计近30天退货入库关联单据数
     * 该查询返回的是单列聚合值（Integer），不涉及别名映射
     */
    @Select("SELECT COUNT(DISTINCT relation_id) FROM oms_stock_log " +
            "WHERE type = 3 AND create_time >= CURRENT_DATE - INTERVAL '30 days'")
    Integer countRefundOrderIn30Days();
}
