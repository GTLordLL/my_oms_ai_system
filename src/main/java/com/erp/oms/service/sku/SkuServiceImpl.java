package com.erp.oms.service.sku;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.oms.dto.stockManager.PurchaseInboundDTO;
import com.erp.oms.dto.api.PageResultVO;
import com.erp.oms.dto.pageQuery.SkuQueryDTO;
import com.erp.oms.dto.stockManager.RefundInboundDTO;
import com.erp.oms.dto.update.SkuUpdateDTO;
import com.erp.oms.dto.viewObject.SkuVO;
import com.erp.oms.entity.Order;
import com.erp.oms.entity.Sku;
import com.erp.oms.enums.OrderStatus;
import com.erp.oms.enums.StockChangeType;
import com.erp.oms.mapper.SkuMapper;
import com.erp.oms.service.order.OrderService;
import com.erp.oms.service.stockLog.StockLogService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * [查询] SKU 分页：包含库存预警过滤 & 安全状态计算
 */
@Service
public class SkuServiceImpl extends ServiceImpl<SkuMapper, Sku> implements SkuService {
    private final StockLogService stockLogService;
    private final SkuMapper skuMapper;
    private final OrderService orderService;

    public SkuServiceImpl(StockLogService stockLogService, SkuMapper skuMapper, OrderService orderService) {
        this.stockLogService = stockLogService;
        this.skuMapper = skuMapper;
        this.orderService = orderService;
    }

    /**
     * [查询] SKU 分页：包含库存预警过滤 & 安全状态计算
     */
    @Override
    public PageResultVO<SkuVO> pageByQuery(SkuQueryDTO query) {
        Page<Sku> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<Sku> w = new LambdaQueryWrapper<>();

        // 1. 基础过滤：关联商品、编码/规格模糊匹配
        if (query.getProductId() != null) w.eq(Sku::getProductId, query.getProductId());
        if (StringUtils.hasText(query.getKeyword())) {
            w.and(q -> q.like(Sku::getSkuCode, query.getKeyword()).or().like(Sku::getSpecs, query.getKeyword()));
        }

        // 2. 预警触发：筛选 当前库存 <= 预警值 的记录
        if (Boolean.TRUE.equals(query.getLowStock())) {
            w.apply("stock_quantity <= alert_quantity");
        }

        // 3. 价格区间过滤
        if (query.getMinPrice() != null) w.ge(Sku::getCostPrice, query.getMinPrice());
        if (query.getMaxPrice() != null) w.le(Sku::getCostPrice, query.getMaxPrice());

        IPage<Sku> skuPage = this.page(page, w.orderByDesc(Sku::getId));

        // 4. VO 转换：增加“是否安全”前端标识
        List<SkuVO> voList = skuPage.getRecords().stream().map(sku -> {
            SkuVO vo = new SkuVO();
            BeanUtils.copyProperties(sku, vo);
            vo.setIsStockSafe(sku.getStockQuantity() > sku.getAlertQuantity());
            return vo;
        }).collect(Collectors.toList());

        return PageResultVO.of(skuPage.getTotal(), voList);
    }

    /**
     * [修改] SKU 属性：仅限修改编码、规格等，严禁改库存
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateSku(SkuUpdateDTO dto) {
        Sku sku = new Sku();
        BeanUtils.copyProperties(dto, sku);
        sku.setStockQuantity(null); // 强制安全性：库存只能通过入库/订单单据修改
        this.updateById(sku);
    }

    /**
     * [删除] 安全校验：库存清零后才允许逻辑删除
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void safeDeleteSku(Long skuId) {
        Sku sku = this.getById(skuId);
        if (sku == null) return;

        if (sku.getStockQuantity() != null && sku.getStockQuantity() > 0) {
            throw new RuntimeException("规格尚有库存，无法删除");
        }
        this.removeById(skuId);
    }

    /**
     * [入库] 采购补货：原子增加库存 + 记流水
     */
    @Transactional(rollbackFor = Exception.class)
    public void handlePurchaseInbound(PurchaseInboundDTO dto) {
        Sku sku = this.getById(dto.getSkuId());
        if (sku == null) throw new RuntimeException("SKU不存在");

        int beforeCount = sku.getStockQuantity();

        // 【核心】使用 setSql 确保数据库行锁自增，防止并发掉数
        boolean success = this.update()
                .setSql("stock_quantity = stock_quantity + " + dto.getChangeCount())
                .eq("id", dto.getSkuId())
                .update();

        if (!success) throw new RuntimeException("库存更新失败");

        // 记录采购流水
        stockLogService.recordLog(
                sku.getId(),
                dto.getChangeCount(),
                beforeCount,
                StockChangeType.REPLENISH_IN,
                dto.getRelationId(),
                dto.getOperator()
        );
    }

    /**
     * [入库] 售后退货：增加库存 + 强制修改订单状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleRefundInbound(RefundInboundDTO dto) {
        // 1. 查询 SKU 基础信息
        Sku sku = skuMapper.selectById(dto.getSkuId());
        if (sku == null) throw new RuntimeException("SKU不存在");
        int beforeCount = sku.getStockQuantity();

        // 2. 根据平台订单号定位订单
        Order order = orderService.getOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getPlatformOrderSn, dto.getPlatformOrderSn()));

        if (order == null) {
            throw new RuntimeException("未找到平台订单号为 [" + dto.getPlatformOrderSn() + "] 的订单，请核实");
        }

        // 3. 执行库存原子更新
        int rows = skuMapper.updateStock(dto.getSkuId(), dto.getChangeCount());
        if (rows == 0) throw new RuntimeException("库存入库失败");

        // 4. 状态联动：更新订单状态为“售后中”
        order.setStatus(OrderStatus.AFTER_SALE);
        boolean orderUpdated = orderService.updateById(order);
        if (!orderUpdated) throw new RuntimeException("订单状态更新失败");

        // 5. 记录退货流水
        // 注意：这里 relationId 建议存售后单号，dto.getPlatformOrderSn() 作为关联信息存入备注或扩展字段
        stockLogService.recordLog(
                dto.getSkuId(),
                dto.getChangeCount(),
                beforeCount,
                StockChangeType.REFUND_IN,
                dto.getRelationId(), // 这里存退货单号
                dto.getOperator()
        );
    }

    /**
     * [列表] 一次性加载该商品下所有 SKU 明细
     */
    @Override
    public List<SkuVO> listByProductId(Long productId) {
        List<Sku> skus = this.list(new LambdaQueryWrapper<Sku>()
                .eq(Sku::getProductId, productId));

        return skus.stream().map(sku -> {
            SkuVO vo = new SkuVO();
            BeanUtils.copyProperties(sku, vo);
            // 这里可以根据业务增加状态计算逻辑
            return vo;
        }).collect(Collectors.toList());
    }
}