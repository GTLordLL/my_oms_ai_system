package com.erp.oms.dto.association;

import lombok.Data;

import java.util.List;

@Data
public class AssociationResultDTO {
    private String taskId;

    // 算法产出的所有规则
    private List<SingleRuleDTO> rules;
}