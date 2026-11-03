package com.finedge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import com.finedge.entity.UserInfo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
/**
 * Entity for {@code loan_applications} (loan intake staging table).
 *
 * <p>The only auto-increment key in the dump: {@code id INT AUTO_INCREMENT}.
 * {@code ref} is the unique business reference (e.g. {@code MF-...}).
 * Persisted financial figures ({@code emi}, {@code total_interest},
 * {@code total_payable}) are stored as supplied — never recalculated here
 * (senior Q7). {@code status} is free text in the dump
 * ({@code 'Pending Review'} default), so it is mapped as {@code String},
 * not an enum.
 *
 * <p>Part D3: {@code cust_user_id} references {@code user_info.pk_user_id}
 * to associate each application with its authenticated borrower. Existing
 * records may have a NULL borrower (no arbitrary ownership is assigned).
 * No intake→{@code loan_info} relationship is invented (senior Q3).
 */
@Entity
@Table(name = "loan_applications")
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Integer id;

    @Column(name = "ref", length = 50, nullable = false, unique = true)
    private String ref;

    @Column(name = "cust_name", length = 100, nullable = false)
    private String customerName;

    @Column(name = "cust_phone", length = 15, nullable = false)
    private String customerPhone;

    @Column(name = "cust_email", length = 100, nullable = false)
    private String customerEmail;

    @Column(name = "cust_dob")
    private LocalDate customerDob;

    @Column(name = "cust_pan", length = 20)
    private String customerPan;

    @Column(name = "cust_city", length = 100, nullable = false)
    private String customerCity;

    @Column(name = "cust_address", length = 255, nullable = false)
    private String customerAddress;

    @Column(name = "financier_key", length = 50, nullable = false)
    private String financierKey;

    @Column(name = "financier_label", length = 100, nullable = false)
    private String financierLabel;

    @Column(name = "loan_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal loanAmount;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(name = "interest_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal interestRate;

    @Column(name = "emi", precision = 15, scale = 2, nullable = false)
    private BigDecimal emi;

    @Column(name = "total_interest", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalInterest;

    @Column(name = "total_payable", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalPayable;

    @Column(name = "status", length = 30, nullable = false)
    private String status = "Pending Review";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "cust_user_id", nullable = true)
    private UserInfo borrower;

    public LoanApplication() {
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = "Pending Review";
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public LocalDate getCustomerDob() {
        return customerDob;
    }

    public void setCustomerDob(LocalDate customerDob) {
        this.customerDob = customerDob;
    }

    public String getCustomerPan() {
        return customerPan;
    }

    public void setCustomerPan(String customerPan) {
        this.customerPan = customerPan;
    }

    public String getCustomerCity() {
        return customerCity;
    }

    public void setCustomerCity(String customerCity) {
        this.customerCity = customerCity;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = customerAddress;
    }

    public String getFinancierKey() {
        return financierKey;
    }

    public void setFinancierKey(String financierKey) {
        this.financierKey = financierKey;
    }

    public String getFinancierLabel() {
        return financierLabel;
    }

    public void setFinancierLabel(String financierLabel) {
        this.financierLabel = financierLabel;
    }

    public BigDecimal getLoanAmount() {
        return loanAmount;
    }

    public void setLoanAmount(BigDecimal loanAmount) {
        this.loanAmount = loanAmount;
    }

    public Integer getTenureMonths() {
        return tenureMonths;
    }

    public void setTenureMonths(Integer tenureMonths) {
        this.tenureMonths = tenureMonths;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public BigDecimal getEmi() {
        return emi;
    }

    public void setEmi(BigDecimal emi) {
        this.emi = emi;
    }

    public BigDecimal getTotalInterest() {
        return totalInterest;
    }

    public void setTotalInterest(BigDecimal totalInterest) {
        this.totalInterest = totalInterest;
    }

    public BigDecimal getTotalPayable() {
        return totalPayable;
    }

    public void setTotalPayable(BigDecimal totalPayable) {
        this.totalPayable = totalPayable;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public UserInfo getBorrower() {
        return borrower;
    }

    public void setBorrower(UserInfo borrower) {
        this.borrower = borrower;
    }
}
