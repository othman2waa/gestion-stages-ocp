package com.OCP.Gestion_Stages.Repository;


import com.OCP.Gestion_Stages.domain.model.Convention;
import com.OCP.Gestion_Stages.domain.enums.ConventionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConventionRepository extends JpaRepository<Convention, Long> {
    Optional<Convention> findByStageId(Long stageId);
    Optional<Convention> findByNumero(String numero);
    List<Convention> findByStatut(ConventionStatus statut);
}