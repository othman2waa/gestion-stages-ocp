package com.OCP.Gestion_Stages.domain.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pointages", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"stage_id", "date_pointage"})
})
@Data
public class Pointage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;

    @Column(name = "date_pointage", nullable = false)
    private LocalDate datePointage;

    @Column(nullable = false)
    private Boolean present = true;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
