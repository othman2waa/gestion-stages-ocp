package com.OCP.Gestion_Stages.Service.interfaces;

import com.OCP.Gestion_Stages.domain.dto.rapport.RapportResponse;
import com.OCP.Gestion_Stages.domain.model.RapportStage;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

public interface RapportStageService {
    RapportResponse upload(Long stageId, MultipartFile file, String username) throws IOException;
    RapportStage download(Long stageId);
    RapportResponse getMeta(Long stageId);
    void delete(Long stageId);
    List<RapportResponse> getMesRapports(String username);
}