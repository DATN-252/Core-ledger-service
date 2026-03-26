package com.bkbank.ledger.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class PushTokenRegistrationRequest {
    @JsonAlias("token")
    private String expoPushToken;
    private String deviceId;
    private String platform;
    private String appVersion;
}
