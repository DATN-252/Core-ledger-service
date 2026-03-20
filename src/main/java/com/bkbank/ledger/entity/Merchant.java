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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false, unique = true)
    private String merchantId;

    @Column(nullable = false)
    private String name;

    private String category;
    private Double latitude;
    private Double longitude;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "city_reference_id")
    private CityReference cityReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MerchantStatus status = MerchantStatus.ACTIVE;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum MerchantStatus {
        ACTIVE, INACTIVE
    }
}
