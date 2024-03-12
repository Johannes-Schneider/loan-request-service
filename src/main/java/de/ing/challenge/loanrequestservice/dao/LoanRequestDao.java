package de.ing.challenge.loanrequestservice.dao;

import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface LoanRequestDao extends JpaRepository<LoanRequest, Long> {

    @Nonnull
    Collection<LoanRequest> findAllByCustomerId(final long customerId);
}
