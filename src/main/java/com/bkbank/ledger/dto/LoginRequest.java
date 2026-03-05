package com.bkbank.ledger.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String nameAcc; // Mobile app uses this field instead of username
    private String password;

    public String getEffectiveUsername() {
        return (nameAcc != null && !nameAcc.isBlank()) ? nameAcc : username;
    }
}
