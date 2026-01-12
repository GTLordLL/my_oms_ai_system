package com.erp.oms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.oms.dto.association.AssociationRuleVO;
import com.erp.oms.entity.AssociationRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface AssociationRuleMapper extends BaseMapper<AssociationRule> {

    /**
     * 核心：提取“订单-商品”事务矩阵
     * 使用 PostgreSQL 的 string_agg 函数将同一订单下的 SKU 编码合并
     * 返回示例：[{"order_id": 1, "items": "SKU001,SKU002"}, {"order_id": 2, "items": "SKU001,SKU003"}]
     */
    @Select("SELECT order_id, string_agg(sku_code, ',') as items " +
            "FROM oms_order_item " +
            "GROUP BY order_id")
    List<Map<String, Object>> selectOrderTransactions();

    /**
     * 批量获取 SKU 简单信息 (用于 VO 填充)
     * 关联 oms_sku 和 oms_product
     */
    @Select("<script>" +
            "SELECT s.sku_code, p.name as productName, p.main_image as mainImage " +
            "FROM oms_sku s " +
            "JOIN oms_product p ON s.product_id = p.id " +
            "WHERE s.sku_code IN " +
            "<foreach item='code' collection='codes' open='(' separator=',' close=')'>" +
            "#{code}" +
            "</foreach>" +
            "</script>")
    List<AssociationRuleVO.SkuSimpleInfo> selectSkuSimpleInfos(@Param("codes") List<String> codes);

    /**
     * 根据前项或后项模糊查询规则 (用于前端搜索)
     */
    @Select("SELECT * FROM oms_association_rule " +
            "WHERE ante_names LIKE CONCAT('%', #{keyword}, '%') " +
            "OR cons_names LIKE CONCAT('%', #{keyword}, '%') " +
            "ORDER BY lift DESC")
    List<AssociationRule> searchRules(@Param("keyword") String keyword);
}