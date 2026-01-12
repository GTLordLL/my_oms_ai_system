package com.erp.oms.service.DifyOrchestrator;

import com.erp.oms.dto.api.DifyTaskContextDTO;
import com.erp.oms.dto.viewObject.WorkStatusVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class OrchestratorServiceImpl implements OrchestratorService {
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${dify.workflow.url}")
    private String difyUrl;

    private String getRedisKey(String workflowName) {
        return "AI_TASK:" + workflowName + ":" + LocalDate.now();
    }

    // 查询状态与数据
    @Override
    public WorkStatusVO getStatusAndData(String workflowName) {
        String key = getRedisKey(workflowName);
        Object status = redisTemplate.opsForValue().get(key);
        Object data = redisTemplate.opsForValue().get(key + ":result");

        return WorkStatusVO.builder()
                .status(status != null ? status.toString() : "none")
                .data(data)
                .build();
    }

    @Override
    public void updateStatus(String workflowName, String status) {
        String key = getRedisKey(workflowName);
        // 建议设置过期时间，防止 Redis 堆积大量过期的状态信息
        redisTemplate.opsForValue().set(key, status, 1, TimeUnit.HOURS);
    }

    @Override
    public void updateStatusWithData(String workflowName, String status, Object data) {
        String key = getRedisKey(workflowName);
        // 存储状态
        redisTemplate.opsForValue().set(key, status, 1, TimeUnit.HOURS);
        // 存储关联数据（如 ID）
        if (data != null) {
            redisTemplate.opsForValue().set(key + ":result", data, 1, TimeUnit.HOURS);
        }
    }

    // 异步执行核心逻辑（通用）
    @Async("taskExecutor")
    public void executeWorkflow(DifyTaskContextDTO context) {
        String key = getRedisKey(context.getWorkflowName());
        try {
            redisTemplate.opsForValue().set(key, "processing", 30, TimeUnit.MINUTES);

            // 封装请求
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(context.getApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("inputs", context.getInputs());
            body.put("user", "system_admin");
            body.put("response_mode", "blocking");

            log.info("开始执行 AI 工作流: {}", context.getWorkflowName());
            restTemplate.postForEntity(difyUrl, new HttpEntity<>(body, headers), String.class);

            redisTemplate.opsForValue().set(key, "success");
        } catch (Exception e) {
            log.error("AI 工作流执行异常: {}", context.getWorkflowName(), e);
            redisTemplate.opsForValue().set(key, "error");
        }
    }
}
