package com.OCP.Gestion_Stages.Repository;



import com.OCP.Gestion_Stages.domain.model.Etablissement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EtablissementRepository extends JpaRepository<Etablissement, Long> {
    List<Etablissement> findByVille(String ville);
    List<Etablissement> findByType(String type);
    boolean existsByNom(String nom);
}