package com.OCP.Gestion_Stages.Repository;


import com.OCP.Gestion_Stages.domain.model.Departement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DepartementRepository extends JpaRepository<Departement, Long> {
    Optional<Departement> findByCode(String code);
    boolean existsByCode(String code);
}