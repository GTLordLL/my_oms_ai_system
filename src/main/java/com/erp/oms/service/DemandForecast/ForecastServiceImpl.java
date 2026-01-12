package com.erp.oms.service.DemandForecast;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.oms.dto.demandForecast.ForecastRequestDTO;
import com.erp.oms.dto.demandForecast.ForecastResultDTO;
import com.erp.oms.dto.demandForecast.SalesHistoryDTO;
import com.erp.oms.dto.viewObject.WorkStatusVO;
import com.erp.oms.dto.demandForecast.SKUForecastVO;
import com.erp.oms.entity.ForecastRecord;
import com.erp.oms.entity.Product;
import com.erp.oms.entity.Sku;
import com.erp.oms.mapper.ForecastRecordMapper;
import com.erp.oms.mapper.ProductMapper;
import com.erp.oms.mapper.SkuMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

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
public class ForecastServiceImpl extends ServiceImpl<ForecastRecordMapper, ForecastRecord> implements ForecastService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private ForecastRecordMapper forecastRecordMapper;

    @Resource
    private SkuMapper skuMapper;

    @Resource
    private ProductMapper productMapper;

    // Redis Key 定义
    public static final String QUEUE_TASKS = "q:forecast:tasks";
    public static final String KEY_STATUS = "forecast:task:status:";

    /**
     * 【环节 1】初始化任务 (Init)
     * 作用：SpringBoot 接收前端选择的 SKU，异步入队
     */
    @Override
    public String initManualForecast(Long skuId) { // 删掉参数 skuCode
        String taskId = UUID.randomUUID().toString();

        // 1. 核心：通过 ID 查询 SKU 基础信息（补全 skuCode）
        Sku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            updateTaskStatus(taskId, "error", "未找到该商品规格信息");
            return taskId;
        }
        String skuCode = sku.getSkuCode(); // 内部自动获取

        // 2. 提取历史销售数据
        List<SalesHistoryDTO> history = forecastRecordMapper.selectSalesHistory(skuId);

        // 校验数据量
        if (CollectionUtils.isEmpty(history) || history.size() < 14) {
            updateTaskStatus(taskId, "error", "历史销售数据不足14天，无法训练模型");
            return taskId;
        }

        // 3. 构造请求包（依然带上 skuCode，方便 Python 端记录）
        ForecastRequestDTO request = ForecastRequestDTO.builder()
                .skuId(skuId)
                .skuCode(skuCode)
                .horizon(30)
                .history(history)
                .build();

        // 4. 入队逻辑保持不变
        updateTaskStatus(taskId, "processing", "算法正在计算中...");

        Map<String, Object> message = new HashMap<>();
        message.put("taskId", taskId);
        message.put("payload", request);
        redisTemplate.opsForList().leftPush(QUEUE_TASKS, message);

        log.info("SKU {} 预测任务已入队, TaskID: {}", skuCode, taskId);
        return taskId;
    }

    /**
     * 【环节 2】查询任务状态 (Status)
     * 作用：配合 Vue 轮询展示 WorkStatusVO
     */
    public WorkStatusVO getTaskWorkStatus(String taskId) {
        WorkStatusVO ws = (WorkStatusVO) redisTemplate.opsForValue().get(KEY_STATUS + taskId);
        return ws != null ? ws : new WorkStatusVO("none", "未找到任务");
    }

    /**
     * 【环节 3 & 4】持久化接收结果 (Callback & Persist)
     * 作用：SpringBoot 接收结果，存入数据库，更新状态为 Success
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleAlgorithmCallback(ForecastResultDTO resultDTO) { // 修改签名
        String taskId = resultDTO.getTaskId(); // 从对象中取出 taskId
        try {
            // 1. 清理该 SKU 旧预测
            this.remove(new LambdaQueryWrapper<ForecastRecord>()
                    .eq(ForecastRecord::getSkuId, resultDTO.getSkuId()));

            // 2. 转换为 Entity 并批量保存
            List<ForecastRecord> entities = resultDTO.getPredictions().stream().map(p ->
                    ForecastRecord.builder()
                            .skuId(resultDTO.getSkuId())
                            .skuCode(resultDTO.getSkuCode())
                            .forecastDate(p.getDs())
                            .forecastValue(p.getYhat())
                            .upperBound(p.getYhatUpper())
                            .lowerBound(p.getYhatLower())
                            .computeTime(LocalDateTime.now())
                            .build()
            ).collect(Collectors.toList());

            this.saveBatch(entities);

            // 3. 状态置为 SUCCESS
            updateTaskStatus(taskId, "success", resultDTO.getSkuId());
            log.info("Task {} 结果持久化成功", taskId);

        } catch (Exception e) {
            log.error("持久化预测结果失败, TaskID: {}", taskId, e);
            updateTaskStatus(taskId, "error", "数据库写入失败: " + e.getMessage());
        }
    }

    /**
     * 【环节 5】获取结果展示 (View)
     * 作用：前端拿到 success 后，请求此接口获取聚合了库存信息的最终 VO
     */
    public SKUForecastVO getForecastView(Long skuId) {
        // 1. 获取基础信息
        SKUForecastVO vo = forecastRecordMapper.selectSkuBaseInfoById(skuId);

        // 2. 获取明细
        List<ForecastRecord> records = this.list(new LambdaQueryWrapper<ForecastRecord>()
                .eq(ForecastRecord::getSkuId, skuId)
                .orderByAsc(ForecastRecord::getForecastDate));

        // 3. 将明细存入 VO (关键点！)
        vo.setRecords(records);

        // 4. 计算聚合指标
        if (!CollectionUtils.isEmpty(records)) {
            calculateBusinessLogic(vo, records);
        }
        return vo;
    }

    /**
     * 获取全量预测看板数据
     * 用于页面初始化加载
     */
    public List<SKUForecastVO> getAllForecastDashboard() {
        // 1. 使用 selectAllSkuBaseInfo 拿到所有 SKU 基础信息
        List<SKUForecastVO> voList = baseMapper.selectAllSkuBaseInfo();

        // 2. 批量获取数据库中现有的所有预测记录 (避免在循环中查库)
        List<ForecastRecord> allRecords = this.list();

        // 3. 按 skuId 分组，方便快速匹配
        Map<Long, List<ForecastRecord>> recordMap = allRecords.stream()
                .collect(Collectors.groupingBy(ForecastRecord::getSkuId));

        // 4. 遍历填充逻辑
        for (SKUForecastVO vo : voList) {
            List<ForecastRecord> skuRecords = recordMap.get(vo.getSkuId());
            if (!CollectionUtils.isEmpty(skuRecords)) {
                // 复用你刚刚写的计算逻辑
                calculateBusinessLogic(vo, skuRecords);
            }
        }
        return voList;
    }

    /**
     * 业务逻辑核心计算方法
     */
    private void calculateBusinessLogic(SKUForecastVO vo, List<ForecastRecord> predictions) {
        int totalDays = predictions.size();
        if (totalDays == 0) return;

        // 1. 动态计算和 (考虑实际天数可能不足 7 或 30)
        double sum7 = predictions.stream()
                .limit(7)
                .mapToDouble(p -> p.getForecastValue().doubleValue())
                .sum();

        double sum30 = predictions.stream()
                .limit(30)
                .mapToDouble(p -> p.getForecastValue().doubleValue())
                .sum();

        vo.setDemandNext7Days((int) Math.ceil(sum7));
        vo.setDemandNext30Days((int) Math.ceil(sum30));

        // 2. 补货建议 (只有当预测天数覆盖了 7 天才算准确)
        if (totalDays >= 7) {
            int suggested = vo.getDemandNext7Days() - vo.getCurrentStock() + vo.getAlertQuantity();
            vo.setSuggestedReplenishment(Math.max(0, suggested));
        }

        // 3. 趋势预判 (动态分母，防止除以 0)
        if (totalDays > 7) {
            double avg7 = sum7 / 7.0;
            double avgRest = (sum30 - sum7) / (double)(totalDays - 7);
            // 使用更细致的波动阈值，避免微小差异导致频繁变动
            if (avgRest > avg7 * 1.05) vo.setTrend("UP");
            else if (avg7 > avgRest * 1.05) vo.setTrend("DOWN");
            else vo.setTrend("STABLE");
        } else {
            vo.setTrend("STABLE");
        }

        // 4. 风险等级 (增加断货风险判断)
        if (vo.getCurrentStock() == 0) {
            vo.setRiskLevel("OUT_OF_STOCK");
        } else if (vo.getCurrentStock() < vo.getDemandNext7Days()) {
            vo.setRiskLevel("CRITICAL"); // 撑不到一周
        } else if (vo.getCurrentStock() < vo.getAlertQuantity()) {
            vo.setRiskLevel("WARNING");  // 低于人工设置的警戒线
        } else {
            vo.setRiskLevel("SAFE");
        }
    }

    /**
     * 内部辅助：更新 Redis 状态机
     */
    private void updateTaskStatus(String taskId, String status, Object data) {
        WorkStatusVO ws = new WorkStatusVO();
        ws.setStatus(status);
        ws.setData(data);
        // 状态保存 30 分钟，足够前端轮询
        redisTemplate.opsForValue().set(KEY_STATUS + taskId, ws, 30, TimeUnit.MINUTES);
    }

    /**
     * 【全量定时预测】
     * 每天凌晨 2:00 自动执行
     * 逻辑：扫描所有“上架状态”商品下的所有 SKU，并依次推入预测队列
     */
    @Scheduled(cron = "0 0 2 * * ?") // 线上建议凌晨跑
    // @Scheduled(fixedDelay = 3600000) // 也可以设置项目启动后每隔一小时跑一次
    // @Scheduled(cron = "0 0/15 * * * ?")
    public void autoForecastAllSkusTask() {
        log.info(">>> 开始执行全量 SKU 销量预测定时任务 <<<");

        // 1. 查找所有上架状态(status=1)的商品 ID
        List<Product> onlineProducts = productMapper.selectList(
                new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1)
        );

        if (CollectionUtils.isEmpty(onlineProducts)) {
            log.warn("未找到任何上架商品，跳过定时预测");
            return;
        }

        List<Long> productIds = onlineProducts.stream().map(Product::getId).collect(Collectors.toList());

        // 2. 查找这些商品下所有的 SKU
        List<Sku> skuList = skuMapper.selectList(
                new LambdaQueryWrapper<Sku>().in(Sku::getProductId, productIds)
        );

        log.info("本次预扫描到 {} 个候选 SKU", skuList.size());

        int pushCount = 0;
        for (Sku sku : skuList) {
            try {
                // 直接调用你现有的初始化方法
                // 该方法内部自带“历史数据是否足14天”的检查，不足会自动跳过
                String taskId = this.initManualForecast(sku.getId());
                log.info("taskId: {}",taskId);
                // 注意：因为 initManualForecast 可能会因为数据不足返回 error 状态的 taskId
                // 如果你想统计真正进入队列的数量，可以根据业务微调返回值判断
                pushCount++;
            } catch (Exception e) {
                log.error("SKU {} 自动预测入队异常: {}", sku.getSkuCode(), e.getMessage());
            }
        }
        log.info(">>> 全量预测任务分发完成，共计推送 {} 个任务至 Redis 队列 <<<", pushCount);
    }

    /**
     * 专门为 AI 决策报告提供的：获取高风险 SKU 列表
     * 逻辑：从所有预测看板数据中，筛选出风险等级为 OUT_OF_STOCK 或 CRITICAL 的商品
     */
    @Override
    public List<SKUForecastVO> getHighRiskSkus(int limit) {
        // 1. 获取所有带有预测计算逻辑的 SKU 列表
        List<SKUForecastVO> allDashboard = this.getAllForecastDashboard();

        // 2. 筛选：只看断货或严重不足的 (OUT_OF_STOCK, CRITICAL)
        // 3. 排序：按建议补货量降序排列，把最急需补货的推给 AI
        return allDashboard.stream()
                .filter(vo -> "CRITICAL".equals(vo.getRiskLevel()) || "OUT_OF_STOCK".equals(vo.getRiskLevel()))
                .sorted(Comparator.comparing(SKUForecastVO::getSuggestedReplenishment).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
}
