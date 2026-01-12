package com.erp.oms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.oms.dto.rfmAnalysis.RfmDashboardVO;
import com.erp.oms.entity.RfmAnalysis;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RfmAnalysisMapper extends BaseMapper<RfmAnalysis> {

    /**
     * 核心聚合 SQL：提取 RFM 原始向量
     * R (Recency): 当前时间 - 最近一次付款时间 (天数)
     * F (Frequency): 订单总数
     * M (Monetary): 实付总金额
     */
    @Select("SELECT " +
            "  receiver_mobile AS buyerIdentifier, " +
            "  MAX(buyer_nick) AS buyerNick, " +
            "  EXTRACT(DAY FROM (NOW() - MAX(pay_time))) AS recency, " +
            "  COUNT(platform_order_sn) AS frequency, " +
            "  SUM(pay_amount) AS monetary, " +
            "  MAX(pay_time) AS lastOrderTime " +
            "FROM oms_order " +
            "WHERE status IN (3, 5) " +
            "  AND is_deleted = 0 " +
            "  AND receiver_mobile IS NOT NULL " + // *** 关键修复：排除空手机号 ***
            "  AND receiver_mobile != '' " +       // 同时也排除空字符串
            "GROUP BY receiver_mobile")
    List<RfmAnalysis> selectRfmMetrics();

    /**
     * 获取分层统计数据 (用于 Dashboard)
     * 计算每个簇的平均值，用来给前端画雷达图（对比各群组特征）
     */
    @Select("SELECT " +
            "  customer_level AS levelName, " +
            "  COUNT(*) AS count, " +
            "  ROUND(CAST(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM oms_rfm_analysis) AS NUMERIC), 2) AS percentage, " +
            "  AVG(recency) AS avgR, " +
            "  AVG(frequency) AS avgF, " +
            "  AVG(monetary) AS avgM " +
            "FROM oms_rfm_analysis " +
            "GROUP BY customer_level")
    List<RfmDashboardVO.LevelStat> selectLevelStats();

    /**
     * 分页查询用户分层明细
     * MyBatis-Plus 识别到第一个参数是 Page 时，会自动处理分页 SQL
     */
    @Select("<script>" +
            "SELECT * FROM oms_rfm_analysis " +
            "<where>" +
            "  <if test='level != null and level != \"\"'>" +
            "    AND customer_level = #{level}" +
            "  </if>" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    AND (buyer_identifier LIKE CONCAT('%', #{keyword}, '%') OR buyer_nick LIKE CONCAT('%', #{keyword}, '%'))" +
            "  </if>" +
            "</where>" +
            "ORDER BY monetary DESC" +
            "</script>")
    List<RfmAnalysis> selectDetailsPage(
            IPage<RfmAnalysis> page, // 必须放在第一位
            @Param("level") String level,
            @Param("keyword") String keyword
    );
}