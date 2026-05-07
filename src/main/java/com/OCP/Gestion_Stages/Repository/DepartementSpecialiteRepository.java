package com.OCP.Gestion_Stages.Repository;

import com.OCP.Gestion_Stages.domain.model.DepartementSpecialite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DepartementSpecialiteRepository extends JpaRepository<DepartementSpecialite, Long> {
    List<DepartementSpecialite> findByDepartementIdOrderByNomAsc(Long departementId);
}