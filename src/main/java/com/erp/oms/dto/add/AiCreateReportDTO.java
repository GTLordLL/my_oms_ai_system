package com.erp.oms.dto.add;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;


@Data
@Schema(description = "AI 报告回传数据传输对象")
public class AiCreateReportDTO {
    @NotBlank(message = "报告内容不能为空")
    @Schema(description = "AI 生成的 Markdown 正文")
    private String content;

    @Schema(description = "AI 的推理过程 (Reasoning Content)")
    private String reasoning;

    @NotNull(message = "报告日期不能为空")
    @Schema(description = "报告对应的业务日期", example = "2023-10-27")
    private LocalDate reportDate;

    @Schema(description = "报告类型: 1-日报, 2-周报", example = "1")
    private Integer reportType;

    @Schema(description = "本次调用消耗的 Token 总数")
    private Integer totalTokens;

    @Schema(description = "使用的模型名称", example = "GLM-4-FlashX")
    private String modelName;

    @Schema(description = "生成报告时的原始指标快照")
    private String summaryData;
}
