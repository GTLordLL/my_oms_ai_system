package com.erp.oms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@TableName("oms_stock_log")
public class StockLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long skuId;
    private Integer changeCount; // 变动数量
    private Integer type;        // 1-采购入库, 2-销售出库, 3-退货入库, 4-盘点调整
    private String relationId;   // 关联单据号
    private String operator;     // 操作人
    private LocalDateTime createTime;
}