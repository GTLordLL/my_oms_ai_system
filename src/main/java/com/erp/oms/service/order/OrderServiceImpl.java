package com.erp.oms.service.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.oms.dto.add.OrderCreateDTO;
import com.erp.oms.dto.api.DifyTaskContextDTO;
import com.erp.oms.dto.api.PageResultVO;
import com.erp.oms.dto.pageQuery.OrderQueryDTO;
import com.erp.oms.dto.update.OrderUpdateDTO;
import com.erp.oms.dto.viewObject.CompositeVO.OrderFullVO;
import com.erp.oms.dto.viewObject.OrderItemVO;
import com.erp.oms.dto.viewObject.OrderVO;
import com.erp.oms.dto.viewObject.WorkStatusVO;
import com.erp.oms.entity.Order;
import com.erp.oms.entity.OrderItem;
import com.erp.oms.entity.Sku;
import com.erp.oms.enums.OrderStatus;
import com.erp.oms.enums.StockChangeType;
import com.erp.oms.exception.BizException;
import com.erp.oms.mapper.OrderMapper;
import com.erp.oms.mapper.SkuMapper;
import com.erp.oms.service.DifyOrchestrator.OrchestratorService;
import com.erp.oms.service.orderItem.OrderItemService;
import com.erp.oms.service.stockLog.StockLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单业务实现：处理订单全生命周期（创建、查询、发货、关闭）
 */
@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {
    private final SkuMapper skuMapper;
    private final OrderItemService orderItemService;
    private final StockLogService stockLogService;
    private final OrderMapper orderMapper;
    private final OrchestratorService orchestratorService;

    private static final String WORKFLOW_NAME = "SIMULATE_CUSTOMER";

    @Value("${dify.workflow.api-key.simulate-customer}")
    private String difyApiKey;

    public OrderServiceImpl(OrderItemService orderItemService, SkuMapper skuMapper, StockLogService stockLogService, OrderMapper orderMapper, OrchestratorService orchestratorService) {
        this.orderItemService = orderItemService;
        this.skuMapper = skuMapper;
        this.stockLogService = stockLogService;
        this.orderMapper = orderMapper;
        this.orchestratorService = orchestratorService;
    }

    /**
     * [工具] 内部转换 VO：处理枚举描述及手机号脱敏
     */
    private OrderVO convertToVO(Order order) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);

        // 状态转文字
        OrderStatus statusEnum = order.getStatus();
        if (statusEnum != null) {
            vo.setStatus(statusEnum.getValue());
            vo.setStatusName(statusEnum.getDesc());
        }

        // 脱敏：13812345678 -> 138****5678
        if (StringUtils.hasText(vo.getReceiverMobile())) {
            vo.setReceiverMobile(vo.getReceiverMobile().replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
        }
        return vo;
    }

    /**
     * [查询] 通过id查询单个订单
     */
    @Override
    public OrderVO getByOrderId(Long id) {
        Order order = this.getById(id);
        if (order == null) throw BizException.notFound("订单不存在");
        return convertToVO(order);
    }

    /**
     * [查询] 通过id查询订单详情
     */
    @Override
    public OrderFullVO getOrderDetails(Long orderId) {
        // 1. 获取主表信息并转为 VO
        OrderVO orderVO = this.getByOrderId(orderId);

        // 2. 获取明细列表
        List<OrderItemVO> items = orderItemService.listByOrderId(orderId);

        // 3. 组装
        OrderFullVO fullVO = new OrderFullVO();
        BeanUtils.copyProperties(orderVO, fullVO);
        fullVO.setItems(items);

        return fullVO;
    }

    /**
     * [查询] 订单分页：支持单号、平台、状态及多字段模糊搜索
     */
    @Override
    public PageResultVO<OrderVO> pageByQuery(OrderQueryDTO query) {
        Page<Order> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<Order> w = new LambdaQueryWrapper<>();

        // 1. 固定条件
        if (StringUtils.hasText(query.getPlatformOrderSn())) {
            w.eq(Order::getPlatformOrderSn, query.getPlatformOrderSn());
        }
        if (StringUtils.hasText(query.getSourcePlatform())) {
            w.eq(Order::getSourcePlatform, query.getSourcePlatform());
        }

        // 2. 状态转换：前端传 int -> 后端转 Enum
        if (query.getStatus() != null) {
            OrderStatus statusEnum = OrderStatus.fromValue(query.getStatus());
            w.eq(statusEnum != null, Order::getStatus, statusEnum);
        }

        // 3. 全局搜索：单号/昵称/手机号/快递号
        if (StringUtils.hasText(query.getKeyword())) {
            w.and(wrapper -> wrapper
                    .like(Order::getPlatformOrderSn, query.getKeyword())
                    .or().like(Order::getBuyerNick, query.getKeyword())
                    .or().like(Order::getReceiverMobile, query.getKeyword())
                    .or().like(Order::getLogisticsNo, query.getKeyword())
            );
        }

        // 4. 金额/时间范围过滤
        w.ge(query.getCreateTimeStart() != null, Order::getCreateTime, query.getCreateTimeStart());
        w.le(query.getCreateTimeEnd() != null, Order::getCreateTime, query.getCreateTimeEnd());
        w.ge(query.getMinPayAmount() != null, Order::getPayAmount, query.getMinPayAmount());
        w.le(query.getMaxPayAmount() != null, Order::getPayAmount, query.getMaxPayAmount());

        IPage<Order> orderPage = this.page(page, w.orderByDesc(Order::getCreateTime));

        List<OrderVO> voList = orderPage.getRecords().stream()
                .map(this::convertToVO).collect(Collectors.toList());

        return PageResultVO.of(orderPage.getTotal(), voList);
    }

    /**
     * [核心] 手动下单：扣减库存 -> 保存明细 -> 记录流水
     * 毕业设计：没有支付接口，逻辑上“下单”就等同于“支付成功”。
     */
    @Transactional(rollbackFor = Exception.class)
    public void createManualOrder(OrderCreateDTO dto) {
        // 1. 创建订单主记录
        Order order = new Order();
        BeanUtils.copyProperties(dto, order);

        if (dto.getTotalAmount() == null) {
            BigDecimal calculatedTotal = dto.getItems().stream()
                    .map(item -> item.getUnitPrice().multiply(new BigDecimal(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            order.setTotalAmount(calculatedTotal);
        }

        order.setStatus(OrderStatus.WAIT_DELIVER);
        order.setPayTime(LocalDateTime.now()); // 既然跳过支付接口，下单即支付
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);

        // 2. 遍历明细处理货品
        for (OrderCreateDTO.OrderItemDTO itemDto : dto.getItems()) {
            // [防超卖] 调用 Mapper 执行：update ... set stock = stock - n where id = ? and stock >= n
            int rows = skuMapper.updateStock(itemDto.getSkuId(), -itemDto.getQuantity());

            if (rows == 0) {
                throw new RuntimeException("库存不足，下单失败！SKU ID: " + itemDto.getSkuId());
            }

            // 获取 SKU 快照记录成本和编码
            Sku sku = skuMapper.selectById(itemDto.getSkuId());

            // 保存明细行
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setSkuId(sku.getId());
            item.setSkuCode(sku.getSkuCode());
            item.setQuantity(itemDto.getQuantity());
            item.setUnitPrice(itemDto.getUnitPrice());
            item.setUnitCost(sku.getCostPrice());
            orderItemService.save(item);

            // 记录出库流水
            stockLogService.recordLog(
                    sku.getId(),
                    itemDto.getQuantity(),
                    sku.getStockQuantity() + itemDto.getQuantity(), // 计算变动前库存
                    StockChangeType.SALE_OUT,
                    order.getPlatformOrderSn() != null ? order.getPlatformOrderSn() : "MANUAL_" + order.getId(),
                    "SYSTEM"
            );
        }
    }

    /**
     * [操作] 修改订单：处理收货信息变更或状态强转（发货/关闭）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderManual(OrderUpdateDTO dto) {
        Order oldOrder = this.getById(dto.getId());
        if (oldOrder == null) throw new RuntimeException("订单不存在");

        // 快捷入口：如果状态改为关闭，走关闭逻辑
        if (dto.getStatus() == OrderStatus.CLOSED && oldOrder.getStatus() != OrderStatus.CLOSED) {
            this.closeOrder(dto.getId(), "SYSTEM_ADMIN");
            return;
        }

        // 拦截：已完成订单禁止修改
        if (oldOrder.getStatus() == OrderStatus.COMPLETED || oldOrder.getStatus() == OrderStatus.CLOSED) {
            throw new RuntimeException("当前订单状态[" + oldOrder.getStatus().getDesc() + "]禁止修改收货信息");
        }

        // 拦截：改发货必须填单号
        if (dto.getStatus() == OrderStatus.DELIVERED) {
            String logisticsNo = dto.getLogisticsNo() != null ? dto.getLogisticsNo() : oldOrder.getLogisticsNo();
            if (!StringUtils.hasText(logisticsNo)) throw new RuntimeException("执行发货操作必须填写快递单号");
        }

        Order updateOrder = new Order();
        BeanUtils.copyProperties(dto, updateOrder);
        this.updateById(updateOrder);
    }

    /**
     * [逆向] 关闭订单：状态回流并退回库存
     */
    @Transactional(rollbackFor = Exception.class)
    public void closeOrder(Long orderId, String operator) {
        Order order = this.getById(orderId);
        if (order == null) throw new RuntimeException("订单不存在");

        // 拦截：已发货订单禁止直接关闭（应走退货流程）
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.COMPLETED) {
            throw new RuntimeException("订单已发货或已完成，无法直接关闭");
        }
        if (order.getStatus() == OrderStatus.CLOSED) return;

        // 1. 库存回还逻辑
        List<OrderItem> items = orderItemService.list(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));

        for (OrderItem item : items) {
            // 增加库存
            skuMapper.updateStock(item.getSkuId(), item.getQuantity());

            Sku sku = skuMapper.selectById(item.getSkuId());

            // 记录退还流水
            stockLogService.recordLog(
                    item.getSkuId(),
                    item.getQuantity(),
                    sku.getStockQuantity() - item.getQuantity(),
                    StockChangeType.CANCEL_RETURN,
                    "CLOSE_" + order.getPlatformOrderSn(),
                    operator
            );
        }

        // 2. 更新最终状态
        this.update().set("status", OrderStatus.CLOSED).eq("id", orderId).update();
    }

    /**
     * [逆向] 发货
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void shipOrder(Long orderId, String logisticsNo, String operator) {
        Order order = this.getById(orderId);
        if (order == null) throw new RuntimeException("订单不存在");

        // 1. 状态拦截
        if (order.getStatus() != OrderStatus.WAIT_DELIVER) {
            throw new RuntimeException("订单当前状态为[" + order.getStatus().getDesc() + "]，无法执行发货操作");
        }

        // 2. 参数校验
        if (!StringUtils.hasText(logisticsNo)) {
            throw new RuntimeException("发货必须填写快递单号");
        }

        // 3. 执行更新
        var updateChain = this.lambdaUpdate()
                .set(Order::getStatus, OrderStatus.DELIVERED)
                .set(Order::getLogisticsNo, logisticsNo)
                .set(Order::getDeliverTime, LocalDateTime.now());

        // --- 核心修复：补全支付时间 ---
        // 如果 pay_time 为空（可能是测试数据或跳过支付逻辑的订单），发货时强制补齐
        if (order.getPayTime() == null) {
            // 设为发货前 10 分钟，模拟正常的支付->发货时差
            updateChain.set(Order::getPayTime, LocalDateTime.now().minusMinutes(10));
        }
        // ----------------------------

        boolean success = updateChain
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, OrderStatus.WAIT_DELIVER)
                .update();

        if (!success) {
            throw new RuntimeException("发货失败，订单状态可能已被更改");
        }
    }

    @Override
    @Async("taskExecutor")
    public void trigSimCustomer(Integer times) {
        orchestratorService.updateStatus(WORKFLOW_NAME,"processing");

        for (int i = 0; i < times; i++) {
            try {
                DifyTaskContextDTO context = DifyTaskContextDTO.builder()
                        .workflowName(WORKFLOW_NAME)
                        .apiKey(difyApiKey)
                        .inputs(Map.of("date", LocalDate.now().toString()))
                        .build();

                // 阻塞调用，确保 Dify 接收到请求
                orchestratorService.executeWorkflow(context);

                // 💡 关键：适当的间隔（可选）
                // 如果次数很多，建议每单间隔 1-2 秒，防止触发 LLM 的 RPM 限制
                Thread.sleep(1000);

            } catch (Exception e) {
                log.error("第 {} 次模拟执行失败: {}", i + 1, e.getMessage());
            }
        }
        // 2. 所有任务发送完毕，更新状态为 "success"
        orchestratorService.updateStatus(WORKFLOW_NAME,"success");
    }

    @Override
    public WorkStatusVO checkWorkflowStatus() {
        return orchestratorService.getStatusAndData(WORKFLOW_NAME);
    }
}
