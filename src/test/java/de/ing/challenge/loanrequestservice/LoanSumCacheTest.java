package de.ing.challenge.loanrequestservice;

import de.ing.challenge.loanrequestservice.dao.LoanRequest;
import de.ing.challenge.loanrequestservice.dao.LoanRequestDao;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class LoanSumCacheTest {
    private static final long CUSTOMER_ID = 42;
    @Nonnull
    private static final LoanRequest FIRST_LOAN = mockLoanRequestDao(1_500.0d);
    @Nonnull
    private static final LoanRequest SECOND_LOAN = mockLoanRequestDao(750.0d);
    @Nonnull
    private static final BigDecimal SUMMED_AMOUNT = FIRST_LOAN.getAmount().add(SECOND_LOAN.getAmount());

    @Test
    void get_WithCachedValue_DoesNotAccessDatabase() {
        final LoanRequestDao loanRequestDao = mock(LoanRequestDao.class);
        final LoanSumCache sut = new LoanSumCache(loanRequestDao);

        sut.getSummedLoans().put(CUSTOMER_ID, FIRST_LOAN.getAmount());

        final Optional<BigDecimal> actual = sut.get(CUSTOMER_ID);
        assertActualIsEqualTo(actual, FIRST_LOAN.getAmount());

        verifyNoMoreInteractions(loanRequestDao);
    }

    @Test
    void get_WithoutCachedValue_DoesAccessDatabase() {
        final LoanRequestDao loanRequestDao = mockLoanRequestDao(FIRST_LOAN, SECOND_LOAN);

        final LoanSumCache sut = new LoanSumCache(loanRequestDao);

        final Optional<BigDecimal> actual = sut.get(CUSTOMER_ID);
        assertActualIsEqualTo(actual, SUMMED_AMOUNT);

        verify(loanRequestDao, times(1)).findAllByCustomerId(eq(CUSTOMER_ID));
        verifyNoMoreInteractions(loanRequestDao);
    }

    @Test
    void get_WithoutCachedValue_AndNonExistingCustomer_DoesAccessDatabaseEveryTime() {
        final LoanRequestDao loanRequestDao = mockLoanRequestDao();

        final LoanSumCache sut = new LoanSumCache(loanRequestDao);

        // first access
        final Optional<BigDecimal> firstResult = sut.get(CUSTOMER_ID);
        assertActualIsEqualTo(firstResult, null);
        assertThat(sut.getSummedLoans()).isEmpty();
        verify(loanRequestDao, times(1)).findAllByCustomerId(eq(CUSTOMER_ID));

        // second access
        final Optional<BigDecimal> secondResult = sut.get(CUSTOMER_ID);
        assertActualIsEqualTo(secondResult, null);
        assertThat(sut.getSummedLoans()).isEmpty();
        verify(loanRequestDao, times(2)).findAllByCustomerId(eq(CUSTOMER_ID));
    }

    @Test
    void insertOrAdd_WithCachedValue_DoesNotAccessDatabase() {
        final LoanRequestDao loanRequestDao = mock(LoanRequestDao.class);
        final LoanSumCache sut = new LoanSumCache(loanRequestDao);

        sut.getSummedLoans().put(CUSTOMER_ID, FIRST_LOAN.getAmount());

        sut.insertOrAdd(CUSTOMER_ID, SECOND_LOAN.getAmount());

        final Optional<BigDecimal> actual = sut.get(CUSTOMER_ID);
        assertActualIsEqualTo(actual, SUMMED_AMOUNT);

        verifyNoMoreInteractions(loanRequestDao);
    }

    @Test
    void insertOrAdd_WithoutCachedValue_DoesAccessDatabase() {
        final LoanRequestDao loanRequestDao = mockLoanRequestDao(FIRST_LOAN);
        final LoanSumCache sut = new LoanSumCache(loanRequestDao);

        sut.insertOrAdd(CUSTOMER_ID, SECOND_LOAN.getAmount());

        assertThat(sut.getSummedLoans()).containsOnlyKeys(CUSTOMER_ID);
        assertThat(sut.getSummedLoans().get(CUSTOMER_ID).compareTo(SUMMED_AMOUNT)).isZero();

        verify(loanRequestDao, times(1)).findAllByCustomerId(eq(CUSTOMER_ID));
    }

    @Nonnull
    private static LoanRequestDao mockLoanRequestDao(@Nonnull final LoanRequest... daos) {
        final LoanRequestDao loanRequestDao = mock(LoanRequestDao.class);
        final List<LoanRequest> listOfDaos = Arrays.stream(daos).toList();
        when(loanRequestDao.findAllByCustomerId(any(Long.class))).thenReturn(listOfDaos);

        return loanRequestDao;
    }

    @Nonnull
    private static LoanRequest mockLoanRequestDao(final double amount) {
        final LoanRequest mock = mock(LoanRequest.class);
        when(mock.getAmount()).thenReturn(BigDecimal.valueOf(amount));

        return mock;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static void assertActualIsEqualTo(@Nonnull final Optional<BigDecimal> actual, @Nullable final BigDecimal expected) {
        if (expected == null) {
            assertThat(actual).isEmpty();
            return;
        }

        assertThat(actual).isNotEmpty();
        assertThat(actual.get().compareTo(expected)).isZero();
    }
}
