package de.ing.challenge.loanrequestservice.dao;

import jakarta.annotation.Nonnull;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity(name = "LOAN_REQUEST")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class LoanRequest {
    @Id
    @Column(name = "ID", nullable = false)
    private long id;

    @Column(name = "AMOUNT", nullable = false)
    @Nonnull
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "CUSTOMER_ID")
    @Nonnull
    private Customer customer;
}
