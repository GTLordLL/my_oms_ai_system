package com.erp.oms.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

@Getter
public enum StockChangeType implements IEnum<Integer> {
    NEW_PRODUCT_IN(1, "商品上新", 1),
    SALE_OUT(2, "销售出库", -1),
    REFUND_IN(3, "退货入库", 1),
    ADJUST(4, "盘点调整", 0),       // 调整可能是加也可能是减，设为0表示由业务决定
    REPLENISH_IN(5, "采购补货", 1),
    CANCEL_RETURN(6, "订单取消回库", 1);

    @EnumValue // MyBatis-Plus 识别此字段存入数据库
    private final int code;
    private final String desc;
    private final int symbol; // 1代表增加，-1代表减少

    StockChangeType(int code, String desc, int symbol) {
        this.code = code;
        this.desc = desc;
        this.symbol = symbol;
    }

    @Override
    public Integer getValue() {
        return this.code;
    }

    public static StockChangeType fromCode(int code) {
        for (StockChangeType t : values()) {
            if (t.getCode() == code) return t;
        }
        return null; // 建议不要直接抛异常，返回null由业务判断
    }
}