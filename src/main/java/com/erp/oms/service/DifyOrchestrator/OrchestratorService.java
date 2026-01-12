package com.erp.oms.service.DifyOrchestrator;

import com.erp.oms.dto.api.DifyTaskContextDTO;
import com.erp.oms.dto.viewObject.WorkStatusVO;

public interface OrchestratorService {
    void updateStatus(String workflowName, String status);
    void executeWorkflow(DifyTaskContextDTO context);
    void updateStatusWithData(String workflowName, String status, Object data);
    WorkStatusVO getStatusAndData(String workflowName);
}
