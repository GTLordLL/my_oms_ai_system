package com.erp.oms.dto.difyAgent;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@Schema(description = "AI 智能体对话同步请求")
public class AgentChatDTO {

    @NotBlank(message = "内容不能为空")
    @Schema(description = "用户的提问或指令", example = "分析一下目前的库存风险")
    private String query;

    @Schema(description = "会话ID，若需关联历史上下文则必传", example = "d123-456-789")
    private String conversationId;

    @Schema(description = "业务参数输入", example = "{\"warehouse\": \"A1\"}")
    private Map<String, Object> inputs = new HashMap<>();

}