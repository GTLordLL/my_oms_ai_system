package com.erp.oms.service.RfmAnalysis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.oms.dto.api.PageResultVO;
import com.erp.oms.dto.pageQuery.RfmQueryDTO;
import com.erp.oms.dto.rfmAnalysis.RfmAnalysisVO;
import com.erp.oms.dto.rfmAnalysis.RfmDashboardVO;
import com.erp.oms.dto.rfmAnalysis.RfmResultDTO;
import com.erp.oms.dto.rfmAnalysis.RfmTaskRequestDTO;
import com.erp.oms.dto.viewObject.AiAnalysis.BusinessInsightVO;
import com.erp.oms.dto.viewObject.WorkStatusVO;
import com.erp.oms.entity.RfmAnalysis;
import com.erp.oms.mapper.RfmAnalysisMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RfmServiceImpl extends ServiceImpl<RfmAnalysisMapper, RfmAnalysis> implements RfmService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RfmAnalysisMapper rfmAnalysisMapper;

    public static final String QUEUE_RFM = "q:rfm:tasks";
    public static final String KEY_RFM_STATUS = "rfm:task:status:";

    /**
     * 【环节 1】初始化任务 (Init)
     * 作用：聚合全量用户 RFM 向量并入队
     */
    @Override
    public String initRfmAnalysis() {
        String taskId = UUID.randomUUID().toString();

        // 1. 调用 Mapper 聚合 SQL 获取原始 RFM 数据
        List<RfmAnalysis> metrics = rfmAnalysisMapper.selectRfmMetrics();

        if (CollectionUtils.isEmpty(metrics)) {
            updateTaskStatus(taskId, "error", "未找到有效订单数据，无法进行分层");
            return taskId;
        }

        // 2. 构造发送给 FastAPI 的 DTO
        List<RfmTaskRequestDTO.UserRfmMetric> dtoList = metrics.stream()
                .filter(m -> StringUtils.hasText(m.getBuyerIdentifier()))
                .map(m -> new RfmTaskRequestDTO.UserRfmMetric(
                        m.getBuyerIdentifier(),
                        m.getRecency(),
                        m.getFrequency(),
                        m.getMonetary()
                )
        ).collect(Collectors.toList());

        RfmTaskRequestDTO request = RfmTaskRequestDTO.builder()
                .taskId(taskId)
                .metrics(dtoList)
                .build();

        // 3. 状态预设并入队
        updateTaskStatus(taskId, "processing", "正在进行 K-Means 聚类分析...");

        Map<String, Object> message = new HashMap<>();
        message.put("taskId", taskId);
        message.put("payload", request);
        redisTemplate.opsForList().leftPush(QUEUE_RFM, message);

        log.info("RFM 分层任务已入队, 样本用户数: {}, TaskID: {}", dtoList.size(), taskId);
        return taskId;
    }

    /**
     * 【环节 3 & 4】持久化结果 (Callback & Persist)
     * 核心逻辑：将 Label 翻译为业务层级
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void handleAlgorithmCallback(RfmResultDTO resultDTO) {
        String taskId = resultDTO.getTaskId();
        try {
            // 1. 获取聚合后的原始数据（用于补全昵称和下单时间）
            Map<String, RfmAnalysis> rawMap = rfmAnalysisMapper.selectRfmMetrics()
                    .stream().collect(Collectors.toMap(RfmAnalysis::getBuyerIdentifier, m -> m));

            // 2. 转换并定义层级标签
            // 注意：理想情况下 FastAPI 应该返回每个 Label 的特征
            // 这里我们采用简单映射，或者由 FastAPI 在 clusterLabels Map 中返回
            List<RfmAnalysis> entities = resultDTO.getResults().stream().map(res -> {
                RfmAnalysis raw = rawMap.get(res.getIdentifier());

                // 从结果包中获取算法建议的标签，如果没有则默认为 "等级 " + ClusterID
                String levelName = resultDTO.getClusterLabels() != null ?
                        resultDTO.getClusterLabels().get(res.getCluster()) :
                        "客户群组 " + res.getCluster();

                return RfmAnalysis.builder()
                        .buyerIdentifier(res.getIdentifier())
                        .buyerNick(raw != null ? raw.getBuyerNick() : "未知")
                        .recency(raw != null ? raw.getRecency() : 0)
                        .frequency(raw != null ? raw.getFrequency() : 0)
                        .monetary(raw != null ? raw.getMonetary() : BigDecimal.ZERO)
                        .lastOrderTime(raw != null ? raw.getLastOrderTime() : null)
                        .clusterLabel(res.getCluster())
                        .customerLevel(levelName)
                        .computeTime(LocalDateTime.now())
                        .build();
            }).collect(Collectors.toList());

            // 3. 全量覆盖式更新
            this.remove(new LambdaQueryWrapper<>());
            this.saveBatch(entities);

            updateTaskStatus(taskId, "success", entities.size());
            log.info("RFM 结果持久化成功，TaskID: {}", taskId);

        } catch (Exception e) {
            log.error("RFM 持久化失败", e);
            updateTaskStatus(taskId, "error", e.getMessage());
        }
    }

    /**
     * 【环节 5】获取展示看板 VO (View)
     */
    @Override
    public RfmDashboardVO getRfmDashboard() {
        RfmDashboardVO vo = new RfmDashboardVO();

        // 1. 获取汇总统计（用于饼图和雷达图）
        List<RfmDashboardVO.LevelStat> stats = rfmAnalysisMapper.selectLevelStats();
        vo.setLevelStats(stats);

        // 2. 计算总用户数
        int total = stats.stream().mapToInt(RfmDashboardVO.LevelStat::getCount).sum();
        vo.setTotalUsers(total);

        // 3. 获取前 100 名高价值用户明细 (Top 100 by Monetary)
        // 这里使用了 selectDetailsPage，传入 null 表示不按等级筛选，只按金额排序
        List<RfmAnalysis> topUsers = rfmAnalysisMapper.selectDetailsPage(
                new Page<>(1, 100), // 第 1 个参数：分页对象，取第 1 页的前 100 条
                null,               // 第 2 个参数：level
                null                // 第 3 个参数：keyword
        );

        // 4. 将 Entity 转换为 VO 列表
        List<RfmAnalysisVO> details = topUsers.stream().map(entity -> {
            RfmAnalysisVO detailVO = new RfmAnalysisVO();
            BeanUtils.copyProperties(entity, detailVO);
            return detailVO;
        }).collect(Collectors.toList());

        vo.setUserDetails(details); // 这里的 set 方法对应之前定义的 RfmDashboardVO 属性

        return vo;
    }

    public WorkStatusVO getTaskWorkStatus(String taskId) {
        WorkStatusVO ws = (WorkStatusVO) redisTemplate.opsForValue().get(KEY_RFM_STATUS + taskId);
        return ws != null ? ws : new WorkStatusVO("none", "未找到任务");
    }

    private void updateTaskStatus(String taskId, String status, Object data) {
        WorkStatusVO ws = new WorkStatusVO();
        ws.setStatus(status);
        ws.setData(data);
        redisTemplate.opsForValue().set(KEY_RFM_STATUS + taskId, ws, 30, TimeUnit.MINUTES);
    }

    /**
     * 供 Controller 调用的分页搜索接口
     */
    public PageResultVO<RfmAnalysisVO> getRfmDetailsPage(RfmQueryDTO queryDTO) {
        // 1. 构建 MyBatis-Plus 分页对象
        Page<RfmAnalysis> pageParam = new Page<>(queryDTO.getPage(), queryDTO.getSize());

        // 2. 调用 Mapper (注意：这里需要把 pageParam 传进去)
        // MyBatis-Plus 会自动将结果封装进 pageParam，同时返回 List
        List<RfmAnalysis> list = rfmAnalysisMapper.selectDetailsPage(pageParam, queryDTO.getLevel(), queryDTO.getKeyword());

        // 3. 转换为 VO 列表
        List<RfmAnalysisVO> voList = list.stream().map(e -> {
            RfmAnalysisVO vo = new RfmAnalysisVO();
            BeanUtils.copyProperties(e, vo);
            return vo;
        }).collect(Collectors.toList());

        // 4. 使用你的 PageResultVO 返回真正的 total
        return PageResultVO.of(pageParam.getTotal(), voList);
    }

    @Override
    public List<BusinessInsightVO.RfmLevelInsight> getAILevelInsights() {
        // 1. 调用 Mapper 获取统计汇总数据 (已经包含了 count, percentage, avgM 等)
        List<RfmDashboardVO.LevelStat> stats = rfmAnalysisMapper.selectLevelStats();

        if (CollectionUtils.isEmpty(stats)) {
            return new ArrayList<>();
        }

        // 2. 转换为 AI 专用的 Insight 对象
        return stats.stream().map(stat -> {
            BusinessInsightVO.RfmLevelInsight insight = new BusinessInsightVO.RfmLevelInsight();

            insight.setLevelName(stat.getLevelName());
            insight.setUserCount(stat.getCount());
            insight.setPercentage(stat.getPercentage());

            // 将 Double 类型的平均金额转为 BigDecimal，保证 AI 看板一致性
            insight.setAvgMonetary(BigDecimal.valueOf(stat.getAvgM()).setScale(2, RoundingMode.HALF_UP));

            // 趋势判定逻辑：这里可以根据业务需求，对比上一次计算的结果
            // 暂时默认返回 STABLE，如果数据库有历史快照表，可在此处对比 userCount 的增减
            insight.setTrend("STABLE");

            return insight;
        }).collect(Collectors.toList());
    }
}