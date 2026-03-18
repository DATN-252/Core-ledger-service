package com.bkbank.ledger.service;

import com.bkbank.ledger.dto.response.MerchantSettlementPreviewResponse;
import com.bkbank.ledger.entity.Merchant;
import com.bkbank.ledger.entity.Transaction;
import com.bkbank.ledger.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final MerchantService merchantService;
    private final TransactionRepository transactionRepository;

    public MerchantSettlementPreviewResponse previewSettlement(String merchantId,
                                                              LocalDate fromDate,
                                                              LocalDate toDate,
                                                              Double feeRate) {
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate must be before or equal to toDate");
        }

        Merchant merchant = merchantService.getActiveMerchant(merchantId);
        double appliedFeeRate = feeRate != null ? feeRate : 0.0;
        if (appliedFeeRate < 0) {
            throw new IllegalArgumentException("feeRate must be greater than or equal to 0");
        }

        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(LocalTime.MAX);

        List<Transaction> transactions = transactionRepository
                .findByMerchantIdAndStatusAndTransactionDateBetweenOrderByTransactionDateAsc(
                        merchantId,
                        "SUCCESS",
                        from,
                        to
                );

        double grossAmount = transactions.stream()
                .filter(this::affectsSettlement)
                .mapToDouble(this::signedSettlementAmount)
                .sum();

        int transactionCount = (int) transactions.stream()
                .filter(this::affectsSettlement)
                .count();

        double feeAmount = grossAmount * appliedFeeRate / 100.0;
        double netAmount = grossAmount - feeAmount;

        return new MerchantSettlementPreviewResponse(
                merchant.getMerchantId(),
                merchant.getName(),
                "USD",
                fromDate,
                toDate,
                transactionCount,
                grossAmount,
                appliedFeeRate,
                feeAmount,
                netAmount
        );
    }

    private boolean affectsSettlement(Transaction tx) {
        return "CHARGE".equalsIgnoreCase(tx.getTransactionType())
                || "WITHDRAWAL".equalsIgnoreCase(tx.getTransactionType())
                || "REFUND".equalsIgnoreCase(tx.getTransactionType())
                || "REVERSAL".equalsIgnoreCase(tx.getTransactionType());
    }

    private double signedSettlementAmount(Transaction tx) {
        double amount = tx.getAmount() != null ? tx.getAmount() : 0.0;
        if ("REFUND".equalsIgnoreCase(tx.getTransactionType())
                || "REVERSAL".equalsIgnoreCase(tx.getTransactionType())) {
            return -amount;
        }
        return amount;
    }
}
