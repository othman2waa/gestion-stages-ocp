package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Service.interfaces.PointageService;
import com.OCP.Gestion_Stages.domain.dto.pointage.PointageBulkRequest;
import com.OCP.Gestion_Stages.domain.dto.pointage.PointageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pointages")
@RequiredArgsConstructor
public class PointageController {

    private final PointageService pointageService;

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ENCADRANT','ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<PointageResponse>> saveBulk(@RequestBody PointageBulkRequest request) {
        return ResponseEntity.ok(pointageService.saveAll(request));
    }

    @GetMapping("/stage/{stageId}")
    @PreAuthorize("hasAnyRole('ENCADRANT','ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<List<PointageResponse>> getByStage(@PathVariable Long stageId) {
        return ResponseEntity.ok(pointageService.findByStage(stageId));
    }
}
