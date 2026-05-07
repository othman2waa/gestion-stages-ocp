package com.OCP.Gestion_Stages.Repository;

import com.OCP.Gestion_Stages.domain.model.ArchiveStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArchiveStageRepository extends JpaRepository<ArchiveStage, Long> {
    Optional<ArchiveStage> findByStageId(Long stageId);
    boolean existsByStageId(Long stageId);
    List<ArchiveStage> findByAnneeStageOrderByDateArchivageDesc(Integer annee);
    List<ArchiveStage> findByDepartementNomOrderByDateArchivageDesc(String dept);
    List<ArchiveStage> findAllByOrderByDateArchivageDesc();

    @Query("SELECT DISTINCT a.anneeStage FROM ArchiveStage a ORDER BY a.anneeStage DESC")
    List<Integer> findDistinctAnnees();

    @Query("SELECT DISTINCT a.departementNom FROM ArchiveStage a ORDER BY a.departementNom")
    List<String> findDistinctDepartements();

    @Query("SELECT AVG(a.noteFinale) FROM ArchiveStage a WHERE a.noteFinale IS NOT NULL")
    Double findAverageNote();
}
