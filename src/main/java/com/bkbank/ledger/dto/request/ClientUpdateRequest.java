package com.bkbank.ledger.dto;

import com.bkbank.ledger.entity.enums.ClientStatus;
import com.bkbank.ledger.entity.enums.EmploymentType;
import com.bkbank.ledger.entity.enums.Gender;

import java.time.LocalDate;

/**
 * DTO for updating client information
 * All fields are optional - only send what needs to be updated
 */
public class ClientUpdateRequest {
    
    private String fullName;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String email;
    private String phoneNumber;
    private String address;
    private String city;
    private String country;
    private String homeBranchId;
    
    // Can't update ID number (immutable)
    
    private LocalDate idIssueDate;
    private LocalDate idExpiryDate;
    
    private String occupation;
    private String employerName;
    private String employerAddress;
    private EmploymentType employmentType;
    private Double monthlyIncome;
    private Integer yearsAtCurrentJob;
    
    private ClientStatus status;  // Admin can change status
    
    // Getters and Setters
    
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

    public ClientStatus getStatus() {
        return status;
    }

    public void setStatus(ClientStatus status) {
        this.status = status;
    }
}
