package com.erp.oms.service.orderItem;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.oms.dto.api.PageResultVO;
import com.erp.oms.dto.pageQuery.OrderItemQueryDTO;
import com.erp.oms.dto.viewObject.OrderItemVO;
import com.erp.oms.entity.OrderItem;
import com.erp.oms.mapper.OrderItemMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单明细实现类：处理订单下具体的 SKU 关联数据与利润计算
 */
@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItem> implements OrderItemService {
    /**
     * [查询] 明细分页：支持按订单ID、SKU、价格区间筛选
     */
    @Override
    public PageResultVO<OrderItemVO> pageByQuery(OrderItemQueryDTO query) {
        Page<OrderItem> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<OrderItem> w = new LambdaQueryWrapper<>();

        // 1. 基础过滤：按所属订单、SKU、编码模糊匹配
        if (query.getOrderId() != null) w.eq(OrderItem::getOrderId, query.getOrderId());
        if (query.getSkuId() != null) w.eq(OrderItem::getSkuId, query.getSkuId());
        if (StringUtils.hasText(query.getKeyword())) w.like(OrderItem::getSkuCode, query.getKeyword());

        // 2. 价格区间过滤
        if (query.getMinPrice() != null) w.ge(OrderItem::getUnitPrice, query.getMinPrice());
        if (query.getMaxPrice() != null) w.le(OrderItem::getUnitPrice, query.getMaxPrice());

        IPage<OrderItem> itemPage = this.page(page, w.orderByDesc(OrderItem::getId));

        // 3. 封装 VO 并同步计算【核心金额】
        List<OrderItemVO> voList = itemPage.getRecords().stream().map(item -> {
            OrderItemVO vo = new OrderItemVO();
            BeanUtils.copyProperties(item, vo);

            // 【计算1】行总价 = 单价 * 数量
            BigDecimal total = item.getUnitPrice().multiply(new BigDecimal(item.getQuantity()));
            vo.setTotalPrice(total);

            // 【计算2】预计利润 = (单价 - 成本) * 数量
            if (item.getUnitCost() != null) {
                BigDecimal profit = item.getUnitPrice()
                        .subtract(item.getUnitCost())
                        .multiply(new BigDecimal(item.getQuantity()));
                vo.setEstimatedProfit(profit);
            }

            return vo;
        }).collect(Collectors.toList());

        return PageResultVO.of(itemPage.getTotal(), voList);
    }

    /**
     * [列表] 订单详情专用：一次性加载该订单下所有 SKU 明细
     */
    @Override
    public List<OrderItemVO> listByOrderId(Long orderId) {
        if (orderId == null) return Collections.emptyList();

        List<OrderItem> list = this.list(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));

        return list.stream().map(item -> {
            OrderItemVO vo = new OrderItemVO();
            BeanUtils.copyProperties(item, vo);

            // 1. 准备计算数据（防空指针）
            BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal cost = item.getUnitCost() != null ? item.getUnitCost() : BigDecimal.ZERO;
            BigDecimal qty = item.getQuantity() != null ? new BigDecimal(item.getQuantity()) : BigDecimal.ZERO;

            // 2. 计算成交总额：单价 * 数量
            vo.setTotalPrice(price.multiply(qty));

            // 3. 计算预估利润：(单价 - 成本) * 数量
            // 核心修复点在这里
            BigDecimal profit = price.subtract(cost).multiply(qty);
            vo.setEstimatedProfit(profit);

            return vo;
        }).collect(Collectors.toList());
    }
}
