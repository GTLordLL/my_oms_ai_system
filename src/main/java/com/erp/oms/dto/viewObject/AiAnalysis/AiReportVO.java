package com.erp.oms.dto.viewObject.AiAnalysis;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "AI 智能经营报表显示对象")
public class AiReportVO {
    @Schema(description = "报告ID", example = "17688123456789")
    private Long id;

    @Schema(description = "报告归属日期", example = "2023-10-27")
    private LocalDate reportDate;

    @Schema(description = "报告类型描述 (日报/周报/季报)", example = "经营日报")
    private String reportTypeName;

    @Schema(description = "AI 分析报告正文 (Markdown 格式)")
    private String content;

    @Schema(description = "AI 推理过程 (思维链)")
    private String reasoning;

    @Schema(description = "生成报告时的原始指标快照 (前端可直接解析的对象)")
    private String summaryData;

    @Schema(description = "所用模型", example = "GLM-4-FlashX")
    private String modelName;

    @Schema(description = "Token 消耗", example = "2307")
    private Integer tokensUsed;

    @Schema(description = "生成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}