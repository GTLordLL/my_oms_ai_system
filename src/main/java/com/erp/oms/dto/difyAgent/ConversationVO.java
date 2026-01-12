package com.erp.oms.dto.difyAgent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "会话历史简述")
public class ConversationVO {
    private String id;        // Dify 的 conversation_id
    private String name;      // 对话标题（通常是第一句话）
    private LocalDateTime updatedAt;
    private String lastMessage;
}