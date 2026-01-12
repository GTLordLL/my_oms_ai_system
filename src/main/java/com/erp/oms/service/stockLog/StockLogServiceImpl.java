package com.erp.oms.service.stockLog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.oms.dto.api.PageResultVO;
import com.erp.oms.dto.pageQuery.StockLogQueryDTO;
import com.erp.oms.dto.viewObject.StockLogVO;
import com.erp.oms.entity.Sku;
import com.erp.oms.entity.StockLog;
import com.erp.oms.enums.StockChangeType;
import com.erp.oms.mapper.StockLogMapper;
import com.erp.oms.service.sku.SkuService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 库存流水实现类：记录每一笔“货”的动向，用于对账和溯源
 */
@Service
public class StockLogServiceImpl extends ServiceImpl<StockLogMapper, StockLog> implements StockLogService{
    private final StockLogMapper stockLogMapper;
    private final SkuService skuService;

    public StockLogServiceImpl(StockLogMapper stockLogMapper,@Lazy SkuService skuService) {
        this.stockLogMapper = stockLogMapper;
        this.skuService = skuService;
    }

    /**
     * [查询] 流水分页：支持按 SKU、类型、单据号、时间范围筛选
     */
    @Override
    public PageResultVO<StockLogVO> pageByQuery(StockLogQueryDTO query){
        Page<StockLog> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<StockLog> w = new LambdaQueryWrapper<>();

        // 1. 精确过滤：SKU ID 或 变动类型（入库/出库/盘点等）
        if (query.getSkuId() != null) w.eq(StockLog::getSkuId, query.getSkuId());
        if (query.getType() != null) {
            w.eq(StockLog::getType, StockChangeType.fromCode(query.getType()));
        }

        // 2. 模糊搜索：单据号（关联 ID）或 操作人姓名
        if (StringUtils.hasText(query.getKeyword())) {
            w.and(wrapper -> wrapper
                    .like(StockLog::getRelationId, query.getKeyword())
                    .or()
                    .like(StockLog::getOperator, query.getKeyword()));
        }

        // 3. 时间维度：筛选特定的起止时间
        if (query.getStartTime() != null) w.ge(StockLog::getCreateTime, query.getStartTime());
        if (query.getEndTime() != null) w.le(StockLog::getCreateTime, query.getEndTime());

        // 4. 执行并转换：按时间倒序排列（最新的动向排第一）
        IPage<StockLog> logPage = this.page(page, w.orderByDesc(StockLog::getCreateTime));
        List<StockLog> records = logPage.getRecords();

        // --- 核心优化逻辑：批量获取 skuCode ---
        final Map<Long, String> skuCodeMap;

        if (!records.isEmpty()) {
            Set<Long> skuIds = records.stream().map(StockLog::getSkuId).collect(Collectors.toSet());
            List<Sku> skus = skuService.listByIds(skuIds);
            skuCodeMap = skus.stream().collect(Collectors.toMap(Sku::getId, Sku::getSkuCode));
        } else {
            skuCodeMap = Collections.emptyMap();
        }
        // ------------------------------------

        List<StockLogVO> voList = records.stream().map(log -> {
            StockLogVO vo = new StockLogVO();
            BeanUtils.copyProperties(log, vo);

            // 设置 skuCode
            vo.setSkuCode(skuCodeMap.getOrDefault(log.getSkuId(), "未知商品"));

            if (log.getType() != null) {
                vo.setType(log.getType().getCode());
                vo.setTypeName(log.getType().getDesc());
            }
            return vo;
        }).collect(Collectors.toList());

        return PageResultVO.of(logPage.getTotal(), voList);
    }

    /**
     * [核心] 写账逻辑：自动根据变动类型（Symbol）纠正正负号并存盘
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void recordLog(Long skuId, Integer changeCount, Integer beforeCount,
                          StockChangeType type, String relationId, String operator){
        StockLog log = new StockLog();

        // 【防错逻辑】强制根据枚举定义的 Symbol (1为加, -1为减) 计算真实的库存变动
        // 哪怕传入的 changeCount 是负数，这里也会根据业务逻辑自动纠正
        int realChange = Math.abs(changeCount) * (type.getSymbol() == 0 ? 1 : type.getSymbol());

        log.setSkuId(skuId);
        log.setType(type);
        log.setChangeCount(realChange);
        log.setBeforeCount(beforeCount);
        log.setAfterCount(beforeCount + realChange); // 计算变动后库存快照
        log.setRelationId(relationId);
        log.setOperator(operator);
        log.setCreateTime(LocalDateTime.now());

        stockLogMapper.insert(log);
    }
}
