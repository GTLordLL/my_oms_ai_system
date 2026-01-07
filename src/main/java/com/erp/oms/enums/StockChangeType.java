package com.erp.oms.enums;

import lombok.Getter;

@Getter
public enum StockChangeType {
    PURCHASE_IN(1), // 采购入库
    SALE_OUT(2),    // 销售出库
    REFUND_IN(3),   // 退货入库
    ADJUST(4);      // 盘点/手工调整

    private final int code;

    StockChangeType(int code) { this.code = code; }

    public static StockChangeType fromCode(int code) {
        for (StockChangeType t : values()) {
            if (t.code == code) return t;
        }
        throw new IllegalArgumentException("Unknown StockChangeType code: " + code);
    }
}
