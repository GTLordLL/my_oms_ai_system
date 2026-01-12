package com.erp.oms.dto.api;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class DifyTaskContextDTO {
    private String workflowName;     // 用于 Redis Key 的区分
    private String apiKey;           // 不同工作流可能对应不同 API Key
    private Map<String, Object> inputs; // 工作流入参
}
