package com.bkbank.ledger.dto;

import com.bkbank.ledger.entity.enums.EmploymentType;
import com.bkbank.ledger.entity.enums.Gender;
import com.bkbank.ledger.entity.enums.IdType;

import java.time.LocalDate;

/**
 * DTO for creating a new client
 * Only required fields are mandatory
 */
public class ClientCreateRequest {
    
    // ============================================
    // REQUIRED FIELDS
    // ============================================
    
    private String clientId;
    private String fullName;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String email;
    private String phoneNumber;
    private String address;
    private String idNumber;
    private IdType idType;
    
    // ============================================
    // OPTIONAL FIELDS
    // ============================================
    
    // Extended Contact
    private String city;
    private String country;
    private String homeBranchId;
    
    // ID Details
    private LocalDate idIssueDate;
    private LocalDate idExpiryDate;
    
    // Employment
    private String occupation;
    private String employerName;
    private String employerAddress;
    private EmploymentType employmentType;
    private Double monthlyIncome;
    private Integer yearsAtCurrentJob;
    
    // Getters and Setters
    
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

    public String getHomeBranchId() {
        return homeBranchId;
    }

    public void setHomeBranchId(String homeBranchId) {
        this.homeBranchId = homeBranchId;
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
}
