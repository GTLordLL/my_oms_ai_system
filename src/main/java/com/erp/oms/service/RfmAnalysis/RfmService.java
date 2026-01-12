package com.erp.oms.service.RfmAnalysis;

import com.erp.oms.dto.api.PageResultVO;
import com.erp.oms.dto.pageQuery.RfmQueryDTO;
import com.erp.oms.dto.rfmAnalysis.RfmAnalysisVO;
import com.erp.oms.dto.rfmAnalysis.RfmDashboardVO;
import com.erp.oms.dto.rfmAnalysis.RfmResultDTO;
import com.erp.oms.dto.viewObject.AiAnalysis.BusinessInsightVO;
import com.erp.oms.dto.viewObject.WorkStatusVO;

import java.util.List;

public interface RfmService {
    String initRfmAnalysis();
    void handleAlgorithmCallback(RfmResultDTO resultDTO);
    RfmDashboardVO getRfmDashboard();
    WorkStatusVO getTaskWorkStatus(String taskId);
    PageResultVO<RfmAnalysisVO> getRfmDetailsPage(RfmQueryDTO queryDTO);
    List<BusinessInsightVO.RfmLevelInsight> getAILevelInsights();
}
