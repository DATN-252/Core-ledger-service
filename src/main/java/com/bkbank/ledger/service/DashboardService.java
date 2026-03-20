package com.bkbank.ledger.service;

import com.bkbank.ledger.dto.response.DashboardNamedCountResponse;
import com.bkbank.ledger.dto.response.DashboardSummaryResponse;
import com.bkbank.ledger.dto.response.DashboardTransactionTrendResponse;
import com.bkbank.ledger.entity.Transaction;
import com.bkbank.ledger.entity.enums.AccountStatus;
import com.bkbank.ledger.entity.enums.ClientStatus;
import com.bkbank.ledger.repository.ClientRepository;
import com.bkbank.ledger.repository.LoanAccountRepository;
import com.bkbank.ledger.repository.SavingsAccountRepository;
import com.bkbank.ledger.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final DateTimeFormatter DAY_LABEL_FORMAT = DateTimeFormatter.ofPattern("dd/MM");

    private final ClientRepository clientRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final TransactionRepository transactionRepository;

    public DashboardSummaryResponse getSummary() {
        long clientCount = clientRepository.count();
        long activeClientCount = clientRepository.countByStatus(ClientStatus.ACTIVE);

        long loanCount = loanAccountRepository.count();
        long activeLoanCount = loanAccountRepository.countByStatus(AccountStatus.ACTIVE);
        long lockedLoanCount = loanAccountRepository.countByStatus(AccountStatus.LOCKED);

        long savingsCount = savingsAccountRepository.count();
        long activeSavingsCount = savingsAccountRepository.countByStatus(AccountStatus.ACTIVE);

        long totalTransactionCount = transactionRepository.count();
        long failedTransactionCount = transactionRepository.countByStatus("FAILED");
        long successTransactionCount = totalTransactionCount - failedTransactionCount;

        double totalCreditLimit = defaultDouble(loanAccountRepository.sumPrincipal());
        double totalOutstanding = defaultDouble(loanAccountRepository.sumPrincipalOutstanding());

        LocalDate fromDate = LocalDate.now().minusDays(13);
        LocalDateTime fromDateTime = fromDate.atStartOfDay();

        List<Transaction> recentTransactions = transactionRepository.findTop8ByOrderByTransactionDateDesc();
        List<Transaction> recentChartTransactions = transactionRepository.findByTransactionDateGreaterThanEqualOrderByTransactionDateAsc(fromDateTime);

        Map<LocalDate, DashboardTransactionTrendResponse> trendMap = new LinkedHashMap<>();
        for (int i = 0; i < 14; i++) {
            LocalDate day = fromDate.plusDays(i);
            trendMap.put(day, new DashboardTransactionTrendResponse(day.format(DAY_LABEL_FORMAT), 0, 0, 0));
        }

        for (Transaction transaction : recentChartTransactions) {
            if (transaction.getTransactionDate() == null) {
                continue;
            }
            LocalDate day = transaction.getTransactionDate().toLocalDate();
            DashboardTransactionTrendResponse point = trendMap.get(day);
            if (point == null) {
                continue;
            }
            point.setTotal(point.getTotal() + 1);
            if ("FAILED".equalsIgnoreCase(transaction.getStatus())) {
                point.setFailed(point.getFailed() + 1);
            } else {
                point.setSuccess(point.getSuccess() + 1);
            }
        }

        List<DashboardNamedCountResponse> txnTypeData = transactionRepository.countGroupedByTransactionType().stream()
                .map(row -> new DashboardNamedCountResponse(String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .toList();

        return DashboardSummaryResponse.builder()
                .clientCount(clientCount)
                .activeClientCount(activeClientCount)
                .loanCount(loanCount)
                .activeLoanCount(activeLoanCount)
                .lockedLoanCount(lockedLoanCount)
                .savingsCount(savingsCount)
                .activeSavingsCount(activeSavingsCount)
                .totalTransactionCount(totalTransactionCount)
                .successTransactionCount(successTransactionCount)
                .failedTransactionCount(failedTransactionCount)
                .totalCreditLimit(totalCreditLimit)
                .totalOutstanding(totalOutstanding)
                .txnByDay(trendMap.values().stream().toList())
                .txnTypeData(txnTypeData)
                .recentTransactions(recentTransactions)
                .build();
    }

    private double defaultDouble(Double value) {
        return value != null ? value : 0.0;
    }
}
