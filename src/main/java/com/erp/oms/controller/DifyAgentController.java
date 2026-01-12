package com.erp.oms.controller;

import com.erp.oms.dto.api.ResultVO;
import com.erp.oms.dto.difyAgent.AgentChatDTO;
import com.erp.oms.dto.difyAgent.ChatMessageVO;
import com.erp.oms.service.DifyAgent.DifyAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/agent")
@Tag(name = "AI 智能助手", description = "与 Dify Agent 进行对话交互")
public class DifyAgentController {

    @Resource
    private DifyAgentService difyAgentService;

    /**
     * 同步对话接口
     * 流程：前端请求 -> 后端阻塞等待 Dify 完成 -> 返回完整回答
     */
    @PostMapping("/chat")
    @Operation(summary = "同步对话", description = "发送问题并一次性获取完整 AI 回答")
    public ResultVO<ChatMessageVO> chat(@RequestBody @Validated AgentChatDTO chatDTO) {

        // 1. 建议从安全上下文获取当前用户 ID，不要硬编码
        // String userId = SecurityUtils.getUserId();
        String userId = "user_123456";

        log.info("📩 收到 AI 对话请求: query={}, conversationId={}",
                chatDTO.getQuery(), chatDTO.getConversationId());

        // 2. 直接调用同步方法
        // 注意：由于 Agent 思考可能较慢，建议前端设置较长的 Timeout（如 60s+）
        ChatMessageVO response = difyAgentService.chatSync(chatDTO, userId);

        return ResultVO.success(response);
    }
}