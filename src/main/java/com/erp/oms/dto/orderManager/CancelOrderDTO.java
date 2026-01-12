package com.erp.oms.dto.orderManager;

import lombok.Data;

@Data
public class CancelOrderDTO {
    private String operator;
    private String reason; // 预留取消原因，手动进销存很常用
}
