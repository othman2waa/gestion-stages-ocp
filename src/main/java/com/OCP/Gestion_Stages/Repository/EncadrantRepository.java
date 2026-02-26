package com.OCP.Gestion_Stages.Repository;


import com.OCP.Gestion_Stages.domain.model.Encadrant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EncadrantRepository extends JpaRepository<Encadrant, Long> {
    Optional<Encadrant> findByEmail(String email);
    Optional<Encadrant> findByUserId(Long userId);
    List<Encadrant> findByDepartementId(Long departementId);
}