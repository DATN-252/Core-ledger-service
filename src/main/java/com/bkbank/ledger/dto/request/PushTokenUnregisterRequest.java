package com.bkbank.ledger.dto.request;

import lombok.Data;

@Data
public class PushTokenUnregisterRequest {
    private String expoPushToken;
    private String deviceId;
}
