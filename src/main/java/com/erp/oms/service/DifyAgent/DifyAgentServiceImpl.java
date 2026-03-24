package com.erp.oms.service.DifyAgent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.erp.oms.dto.difyAgent.AgentChatDTO;
import com.erp.oms.dto.difyAgent.ChatMessageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;


import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class DifyAgentServiceImpl implements DifyAgentService {

    @Value("${dify.agent.url}")
    private String agentUrl;

    @Value("${dify.agent.cloud.api-key}")
    private String apiKey;

    private final WebClient webClient;

    public DifyAgentServiceImpl(WebClient.Builder webClientBuilder) {
        // 同步模式下，建议增加底层连接池的超时配置
        this.webClient = webClientBuilder.build();
    }

    @Override
    public ChatMessageVO chatSync(AgentChatDTO chatDTO, String userId) {
        log.info("🚀 启动 Agent 伪同步对话: User={}", userId);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("inputs", chatDTO.getInputs());
        requestBody.put("query", chatDTO.getQuery());
        requestBody.put("response_mode", "streaming");
        requestBody.put("user", userId);
        requestBody.put("conversation_id", chatDTO.getConversationId());

        AtomicReference<String> convIdRef = new AtomicReference<>("");

        try {
            List<String> answerChunks = webClient.post()
                    .uri(agentUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class)
                    // .doOnNext(chunk -> log.info("Dify Raw Chunk: {}", chunk)) // 💡 关键日志：看到底返回了什么
                    .flatMap(chunk -> {
                        String[] lines = chunk.split("\n");
                        List<String> results = new ArrayList<>();
                        for (String line : lines) {
                            String cleanLine = line.trim();
                            // 只要包含 JSON 结构就尝试解析
                            if (cleanLine.contains("{") && cleanLine.contains("}")) {
                                try {
                                    // 兼容 "data: {" 和直接 "{" 的情况
                                    String jsonStr = cleanLine.contains("data:")
                                            ? cleanLine.substring(cleanLine.indexOf("{")).trim()
                                            : cleanLine;

                                    JSONObject json = JSON.parseObject(jsonStr);

                                    if (json.containsKey("conversation_id")) {
                                        convIdRef.set(json.getString("conversation_id"));
                                    }

                                    String event = json.getString("event");
                                    // Agent 模式下，文本可能在 answer 字段，也可能在某些特定的消息事件里
                                    String answer = json.getString("answer");

                                    // 这里的逻辑改为：只要有 answer 字段且不是 ping 事件就拿走
                                    if (StringUtils.hasText(answer) && !"ping".equals(event)) {
                                        results.add(answer);
                                    }
                                } catch (Exception ignored) {
                                    // 忽略非 JSON 行
                                }
                            }
                        }
                        return Flux.fromIterable(results);
                    })
                    .collectList()
                    .block(Duration.ofMinutes(2));

            String fullAnswer = (answerChunks != null) ? String.join("", answerChunks) : "";

            log.info("✅ 最终拼接内容: [{}]", fullAnswer);

            return ChatMessageVO.builder()
                    .content(fullAnswer)
                    .conversationId(convIdRef.get())
                    .createdAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("🔥 Agent 调用严重异常: ", e);
            return ChatMessageVO.builder().content("AI 助手响应失败: " + e.getMessage()).build();
        }
    }
}