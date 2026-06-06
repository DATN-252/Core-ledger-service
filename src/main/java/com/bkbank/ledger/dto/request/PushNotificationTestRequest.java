package com.bkbank.ledger.dto.request;

import lombok.Data;

import java.util.Map;

@Data
public class PushNotificationTestRequest {
    private String expoPushToken;
    private String title;
    private String body;
    private Map<String, Object> data;
}
