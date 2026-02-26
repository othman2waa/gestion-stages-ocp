package com.OCP.Gestion_Stages.exeptions;



public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}