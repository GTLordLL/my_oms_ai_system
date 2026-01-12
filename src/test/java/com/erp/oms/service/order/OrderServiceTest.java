package com.erp.oms.service.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.oms.dto.add.OrderCreateDTO;
import com.erp.oms.dto.api.PageResultVO;
import com.erp.oms.dto.pageQuery.OrderQueryDTO;
import com.erp.oms.dto.update.OrderUpdateDTO;
import com.erp.oms.dto.viewObject.OrderVO;
import com.erp.oms.entity.Order;
import com.erp.oms.entity.Product;
import com.erp.oms.entity.Sku;
import com.erp.oms.entity.StockLog;
import com.erp.oms.enums.OrderStatus;
import com.erp.oms.enums.StockChangeType;
import com.erp.oms.mapper.OrderMapper;
import com.erp.oms.mapper.ProductMapper;
import com.erp.oms.mapper.SkuMapper;
import com.erp.oms.mapper.StockLogMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
@ActiveProfiles("test")
@Transactional // 保证测试完数据自动回滚，不污染 MySQL
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private StockLogMapper stockLogMapper;

    private Long testSkuId;

    @BeforeEach
    void setUp() {
        // 建议：每次清理一下之前的测试数据（可选）
        skuMapper.delete(new LambdaQueryWrapper<Sku>().eq(Sku::getSkuCode, "TEST_ORDER_001"));

        Product product = new Product();
        product.setName("基础测试商品");
        productMapper.insert(product);

        Sku sku = new Sku();
        sku.setProductId(product.getId());
        // 关键：给 Code 加上时间戳，防止重复运行报错
        sku.setSkuCode("TEST_" + System.currentTimeMillis());
        sku.setStockQuantity(10);
        sku.setAlertQuantity(5);
        sku.setCostPrice(new BigDecimal("100.00"));
        skuMapper.insert(sku);
        testSkuId = sku.getId();
    }

    @Test
    @DisplayName("正常下单：验证库存扣减和数据一致性")
    void testCreateManualOrder_Success() {
        // 1. 构造下单请求
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setPlatformOrderSn("MANUAL_SN_001");
        dto.setSourcePlatform("MANUAL");
        dto.setReceiverName("张三");
        dto.setReceiverMobile("13812345678");
        dto.setPayAmount(new BigDecimal("180.00"));
        dto.setTotalAmount(new BigDecimal("200.00"));

        OrderCreateDTO.OrderItemDTO item = new OrderCreateDTO.OrderItemDTO();
        item.setSkuId(testSkuId);
        item.setQuantity(3); // 下单 3 件
        item.setUnitPrice(new BigDecimal("200.00"));
        dto.setItems(Collections.singletonList(item));

        // 2. 执行下单
        orderService.createManualOrder(dto);

        // 3. 断言库存：10 - 3 = 7
        Sku updatedSku = skuMapper.selectById(testSkuId);
        Assertions.assertEquals(7, updatedSku.getStockQuantity(), "库存扣减逻辑错误");

        // 4. 断言订单：是否成功插入
        Order savedOrder = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getPlatformOrderSn, "MANUAL_SN_001"));
        Assertions.assertNotNull(savedOrder);
        Assertions.assertEquals(OrderStatus.WAIT_DELIVER, savedOrder.getStatus());
    }

    @Test
    @DisplayName("异常场景：库存不足时下单应失败并回滚")
    void testCreateManualOrder_OutOfStock() {
        // 1. 构造下单请求：尝试买 11 件（初始只有 10 件）
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setPlatformOrderSn("OVER_ORDER_001");

        OrderCreateDTO.OrderItemDTO item = new OrderCreateDTO.OrderItemDTO();
        item.setSkuId(testSkuId);
        item.setQuantity(11);
        dto.setItems(Collections.singletonList(item));

        // 2. 断言抛出异常
        Assertions.assertThrows(RuntimeException.class, () -> orderService.createManualOrder(dto), "库存不足应抛出异常");

        // 3. 验证回滚：库存应该还是 10，且订单不应存在
        Sku skuAfterFail = skuMapper.selectById(testSkuId);
        Assertions.assertEquals(10, skuAfterFail.getStockQuantity(), "事务未回滚，库存已被错误修改");
    }

    @Test
    @DisplayName("逆向测试：正常关闭待发货订单，验证库存回还和流水")
    void testCloseOrder_Success() {
        // 1. 准备：先成功下一个单（初始10，买3个，剩7个）
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setPlatformOrderSn("CLOSE_TEST_001");
        dto.setSourcePlatform("MANUAL");
        dto.setPayAmount(new BigDecimal("100.00"));
        dto.setTotalAmount(new BigDecimal("100.00"));
        dto.setReceiverName("王五");
        dto.setReceiverMobile("13912345678");

        OrderCreateDTO.OrderItemDTO item = new OrderCreateDTO.OrderItemDTO();
        item.setSkuId(testSkuId);
        item.setQuantity(3);
        item.setUnitPrice(new BigDecimal("100.00"));
        dto.setItems(Collections.singletonList(item));

        orderService.createManualOrder(dto);

        // 获取生成的订单ID
        Order savedOrder = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getPlatformOrderSn, "CLOSE_TEST_001"));
        Long orderId = savedOrder.getId();

        // 2. 执行：调用关闭订单方法
        orderService.closeOrder(orderId, "ADMIN_USER");

        // 3. 断言验证：
        // (1) 订单状态应为 CLOSED (枚举值或数据库值)
        Order closedOrder = orderMapper.selectById(orderId);
        Assertions.assertEquals(OrderStatus.CLOSED, closedOrder.getStatus(), "订单状态未变更为已关闭");

        // (2) 库存应回还：7 + 3 = 10
        Sku recoveredSku = skuMapper.selectById(testSkuId);
        Assertions.assertEquals(10, recoveredSku.getStockQuantity(), "关闭订单后库存未正确退回");

        // (3) 验证库存流水：应存在一条 CANCEL_RETURN 类型的记录
        List<StockLog> logs = stockLogMapper.selectList(new LambdaQueryWrapper<StockLog>()
                .eq(StockLog::getSkuId, testSkuId)
                .eq(StockLog::getType, StockChangeType.CANCEL_RETURN));
        Assertions.assertFalse(logs.isEmpty(), "未记录库存退还流水");
        Assertions.assertEquals(3, logs.get(0).getChangeCount(), "流水记录的变动数量不匹配");
    }

    @Test
    @DisplayName("边界测试：尝试关闭已发货订单，应抛出异常")
    void testCloseOrder_IllegalState() {
        // 1. 准备：创建一个已发货状态的订单（补齐所有必填字段）
        Order order = new Order();
        order.setPlatformOrderSn("DELIVERED_ORDER_001");
        order.setStatus(OrderStatus.DELIVERED); // 已发货状态
        order.setSourcePlatform("TEST");        // 补上缺失的必填字段
        order.setPayAmount(new BigDecimal("100"));
        order.setTotalAmount(new BigDecimal("100"));
        order.setReceiverName("测试员");         // 建议也补上，防止数据库后续还有校验
        order.setReceiverMobile("13000000000");

        orderMapper.insert(order);

        // 2. 执行并断言：尝试关闭它，应该抛出 RuntimeException
        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> orderService.closeOrder(order.getId(), "SYSTEM"));

        // 3. 验证异常信息是否符合预期
        Assertions.assertTrue(exception.getMessage().contains("无法直接关闭"),
                "实际异常信息为: " + exception.getMessage());
    }

    @Test
    @DisplayName("状态机测试：已完成订单禁止修改收货信息")
    void testUpdateOrderManual_CompletedOrder() {
        // 1. 模拟一个已完成的订单
        Order order = new Order();
        order.setPlatformOrderSn("COMPLETED_SN_001");
        order.setStatus(OrderStatus.COMPLETED); // 已完成
        order.setSourcePlatform("TEST");
        order.setPayAmount(new BigDecimal("100"));
        order.setTotalAmount(new BigDecimal("100"));
        order.setReceiverName("老客户");
        order.setReceiverMobile("13011112222");
        orderMapper.insert(order);

        // 2. 尝试修改收货人信息
        OrderUpdateDTO updateDto = new OrderUpdateDTO();
        updateDto.setId(order.getId());
        updateDto.setReceiverName("想修改的名字");

        // 3. 断言：应抛出异常
        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> orderService.updateOrderManual(updateDto));

        // 4. 验证报错信息
        Assertions.assertTrue(exception.getMessage().contains("禁止修改"),
                "未拦截已完成订单的修改操作");
    }

    @Test
    @DisplayName("状态机测试：执行发货操作但未填写物流单号，应拦截")
    void testUpdateOrderManual_DeliverWithoutNo() {
        // 1. 模拟一个待发货订单
        Order order = new Order();
        order.setPlatformOrderSn("WAIT_DELIVER_SN_002");
        order.setStatus(OrderStatus.WAIT_DELIVER); // 待发货
        order.setSourcePlatform("TEST");
        order.setPayAmount(new BigDecimal("100"));
        order.setTotalAmount(new BigDecimal("100"));
        orderMapper.insert(order);

        // 2. 构造修改请求：将状态改为“已发货”，但不给物流单号
        OrderUpdateDTO deliverDto = new OrderUpdateDTO();
        deliverDto.setId(order.getId());
        deliverDto.setStatus(OrderStatus.DELIVERED); // 尝试变更为已发货
        deliverDto.setLogisticsNo(""); // 空单号

        // 3. 断言并验证
        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> orderService.updateOrderManual(deliverDto));

        Assertions.assertTrue(exception.getMessage().contains("必须填写快递单号"),
                "未拦截无单号发货操作");
    }

    @Test
    @DisplayName("并发测试：模拟高并发下单，确保不会超卖")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void testConcurrency_PreventOverSell() throws InterruptedException {
        String uniqueId = String.valueOf(System.nanoTime()).substring(10); // 取纳秒后几位
        // 1. 准备环境：必须先创建一个真实的 Product 拿到 ID
        Product product = new Product();
        product.setName("并发商品_" + uniqueId);
        productMapper.insert(product);

        Sku sku = new Sku();
        sku.setProductId(product.getId());
        sku.setSkuCode("CONC_" + uniqueId); // 动态 SKU Code
        sku.setStockQuantity(5);
        sku.setAlertQuantity(2);
        sku.setCostPrice(new BigDecimal("10"));
        skuMapper.insert(sku);
        Long skuId = sku.getId();

        // 2. 构造两个下单请求：每个请求都想买 3 个（总共需要 6 个，库存不足）
        OrderCreateDTO dto1 = createSimpleOrderDTO(skuId, 3, "REQ_001");
        OrderCreateDTO dto2 = createSimpleOrderDTO(skuId, 3, "REQ_002");

        // 3. 使用 CountDownLatch 模拟并发起跑
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        Runnable task1 = () -> {
            try {
                latch.await(); // 等待发令枪
                orderService.createManualOrder(dto1);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
                System.out.println("线程1下单失败: " + e.getMessage());
            }
        };

        Runnable task2 = () -> {
            try {
                latch.await(); // 等待发令枪
                Thread.sleep(50); // 故意慢 50 毫秒，确保排队顺序清晰
                orderService.createManualOrder(dto2);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
                System.out.println("线程2下单失败: " + e.getMessage());
            }
        };

        // 4. 启动线程
        Thread t1 = new Thread(task1);
        Thread t2 = new Thread(task2);
        t1.start();
        t2.start();

        latch.countDown(); // 发令枪响！
        t1.join();
        t2.join();

        // 5. 断言验证
        // 预期：必须是 1 个成功，1 个失败
        Assertions.assertEquals(1, successCount.get(), "应当只有一个请求成功");
        Assertions.assertEquals(1, failCount.get(), "应当有一个请求因库存不足被拦截");

        // 最终库存应为 5 - 3 = 2，不能是 -1
        Sku finalSku = skuMapper.selectById(skuId);
        Assertions.assertEquals(2, finalSku.getStockQuantity(), "库存最终数值错误，发生了超卖！");
    }

    // 辅助工具方法：快速创建简易 DTO
    private OrderCreateDTO createSimpleOrderDTO(Long skuId, Integer qty, String sn) {
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setPlatformOrderSn(sn);
        dto.setSourcePlatform("TEST");
        dto.setPayAmount(new BigDecimal("100"));
        dto.setTotalAmount(new BigDecimal("100"));
        dto.setReceiverName("并发测试");
        dto.setReceiverMobile("13000000000");

        OrderCreateDTO.OrderItemDTO item = new OrderCreateDTO.OrderItemDTO();
        item.setSkuId(skuId);
        item.setQuantity(qty);
        item.setUnitPrice(new BigDecimal("100"));
        dto.setItems(Collections.singletonList(item));
        return dto;
    }

    @Test
    @DisplayName("综合搜索：验证关键字、时间范围和金额的分页过滤")
    @Transactional // 加上事务，测试完自动回滚，不留垃圾
    void testPageByQuery_ComplexFiltering() {
        // 1. 生成本次测试的唯一隔离标记
        String testTag = "TEST_" + System.currentTimeMillis();

        // 2. 准备差异化数据 (传入 testTag 作为 sourcePlatform)
        createOrderForSearch("SN_A", "张三", "13812345678", new BigDecimal("200"), LocalDateTime.now().minusDays(1), testTag);
        createOrderForSearch("SN_B", "李四", "13988888888", new BigDecimal("500"), LocalDateTime.now(), testTag);
        createOrderForSearch("SN_C", "王五", "13800000000", new BigDecimal("1000"), LocalDateTime.now().plusDays(1), testTag);

        // --- 场景 1: 测试关键字 + 隔离标记 ---
        OrderQueryDTO query1 = new OrderQueryDTO();
        query1.setKeyword("138");
        query1.setSourcePlatform(testTag); // 关键：只查本次的数据
        query1.setPage(1L);
        query1.setSize(10L);

        PageResultVO<OrderVO> result1 = orderService.pageByQuery(query1);
        Assertions.assertEquals(2, result1.getTotal(), "关键字 '138' 应该匹配到2条记录");

        // --- 场景 2: 测试金额范围 + 隔离标记 ---
        OrderQueryDTO query2 = new OrderQueryDTO();
        query2.setSourcePlatform(testTag); // 关键
        query2.setMinPayAmount(new BigDecimal("300"));
        query2.setMaxPayAmount(new BigDecimal("800"));
        query2.setPage(1L);
        query2.setSize(10L);

        PageResultVO<OrderVO> result2 = orderService.pageByQuery(query2);
        Assertions.assertEquals(1, result2.getTotal());
        Assertions.assertEquals("SN_B", result2.getList().get(0).getPlatformOrderSn());

        // --- 场景 3: 测试时间范围 + 隔离标记 ---
        OrderQueryDTO query3 = new OrderQueryDTO();
        query3.setSourcePlatform(testTag); // 关键
        query3.setCreateTimeStart(LocalDateTime.now().minusDays(2));
        query3.setCreateTimeEnd(LocalDateTime.now().plusMinutes(1));
        query3.setPage(1L);
        query3.setSize(10L);

        PageResultVO<OrderVO> result3 = orderService.pageByQuery(query3);
        // 此时结果一定是 2（SN_A 和 SN_B），不会受到 REQ_001 等旧数据干扰
        Assertions.assertEquals(2, result3.getTotal());
    }

    // 更新辅助方法，支持传入 platform
    private void createOrderForSearch(String sn, String name, String phone, BigDecimal amount, LocalDateTime time, String platform) {
        Order order = new Order();
        order.setPlatformOrderSn(sn);
        order.setReceiverName(name);
        order.setReceiverMobile(phone);
        order.setTotalAmount(amount);
        order.setPayAmount(amount);
        order.setSourcePlatform(platform); // 设置隔离标记
        order.setStatus(OrderStatus.WAIT_DELIVER);
        order.setCreateTime(time);
        orderMapper.insert(order);
    }
}