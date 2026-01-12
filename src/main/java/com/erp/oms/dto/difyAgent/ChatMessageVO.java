package com.erp.oms.dto.difyAgent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "对话消息响应视图")
public class ChatMessageVO {

    @Schema(description = "消息唯一标识")
    private String messageId;

    @Schema(description = "会话ID，方便前端更新当前 Session")
    private String conversationId;

    @Schema(description = "回答正文 (Markdown)")
    private String content;

    @Schema(description = "产生时间")
    private LocalDateTime createdAt;

}