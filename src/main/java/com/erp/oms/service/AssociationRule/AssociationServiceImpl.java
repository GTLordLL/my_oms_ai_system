package com.erp.oms.service.AssociationRule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.oms.dto.association.AssociationResultDTO;
import com.erp.oms.dto.association.AssociationRuleVO;
import com.erp.oms.dto.association.AssociationTaskRequestDTO;
import com.erp.oms.dto.viewObject.WorkStatusVO;
import com.erp.oms.entity.AssociationRule;
import com.erp.oms.mapper.AssociationRuleMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AssociationServiceImpl extends ServiceImpl<AssociationRuleMapper, AssociationRule> implements AssociationService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private AssociationRuleMapper associationRuleMapper;

    // 复用相同的 Redis Key 规范，但使用不同的前缀以隔离业务
    public static final String QUEUE_ASSOCIATION = "q:association:tasks";
    public static final String KEY_ASSOC_STATUS = "assoc:task:status:";

    /**
     * 【环节 1】初始化任务 (Init)
     * 作用：提取全量“订单-商品”矩阵，推入 Redis 队列
     */
    @Override
    public String initAssociationAnalysis() {
        String taskId = UUID.randomUUID().toString();

        // 1. 提取所有订单的事务列表 (Transaction List)
        // 数据库层面使用 string_agg 聚合，返回 [{order_id: 1, items: "SKU001,SKU002"}]
        List<Map<String, Object>> rawData = associationRuleMapper.selectOrderTransactions();

        if (CollectionUtils.isEmpty(rawData) || rawData.size() < 10) {
            updateTaskStatus(taskId, "error", "历史订单不足10笔，无法进行关联分析");
            return taskId;
        }

        // 2. 将原始数据转换为 List<List<String>> 格式
        List<List<String>> transactions = rawData.stream()
                .map(row -> {
                    String items = (String) row.get("items");
                    return Arrays.asList(items.split(","));
                })
                .collect(Collectors.toList());

        // 3. 构造请求包
        AssociationTaskRequestDTO request = AssociationTaskRequestDTO.builder()
                .taskId(taskId)
                .transactions(transactions)
                .minSupport(0.05)    // 默认支持度 5%
                .minConfidence(0.3)  // 默认置信度 30%
                .build();

        // 4. 设置状态并入队
        updateTaskStatus(taskId, "processing", "正在挖掘商品关联规则...");

        Map<String, Object> message = new HashMap<>();
        message.put("taskId", taskId);
        message.put("payload", request);
        redisTemplate.opsForList().leftPush(QUEUE_ASSOCIATION, message);

        log.info("全量关联分析任务已入队, 样本订单数: {}, TaskID: {}", transactions.size(), taskId);
        return taskId;
    }

    /**
     * 【环节 2】查询任务状态 (Status)
     */
    public WorkStatusVO getTaskWorkStatus(String taskId) {
        WorkStatusVO ws = (WorkStatusVO) redisTemplate.opsForValue().get(KEY_ASSOC_STATUS + taskId);
        return ws != null ? ws : new WorkStatusVO("none", "未找到任务");
    }

    /**
     * 【环节 3 & 4】持久化结果 (Callback & Persist)
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleAlgorithmCallback(AssociationResultDTO resultDTO) {
        String taskId = resultDTO.getTaskId();
        try {
            // 1. 检查规则列表是否为空 (AI 生成的数据由于每单只有一个商品，这里大概率是空的)
            if (CollectionUtils.isEmpty(resultDTO.getRules())) {
                log.warn("任务 {} 未挖掘到任何关联规则（可能是订单中缺乏组合购买行为）", taskId);
                // 清理旧数据（可选，视业务需求而定）
                this.remove(new LambdaQueryWrapper<>());
                // 直接更新状态为成功，但返回数量为 0
                updateTaskStatus(taskId, "success", 0);
                return;
            }

            // 2. 只有当有规则时，才去收集 SKU Code
            Set<String> allCodes = new HashSet<>();
            resultDTO.getRules().forEach(r -> {
                allCodes.addAll(r.getAntecedents());
                allCodes.addAll(r.getConsequents());
            });

            // 3. 只有当 allCodes 不为空时，才执行数据库查询，彻底解决 SQL 语法错误
            Map<String, String> nameMap = new HashMap<>();
            if (!allCodes.isEmpty()) {
                List<AssociationRuleVO.SkuSimpleInfo> infoList = associationRuleMapper.selectSkuSimpleInfos(new ArrayList<>(allCodes));
                if (!CollectionUtils.isEmpty(infoList)) {
                    nameMap = infoList.stream().collect(Collectors.toMap(
                            AssociationRuleVO.SkuSimpleInfo::getSkuCode,
                            AssociationRuleVO.SkuSimpleInfo::getProductName,
                            (k1, k2) -> k1
                    ));
                }
            }

            // 4. 清理旧规则并转换 Entity
            this.remove(new LambdaQueryWrapper<>());
            final Map<String, String> finalNameMap = nameMap;
            List<AssociationRule> entities = resultDTO.getRules().stream().map(rule -> {
                String anteNames = rule.getAntecedents().stream().map(code -> finalNameMap.getOrDefault(code, code)).collect(Collectors.joining(","));
                String consNames = rule.getConsequents().stream().map(code -> finalNameMap.getOrDefault(code, code)).collect(Collectors.joining(","));

                return AssociationRule.builder()
                        .anteSkuIds(String.join(",", rule.getAntecedents()))
                        .anteNames(anteNames)
                        .consSkuIds(String.join(",", rule.getConsequents()))
                        .consNames(consNames)
                        .support(rule.getSupport())
                        .confidence(rule.getConfidence())
                        .lift(rule.getLift())
                        .occurrenceCount(rule.getCount())
                        .ruleType("SKU")
                        .computeTime(LocalDateTime.now())
                        .build();
            }).collect(Collectors.toList());

            this.saveBatch(entities);

            updateTaskStatus(taskId, "success", entities.size());
            log.info("Task {} 关联规则挖掘完成，共生成 {} 条规则", taskId, entities.size());
        } catch (Exception e) {
            log.error("持久化关联规则失败, TaskID: {}", taskId, e);
            updateTaskStatus(taskId, "error", "规则入库失败: " + e.getMessage());
        }
    }

    /**
     * 【环节 5】获取展示 VO (View)
     */
    public List<AssociationRuleVO> getAllRulesVO() {
        // 1. 查出库中所有强关联规则
        List<AssociationRule> rules = this.list(new LambdaQueryWrapper<AssociationRule>()
                .gt(AssociationRule::getLift, 1.0)
                .orderByDesc(AssociationRule::getLift));

        if (CollectionUtils.isEmpty(rules)) {
            return new ArrayList<>();
        }

        // 2. 收集所有规则中涉及到的唯一 SKU Code (用于批量查询，避免 N+1)
        Set<String> allSkuCodes = new HashSet<>();
        for (AssociationRule rule : rules) {
            allSkuCodes.addAll(Arrays.asList(rule.getAnteSkuIds().split(",")));
            allSkuCodes.addAll(Arrays.asList(rule.getConsSkuIds().split(",")));
        }

        // 3. 使用你写的 Mapper 方法：批量获取商品简单信息
        List<AssociationRuleVO.SkuSimpleInfo> skuInfos = associationRuleMapper.selectSkuSimpleInfos(new ArrayList<>(allSkuCodes));

        // 转换为 Map 结构，方便快速查找映射
        Map<String, AssociationRuleVO.SkuSimpleInfo> infoMap = skuInfos.stream()
                .collect(Collectors.toMap(AssociationRuleVO.SkuSimpleInfo::getSkuCode, i -> i));

        // 4. 转换为前端 VO 并填充详细信息
        return rules.stream()
                .map(rule -> convertToVO(rule, infoMap))
                .collect(Collectors.toList());
    }

    private AssociationRuleVO convertToVO(AssociationRule entity, Map<String, AssociationRuleVO.SkuSimpleInfo> infoMap) {
        AssociationRuleVO vo = new AssociationRuleVO();
        BeanUtils.copyProperties(entity, vo);

        // 1. 填充前项商品详情
        List<String> anteCodes = Arrays.asList(entity.getAnteSkuIds().split(","));
        List<AssociationRuleVO.SkuSimpleInfo> anteList = anteCodes.stream()
                .map(code -> infoMap.getOrDefault(code, new AssociationRuleVO.SkuSimpleInfo(code, "未知商品", "")))
                .collect(Collectors.toList());
        vo.setAnteProducts(anteList);

        // 2. 填充后项商品详情
        List<String> consCodes = Arrays.asList(entity.getConsSkuIds().split(","));
        List<AssociationRuleVO.SkuSimpleInfo> consList = consCodes.stream()
                .map(code -> infoMap.getOrDefault(code, new AssociationRuleVO.SkuSimpleInfo(code, "未知商品", "")))
                .collect(Collectors.toList());
        vo.setConsProducts(consList);

        // 3. 动态生成易读的建议文本
        String anteNames = anteList.stream().map(AssociationRuleVO.SkuSimpleInfo::getProductName).collect(Collectors.joining(", "));
        String consNames = consList.stream().map(AssociationRuleVO.SkuSimpleInfo::getProductName).collect(Collectors.joining(", "));
        vo.setSuggestion("购买了 [" + anteNames + "] 的顾客，极大概率也会购买 [" + consNames + "]");

        // 4. 设定强度等级
        double lift = entity.getLift().doubleValue();
        if (lift > 2.0) vo.setStrengthLevel("Strong");
        else if (lift > 1.2) vo.setStrengthLevel("Medium");
        else vo.setStrengthLevel("Weak");

        return vo;
    }

    /**
     * 定时任务：每周日凌晨 4 点更新一次规则
     */
    @Scheduled(cron = "0 0 4 ? * SUN")
    // @Scheduled(cron = "0 0/15 * * * ?")
    public void autoUpdateAssociationRules() {
        log.info(">>> 开始执行每周关联分析挖掘 <<<");
        this.initAssociationAnalysis();
    }

    private void updateTaskStatus(String taskId, String status, Object data) {
        WorkStatusVO ws = new WorkStatusVO();
        ws.setStatus(status);
        ws.setData(data);
        redisTemplate.opsForValue().set(KEY_ASSOC_STATUS + taskId, ws, 30, TimeUnit.MINUTES);
    }

    @Override
    public List<AssociationRuleVO> getStrongRules(int limit) {
        // 1. 获取所有已经转换好商品名称、建议文本和强度等级的规则
        List<AssociationRuleVO> allRules = this.getAllRulesVO();

        if (CollectionUtils.isEmpty(allRules)) {
            return new ArrayList<>();
        }

        // 2. 筛选逻辑：
        // - 只要 Strong 和 Medium 等级的规则（Lift > 1.2）
        // - 按置信度 (Confidence) 降序排列，保证 AI 看到的建议是最高频发生的
        return allRules.stream()
                .filter(rule -> !"Weak".equals(rule.getStrengthLevel()))
                .sorted(Comparator.comparing(AssociationRuleVO::getConfidence).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
}