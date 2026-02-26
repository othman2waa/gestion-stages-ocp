package com.OCP.Gestion_Stages.Service.interfaces;

public interface DocumentService {
    byte[] generateConventionPdf(Long conventionId);
    byte[] generateAttestationPdf(Long stageId);
}