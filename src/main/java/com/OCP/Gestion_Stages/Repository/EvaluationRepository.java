package com.OCP.Gestion_Stages.Repository;

import com.OCP.Gestion_Stages.domain.model.Evaluation;
import com.OCP.Gestion_Stages.domain.enums.TypeEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    List<Evaluation> findByStageId(Long stageId);
    List<Evaluation> findByEncadrantId(Long encadrantId);
    Optional<Evaluation> findByStageIdAndTypeEval(Long stageId, TypeEvaluation typeEval);

    @Query("SELECT AVG(e.note) FROM Evaluation e WHERE e.stage.id = :stageId")
    Double findAverageNoteByStageId(Long stageId);
    @Query("SELECT AVG(e.note) FROM Evaluation e")
    Double findAverageNote();
}