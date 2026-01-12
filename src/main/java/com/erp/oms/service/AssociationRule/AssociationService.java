package com.erp.oms.service.AssociationRule;

import com.erp.oms.dto.association.AssociationResultDTO;
import com.erp.oms.dto.association.AssociationRuleVO;
import com.erp.oms.dto.viewObject.WorkStatusVO;

import java.util.List;

public interface AssociationService {
    String initAssociationAnalysis();
    WorkStatusVO getTaskWorkStatus(String taskId);
    void handleAlgorithmCallback(AssociationResultDTO resultDTO);
    List<AssociationRuleVO> getAllRulesVO();
    List<AssociationRuleVO> getStrongRules(int limit);
}
