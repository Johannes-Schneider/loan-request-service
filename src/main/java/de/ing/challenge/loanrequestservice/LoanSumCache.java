package de.ing.challenge.loanrequestservice;

import de.ing.challenge.loanrequestservice.dao.LoanRequest;
import de.ing.challenge.loanrequestservice.dao.LoanRequestDao;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class LoanSumCache {
    @Nonnull
    private final LoanRequestDao loanRequestDao;
    @Nonnull
    @Getter(AccessLevel.PACKAGE) // for testing
    private final Map<Long, BigDecimal> summedLoans = new ConcurrentHashMap<>();

    public LoanSumCache(@Autowired @Nonnull final LoanRequestDao loanRequestDao) {
        this.loanRequestDao = loanRequestDao;
    }

    @Nonnull
    public Optional<BigDecimal> get(final long customerId) {
        final BigDecimal sum = summedLoans.computeIfAbsent(customerId, this::fetchSumFromDatabase);

        return Optional.ofNullable(sum);
    }

    public BigDecimal insertOrAdd(final long customerId, @Nonnull final BigDecimal newLoan) {
        return summedLoans.compute(customerId, (k, v) -> {
            final BigDecimal existingSum;
            if (v == null) {
                existingSum = Objects.requireNonNullElse(fetchSumFromDatabase(customerId), BigDecimal.ZERO);
            } else {
                existingSum = v;
            }

            return existingSum.add(newLoan);
        });
    }

    @Nullable
    private BigDecimal fetchSumFromDatabase(final long customerId) {
        final Collection<LoanRequest> loanRequests = loanRequestDao.findAllByCustomerId(customerId);
        if (loanRequests.isEmpty()) {
            return null;
        }

        return loanRequests.stream()
                .map(LoanRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    void reset() {
        summedLoans.clear();
    }
}
