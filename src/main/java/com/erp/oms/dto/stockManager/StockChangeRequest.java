package com.erp.oms.dto.stockManager;

import com.erp.oms.enums.StockChangeType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockChangeRequest {
    @NotNull(message = "skuId 不能为空")
    private Long skuId;

    /** 可以为正（入库）也可以为负（出库） */
    @NotNull(message = "changeCount 不能为空")
    private Integer changeCount;

    @NotNull(message = "type 不能为空")
    private StockChangeType type;

    /** 关联单号（订单号 / 采购单号 等），非必需但推荐 */
    @Size(max = 100, message = "relationId 长度不能超过100")
    private String relationId;

    /** 操作人：建议从登录态取，controller 层可不强制传 */
    @Size(max = 50, message = "operator 长度不能超过50")
    private String operator;
}
