package com.bkbank.ledger.service;

import com.bkbank.ledger.event.TransactionNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionNotificationEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(Long transactionId) {
        if (transactionId == null) {
            return;
        }
        applicationEventPublisher.publishEvent(new TransactionNotificationEvent(transactionId));
    }
}
