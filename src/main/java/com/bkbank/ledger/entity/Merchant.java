package com.bkbank.ledger.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "merchants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Merchant {

    public static final String DEFAULT_SETTLEMENT_BANK_NAME = "BKBank Merchant Network";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false, unique = true)
    private String merchantId;

    @Column(nullable = false)
    private String name;

    private String category;
    private String addressLine;
    private String ward;
    private String district;
    private String postalCode;
    private Double latitude;
    private Double longitude;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "city_reference_id")
    private CityReference cityReference;

    @Column(name = "settlement_account_number")
    private String settlementAccountNumber;

    @Column(name = "settlement_account_name")
    private String settlementAccountName;

    @Column(name = "settlement_bank_name")
    private String settlementBankName = DEFAULT_SETTLEMENT_BANK_NAME;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_account_id")
    private SavingsAccount settlementAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MerchantStatus status = MerchantStatus.ACTIVE;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public String getResolvedSettlementAccountNumber() {
        if (settlementAccount != null && settlementAccount.getAccountNumber() != null && !settlementAccount.getAccountNumber().isBlank()) {
            return settlementAccount.getAccountNumber();
        }
        return settlementAccountNumber != null && !settlementAccountNumber.isBlank()
                ? settlementAccountNumber
                : merchantId;
    }

    public String getResolvedSettlementAccountName() {
        if (settlementAccount != null && settlementAccount.getClientName() != null && !settlementAccount.getClientName().isBlank()) {
            return settlementAccount.getClientName();
        }
        return settlementAccountName != null && !settlementAccountName.isBlank()
                ? settlementAccountName
                : name;
    }

    public String getResolvedSettlementBankName() {
        return settlementBankName != null && !settlementBankName.isBlank()
                ? settlementBankName
                : DEFAULT_SETTLEMENT_BANK_NAME;
    }

    public Double getResolvedSettlementAccountBalance() {
        return settlementAccount != null ? settlementAccount.getBalance() : null;
    }

    public enum MerchantStatus {
        ACTIVE, INACTIVE
    }

    @Transient
    public String getDisplayAddress() {
        StringBuilder builder = new StringBuilder();
        appendPart(builder, addressLine);
        appendPart(builder, ward);
        appendPart(builder, district);
        if (cityReference != null) {
            appendPart(builder, cityReference.getCityName());
            appendPart(builder, cityReference.getCountry());
        }
        appendPart(builder, postalCode);
        return builder.length() > 0 ? builder.toString() : name;
    }

    private void appendPart(StringBuilder builder, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(", ");
        }
        builder.append(value);
    }
}
