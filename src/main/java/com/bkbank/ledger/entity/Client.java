package com.bkbank.ledger.entity;

import com.bkbank.ledger.entity.enums.ClientStatus;
import com.bkbank.ledger.entity.enums.EmploymentType;
import com.bkbank.ledger.entity.enums.Gender;
import com.bkbank.ledger.entity.enums.IdType;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Client Entity - Represents a bank customer
 * 
 * Design: Option B - Core Required + Optional Fields
 * - 10 Required fields: Basic info to identify and contact client
 * - 9 Optional fields: Additional info for credit assessment
 */
@Entity
@Table(name = "clients")
@EntityListeners(AuditingEntityListener.class)
public class Client {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // ============================================
    // CORE FIELDS (Required for client creation)
    // ============================================
    
    @Column(unique = true, nullable = false, length = 50)
    private String clientId;  // e.g., "CLI_001"
    
    @Column(nullable = false, length = 255)
    private String fullName;
    
    @Column(nullable = false)
    private LocalDate dateOfBirth;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;
    
    @Column(unique = true, nullable = false, length = 255)
    private String email;
    
    @Column(nullable = false, length = 20)
    private String phoneNumber;
    
    @Column(nullable = false, length = 500)
    private String address;
    
    @Column(unique = true, nullable = false, length = 50)
    private String idNumber;  // CMND/CCCD/Passport number
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IdType idType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClientStatus status;  // Default: ACTIVE
    
    // ============================================
    // OPTIONAL FIELDS (Can be null, update later)
    // ============================================
    
    // Extended Contact
    @Column(length = 100)
    private String city;
    
    @Column(length = 100)
    private String country;
    
    // ID Details
    private LocalDate idIssueDate;
    private LocalDate idExpiryDate;
    
    // Employment Information (for credit assessment)
    @Column(length = 255)
    private String occupation;
    
    @Column(length = 255)
    private String employerName;
    
    @Column(length = 500)
    private String employerAddress;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EmploymentType employmentType;
    
    private Double monthlyIncome;
    
    private Integer yearsAtCurrentJob;
    
    // ============================================
    // AUDIT FIELDS (Auto-generated)
    // ============================================
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    // ============================================
    // RELATIONSHIPS
    // ============================================
    
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SavingsAccount> savingsAccounts = new ArrayList<>();
    
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LoanAccount> loanAccounts = new ArrayList<>();
    
    // ============================================
    // CONSTRUCTORS
    // ============================================
    
    public Client() {
        this.status = ClientStatus.ACTIVE;  // Default status
    }
    
    // ============================================
    // GETTERS & SETTERS
    // ============================================
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public IdType getIdType() {
        return idType;
    }

    public void setIdType(IdType idType) {
        this.idType = idType;
    }

    public ClientStatus getStatus() {
        return status;
    }

    public void setStatus(ClientStatus status) {
        this.status = status;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public LocalDate getIdIssueDate() {
        return idIssueDate;
    }

    public void setIdIssueDate(LocalDate idIssueDate) {
        this.idIssueDate = idIssueDate;
    }

    public LocalDate getIdExpiryDate() {
        return idExpiryDate;
    }

    public void setIdExpiryDate(LocalDate idExpiryDate) {
        this.idExpiryDate = idExpiryDate;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public String getEmployerName() {
        return employerName;
    }

    public void setEmployerName(String employerName) {
        this.employerName = employerName;
    }

    public String getEmployerAddress() {
        return employerAddress;
    }

    public void setEmployerAddress(String employerAddress) {
        this.employerAddress = employerAddress;
    }

    public EmploymentType getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(EmploymentType employmentType) {
        this.employmentType = employmentType;
    }

    public Double getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(Double monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public Integer getYearsAtCurrentJob() {
        return yearsAtCurrentJob;
    }

    public void setYearsAtCurrentJob(Integer yearsAtCurrentJob) {
        this.yearsAtCurrentJob = yearsAtCurrentJob;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<SavingsAccount> getSavingsAccounts() {
        return savingsAccounts;
    }

    public void setSavingsAccounts(List<SavingsAccount> savingsAccounts) {
        this.savingsAccounts = savingsAccounts;
    }

    public List<LoanAccount> getLoanAccounts() {
        return loanAccounts;
    }

    public void setLoanAccounts(List<LoanAccount> loanAccounts) {
        this.loanAccounts = loanAccounts;
    }
    
    // ============================================
    // BUSINESS METHODS
    // ============================================
    
    /**
     * Check if ID is expired
     */
    public boolean isIdExpired() {
        if (idExpiryDate == null) {
            return false;  // No expiry (old CMND)
        }
        return LocalDate.now().isAfter(idExpiryDate);
    }
    
    /**
     * Check if ID expires within the next 6 months
     */
    public boolean isIdExpiringSoon() {
        if (idExpiryDate == null) {
            return false;
        }
        LocalDate sixMonthsFromNow = LocalDate.now().plusMonths(6);
        return idExpiryDate.isBefore(sixMonthsFromNow);
    }
    
    /**
     * Calculate available credit based on income
     * Simple rule: 3-5x monthly income depending on employment type
     */
    public Double calculateSuggestedCreditLimit() {
        if (monthlyIncome == null) {
            return 10000000.0;  // Default 10M VND
        }
        
        double multiplier = switch (employmentType != null ? employmentType : EmploymentType.UNEMPLOYED) {
            case FULL_TIME -> 5.0;
            case PART_TIME -> 3.0;
            case SELF_EMPLOYED -> 4.0;
            case RETIRED -> 2.0;
            case STUDENT, UNEMPLOYED -> 1.0;
        };
        
        return monthlyIncome * multiplier;
    }
}
