package com.erp.oms.service.DemandForecast;

import com.erp.oms.dto.demandForecast.ForecastResultDTO;
import com.erp.oms.dto.viewObject.WorkStatusVO;
import com.erp.oms.dto.demandForecast.SKUForecastVO;

import java.util.List;

public interface ForecastService {
    String initManualForecast(Long skuId);
    WorkStatusVO getTaskWorkStatus(String taskId);
    void handleAlgorithmCallback(ForecastResultDTO resultDTO);
    SKUForecastVO getForecastView(Long skuId);
    List<SKUForecastVO> getAllForecastDashboard();
    List<SKUForecastVO> getHighRiskSkus(int limit);
}
