package com.erp.oms.service.AiAnalysis;


import com.erp.oms.dto.add.AiCreateReportDTO;
import com.erp.oms.dto.api.PageResultVO;
import com.erp.oms.dto.viewObject.AiAnalysis.AiReportVO;
import com.erp.oms.dto.viewObject.AiAnalysis.BusinessInsightVO;
import com.erp.oms.dto.viewObject.WorkStatusVO;

public interface AiAnalysisService {
    BusinessInsightVO getFullBusinessInsight();
    Long saveAiAnalysisReport(AiCreateReportDTO dto);
    AiReportVO getLatestReportVO();
    PageResultVO<AiReportVO> getReportPageVO(int page, int size);
    void triggerReport();
    WorkStatusVO checkStatus();
    AiReportVO getReportById(Long id);
}
