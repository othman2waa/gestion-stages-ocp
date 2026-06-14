package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.PointageRepository;
import com.OCP.Gestion_Stages.Repository.StageRepository;
import com.OCP.Gestion_Stages.Service.interfaces.PointageService;
import com.OCP.Gestion_Stages.domain.dto.pointage.PointageBulkRequest;
import com.OCP.Gestion_Stages.domain.dto.pointage.PointageRequest;
import com.OCP.Gestion_Stages.domain.dto.pointage.PointageResponse;
import com.OCP.Gestion_Stages.domain.model.Pointage;
import com.OCP.Gestion_Stages.domain.model.Stage;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PointageServiceImpl implements PointageService {

    private final PointageRepository pointageRepository;
    private final StageRepository stageRepository;

    @Override
    public List<PointageResponse> saveAll(PointageBulkRequest request) {
        Stage stage = stageRepository.findById(request.getStageId())
                .orElseThrow(() -> new ResourceNotFoundException("Stage introuvable"));

        List<Pointage> saved = new ArrayList<>();
        for (PointageRequest pr : request.getPointages()) {
            Pointage pointage = pointageRepository
                    .findByStageIdAndDatePointage(stage.getId(), pr.getDate())
                    .orElse(new Pointage());
            pointage.setStage(stage);
            pointage.setDatePointage(pr.getDate());
            pointage.setPresent(pr.getPresent());
            pointage.setMotif(pr.getMotif());
            saved.add(pointageRepository.save(pointage));
        }
        return saved.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<PointageResponse> findByStage(Long stageId) {
        return pointageRepository.findByStageIdOrderByDatePointageAsc(stageId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private PointageResponse toResponse(Pointage p) {
        PointageResponse r = new PointageResponse();
        r.setId(p.getId());
        r.setStageId(p.getStage().getId());
        r.setDate(p.getDatePointage());
        r.setPresent(p.getPresent());
        r.setMotif(p.getMotif());
        return r;
    }
}
