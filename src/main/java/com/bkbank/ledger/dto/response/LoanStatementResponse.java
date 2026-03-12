package com.bkbank.ledger.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanStatementResponse {
    private String accountNumber;
    private String currency;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Double creditLimit;
    private Double openingOutstanding;
    private Double totalCharges;
    private Double totalPayments;
    private Double closingOutstanding;
    private Double availableCredit;
    private Integer transactionCount;
    private List<LoanStatementItemResponse> items;
}
