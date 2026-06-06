package com.bkbank.ledger.dto.response;

import com.bkbank.ledger.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private long clientCount;
    private long activeClientCount;
    private long loanCount;
    private long activeLoanCount;
    private long lockedLoanCount;
    private long savingsCount;
    private long activeSavingsCount;
    private long totalTransactionCount;
    private long successTransactionCount;
    private long failedTransactionCount;
    private double totalCreditLimit;
    private double totalOutstanding;
    private List<DashboardTransactionTrendResponse> txnByDay;
    private List<DashboardNamedCountResponse> txnTypeData;
    private List<Transaction> recentTransactions;
}
