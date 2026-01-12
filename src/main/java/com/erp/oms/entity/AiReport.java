package com.erp.oms.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 智能经营报表：存储 AI 生成的分析结论与推理过程")
@TableName(value = "oms_ai_report", autoResultMap = true) // 开启 autoResultMap 以支持 JSON 解析
public class AiReport {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "报告归属日期（数据统计的截止日期）", example = "2023-10-27")
    private LocalDate reportDate;

    @Schema(description = "报告类型: 1-经营日报, 2-周报, 3-季度分析", example = "1")
    private Integer reportType;

    @Schema(description = "AI 生成的分析报告正文 (Markdown 格式)")
    private String content;

    @Schema(description = "AI 的推理思维链内容 (Reasoning Content)，还原思考过程")
    private String reasoning;

    @Schema(description = "生成报告时的原始核心指标快照")
    private String summaryData;

    @Schema(description = "生成该报告的大模型名称", example = "GLM-4-FlashX")
    private String modelName;

    @Schema(description = "该次生成消耗的 Token 总量", example = "2307")
    private Integer tokensUsed;

    @Schema(description = "操作人（系统自动生成或管理员手动触发）", example = "SYSTEM")
    private String operator;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "记录创建时间")
    private LocalDateTime createTime;
}