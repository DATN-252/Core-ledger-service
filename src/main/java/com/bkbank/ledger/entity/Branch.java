package com.bkbank.ledger.entity;

import com.bkbank.ledger.entity.enums.BranchStatus;
import com.bkbank.ledger.entity.enums.BranchType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "branches")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String branchId;

    @Column(unique = true, nullable = false, length = 50)
    private String branchCode;

    @Column(nullable = false, length = 255)
    private String branchName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BranchType branchType = BranchType.BRANCH;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BranchStatus status = BranchStatus.ACTIVE;

    @Column(length = 500)
    private String addressLine;

    @Column(length = 120)
    private String cityName;

    @Column(length = 20)
    private String phoneNumber;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
