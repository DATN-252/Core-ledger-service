package com.bkbank.ledger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementScheduler {

    private final SettlementService settlementService;

    @Value("${settlement.auto.enabled:false}")
    private boolean enabled;

    @Value("${settlement.auto.days-offset:1}")
    private int daysOffset;

    @Value("${settlement.auto.fee-rate:1.5}")
    private double feeRate;

    @Value("${settlement.auto.execute:true}")
    private boolean execute;

    @Scheduled(cron = "${settlement.auto.cron:0 0 1 * * *}")
    public void runDailySettlement() {
        if (!enabled) {
            return;
        }

        LocalDate settlementDate = LocalDate.now().minusDays(daysOffset);
        log.info("Running automatic settlement for date {} with feeRate {} execute {}", settlementDate, feeRate, execute);
        settlementService.runAutomaticSettlement(settlementDate, feeRate, execute);
    }
}
