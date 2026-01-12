package com.erp.oms.service.DifyAgent;

import com.erp.oms.dto.difyAgent.AgentChatDTO;
import com.erp.oms.dto.difyAgent.ChatMessageVO;

public interface DifyAgentService {
    ChatMessageVO chatSync(AgentChatDTO chatDTO, String userId);
}
