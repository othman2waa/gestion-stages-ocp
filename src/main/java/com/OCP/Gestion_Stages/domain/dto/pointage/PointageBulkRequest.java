package com.OCP.Gestion_Stages.domain.dto.pointage;

import lombok.Data;

import java.util.List;

@Data
public class PointageBulkRequest {
    private Long stageId;
    private List<PointageRequest> pointages;
}
