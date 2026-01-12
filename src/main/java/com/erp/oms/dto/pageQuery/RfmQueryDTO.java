package com.erp.oms.dto.pageQuery;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "RFM 详情分页查询参数")
public class RfmQueryDTO extends PageQueryDTO {

    @Schema(description = "客户分层等级 (如：重要价值客户)", example = "重要价值客户")
    private String level;

    @Schema(description = "搜索关键词 (手机号或昵称)", example = "13800138000")
    private String keyword;
}