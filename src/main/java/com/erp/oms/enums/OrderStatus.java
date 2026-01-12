package com.erp.oms.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

/* 关于“扣库存”的时机
* 下单减库存： 优点是绝对不超卖，缺点是恶意下单会占用库存（配合“已关闭”状态释放库存）。
* 支付减库存： 优点是保证购买意愿，缺点是支付瞬时并发高时可能超卖。
* 现在使用的是‘支付减库存‘模式
* */
@Getter
public enum OrderStatus implements IEnum<Integer> {
    WAIT_PAY(0, "待付款"),
    WAIT_DELIVER(1, "待发货"),
    DELIVERED(2, "已发货"),
    COMPLETED(3, "已完成"),
    CLOSED(4, "已关闭"),
    AFTER_SALE(5, "售后中");

    private final Integer value; // 数据库存储的值
    private final String desc;   // 前端显示的文字

    OrderStatus(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    /**
     * MyBatis-Plus 映射枚举的关键方法
     */
    @Override
    public Integer getValue() {
        return this.value;
    }

    /**
     * 根据数值获取枚举对象（可选，手动转换时很有用）
     */
    public static OrderStatus fromValue(Integer value) {
        for (OrderStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return null;
    }
}
