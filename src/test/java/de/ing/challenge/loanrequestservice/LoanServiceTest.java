package de.ing.challenge.loanrequestservice;

import de.ing.challenge.loanrequestservice.dao.Customer;
import de.ing.challenge.loanrequestservice.dao.CustomerDao;
import de.ing.challenge.loanrequestservice.dao.LoanRequest;
import de.ing.challenge.loanrequestservice.dao.LoanRequestDao;
import de.ing.challenge.loanrequestservice.dto.LoanRequestDto;
import jakarta.annotation.Nonnull;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ResourceLock(TestResources.DATABASE)
@ResourceLock(TestResources.LOAN_SUM_CACHE)
class LoanServiceTest {

    private static final LoanRequestDto LOAN_REQUEST_DTO = new LoanRequestDto(1337L, BigDecimal.valueOf(1_000.0d), 42L, "Customer");

    @Autowired
    private Validator validator;
    @Autowired
    private CustomerDao customerDao;
    @Autowired
    private LoanSumCache loanSumCache;
    @Autowired
    private LoanRequestDao loanRequestDao;

    @BeforeEach
    @AfterEach
    void cleanup() {
        loanRequestDao.deleteAll();
        customerDao.deleteAll();
        loanSumCache.reset();
    }

    @Test
    @Transactional
    void createLoanRequest_WithNewLoan_AndNewCustomer_IsSaved() {
        final LoanService sut = createSut();

        final LoanRequest loanRequest = sut.createLoanRequest(LOAN_REQUEST_DTO);

        assertDataOfDto(loanRequest, LOAN_REQUEST_DTO);
        assertIsPersisted(LOAN_REQUEST_DTO);
    }

    @Test
    @Transactional
    void createLoanRequest_WithNewLoan_AndExistingCustomer_IsSaved() {
        final LoanService sut = createSut();

        persistCustomer(LOAN_REQUEST_DTO);

        final LoanRequest loanRequest = sut.createLoanRequest(LOAN_REQUEST_DTO);

        assertDataOfDto(loanRequest, LOAN_REQUEST_DTO);
        assertIsPersisted(LOAN_REQUEST_DTO);
    }

    @Test
    @Transactional
    void createLoanRequest_WithExistingLoanRequest_IsIdempotent() {
        final LoanService sut = createSut();

        final LoanRequest loanRequest = sut.createLoanRequest(LOAN_REQUEST_DTO);

        assertDataOfDto(loanRequest, LOAN_REQUEST_DTO);
        assertIsPersisted(LOAN_REQUEST_DTO);

        // we are calling the method a second time with the exact same parameters
        // therefore, we are expecting to service to be idempotent, i.e. have the same behavior
        assertThatNoException().isThrownBy(() -> sut.createLoanRequest(LOAN_REQUEST_DTO));
    }

    @Test
    @Transactional
    void createLoanRequest_WithNewLoan_AndInconsistentCustomer_LeadsToException() {
        final LoanService sut = createSut();

        persistCustomer(new Customer(LOAN_REQUEST_DTO.getCustomerId(), LOAN_REQUEST_DTO.getCustomerFullName() + " Bar"));

        assertThatThrownBy(() -> sut.createLoanRequest(LOAN_REQUEST_DTO))
                .isExactlyInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("The customer id is already in use.");
    }

    @Test
    @Transactional
    void createLoanRequest_WithInconsistentAmount_LeadsToException() {
        final LoanService sut = createSut();

        final Customer customer = persistCustomer(LOAN_REQUEST_DTO);
        persistLoanRequest(new LoanRequest(LOAN_REQUEST_DTO.getId(), LOAN_REQUEST_DTO.getAmount().add(BigDecimal.ONE), customer));

        assertThatThrownBy(() -> sut.createLoanRequest(LOAN_REQUEST_DTO))
                .isExactlyInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("The loan request id is already in use.");
    }

    @Test
    @Transactional
    void createLoanRequest_WithInconsistentCustomerId_LeadsToException() {
        final LoanService sut = createSut();

        final Customer customer = persistCustomer(new Customer(LOAN_REQUEST_DTO.getCustomerId() + 1, LOAN_REQUEST_DTO.getCustomerFullName()));
        persistLoanRequest(new LoanRequest(LOAN_REQUEST_DTO.getId(), LOAN_REQUEST_DTO.getAmount(), customer));

        assertThatThrownBy(() -> sut.createLoanRequest(LOAN_REQUEST_DTO))
                .isExactlyInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("The loan request id is already in use.");
    }

    @Test
    void getLoanSumByCustomerId_WithCacheHit_ReturnsSum() {
        final LoanSumCache loanSumCache = mock(LoanSumCache.class);
        when(loanSumCache.get(anyLong())).thenReturn(Optional.of(BigDecimal.ONE));

        final LoanService sut = createSut(loanSumCache);

        final BigDecimal actual = sut.getLoanSumByCustomerId(LOAN_REQUEST_DTO.getCustomerId());

        assertThat(actual).isSameAs(BigDecimal.ONE);
    }

    @Test
    void getLoanSumByCustomerId_WithCacheMiss_LeadsException() {
        final LoanSumCache loanSumCache = mock(LoanSumCache.class);
        when(loanSumCache.get(anyLong())).thenReturn(Optional.empty());

        final LoanService sut = createSut(loanSumCache);

        assertThatThrownBy(() -> sut.getLoanSumByCustomerId(LOAN_REQUEST_DTO.getCustomerId()))
                .isExactlyInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Customer with id %s not found.".formatted(LOAN_REQUEST_DTO.getCustomerId()));
    }

    @Test
    void createLoanRequest_WithNewLoan_UpdatesLoanSumCache() {
        final LoanSumCache loanSumCache = mock(LoanSumCache.class);
        doAnswer(invocation -> invocation.getArgument(1, BigDecimal.class)).when(loanSumCache).insertOrAdd(anyLong(), any());

        final LoanService sut = createSut(loanSumCache);

        final LoanRequest loanRequest = sut.createLoanRequest(LOAN_REQUEST_DTO);

        verify(loanSumCache).insertOrAdd(eq(LOAN_REQUEST_DTO.getCustomerId()), same(LOAN_REQUEST_DTO.getAmount()));
    }

    @Test
    @Transactional
    void createLoanRequest_WithExistingLoan_DoesNotUpdateLoanSumCache() {
        final LoanSumCache loanSumCache = mock(LoanSumCache.class);

        final Customer customer = persistCustomer(LOAN_REQUEST_DTO);
        persistLoanRequest(new LoanRequest(LOAN_REQUEST_DTO.getId(), LOAN_REQUEST_DTO.getAmount(), customer));

        final LoanService sut = createSut(loanSumCache);

        final LoanRequest loanRequest = sut.createLoanRequest(LOAN_REQUEST_DTO);

        verify(loanSumCache, never()).insertOrAdd(anyLong(), any());
    }

    @Test
    void createLoanRequest_WithConstraintViolation_LeadsToException() {
        final LoanService sut = createSut();

        final LoanRequestDto loanRequestDto = new LoanRequestDto(-1L, LOAN_REQUEST_DTO.getAmount(), LOAN_REQUEST_DTO.getCustomerId(), LOAN_REQUEST_DTO.getCustomerFullName());

        assertThatThrownBy(() -> sut.createLoanRequest(loanRequestDto))
                .isExactlyInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("must be at least 0.");
    }

    @Nonnull
    private LoanService createSut() {
        return createSut(mock(LoanSumCache.class));
    }

    @Nonnull
    private LoanService createSut(@Nonnull final LoanSumCache loanSumCache) {
        return new LoanService(validator, customerDao, loanRequestDao, loanSumCache);
    }

    private void assertDataOfDto(@Nonnull final LoanRequest loanRequest, @Nonnull final LoanRequestDto dto) {
        assertThat(loanRequest.getId()).isEqualTo(dto.getId());
        assertThat(loanRequest.getAmount().compareTo(dto.getAmount())).isZero();
        assertThat(loanRequest.getCustomer().getId()).isEqualTo(dto.getCustomerId());
        assertThat(loanRequest.getCustomer().getFullName()).isEqualTo(dto.getCustomerFullName());
    }

    private void assertIsPersisted(@Nonnull final LoanRequestDto dto) {
        final Optional<LoanRequest> maybeLoanRequest = loanRequestDao.findById(dto.getId());
        assertThat(maybeLoanRequest).isPresent();

        assertThat(maybeLoanRequest.get().getAmount().compareTo(dto.getAmount())).isZero();
        assertThat(maybeLoanRequest.get().getCustomer()).isNotNull();
        assertThat(maybeLoanRequest.get().getCustomer().getId()).isEqualTo(dto.getCustomerId());
        assertThat(maybeLoanRequest.get().getCustomer().getFullName()).isEqualTo(dto.getCustomerFullName());

        final Optional<Customer> maybeCustomer = customerDao.findById(dto.getCustomerId());
        assertThat(maybeCustomer).isPresent();

        assertThat(maybeCustomer.get().getId()).isEqualTo(dto.getCustomerId());
        assertThat(maybeCustomer.get().getFullName()).isEqualTo(dto.getCustomerFullName());
    }

    private Customer persistCustomer(@Nonnull final LoanRequestDto dto) {
        final Customer customer = new Customer(dto.getCustomerId(), dto.getCustomerFullName());
        return persistCustomer(customer);
    }

    private Customer persistCustomer(@Nonnull final Customer customer) {
        assertThat(customerDao.existsById(customer.getId())).isFalse();

        final Customer result = customerDao.saveAndFlush(customer);

        final Optional<Customer> persistedCustomer = customerDao.findById(customer.getId());
        assertThat(persistedCustomer).isPresent();
        assertThat(persistedCustomer.get().getId()).isEqualTo(customer.getId());
        assertThat(persistedCustomer.get().getFullName()).isEqualTo(customer.getFullName());

        return result;
    }

    private LoanRequest persistLoanRequest(@Nonnull final LoanRequest loanRequest) {
        assertThat(loanRequestDao.existsById(loanRequest.getId())).isFalse();

        final LoanRequest result = loanRequestDao.saveAndFlush(loanRequest);

        final Optional<LoanRequest> persistedLoanRequest = loanRequestDao.findById(loanRequest.getId());
        assertThat(persistedLoanRequest).isPresent();
        assertThat(persistedLoanRequest.get().getId()).isEqualTo(loanRequest.getId());
        assertThat(persistedLoanRequest.get().getAmount().compareTo(loanRequest.getAmount())).isZero();
        assertThat(persistedLoanRequest.get().getCustomer()).isEqualTo(loanRequest.getCustomer());

        return result;
    }
}
