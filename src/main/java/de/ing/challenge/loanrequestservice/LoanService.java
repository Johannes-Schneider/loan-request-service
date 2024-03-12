package de.ing.challenge.loanrequestservice;

import de.ing.challenge.loanrequestservice.dao.Customer;
import de.ing.challenge.loanrequestservice.dao.CustomerDao;
import de.ing.challenge.loanrequestservice.dao.LoanRequest;
import de.ing.challenge.loanrequestservice.dao.LoanRequestDao;
import de.ing.challenge.loanrequestservice.dto.LoanRequestDto;
import jakarta.annotation.Nonnull;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LoanService {
    private static final ConstraintViolationException CUSTOMER_ALREADY_EXISTS = new ConstraintViolationException("The customer id is already in use.", null);
    private static final ConstraintViolationException LOAN_REQUEST_ALREADY_EXISTS = new ConstraintViolationException("The loan request id is already in use.", null);

    @Nonnull
    private final Validator validator;
    @Nonnull
    private final CustomerDao customerDao;
    @Nonnull
    private final LoanRequestDao loanRequestDao;
    @Nonnull
    private final LoanSumCache loanSumCache;

    public LoanService(@Autowired @Nonnull final Validator validator,
                       @Autowired @Nonnull final CustomerDao customerDao,
                       @Autowired @Nonnull final LoanRequestDao loanRequestDao,
                       @Autowired @Nonnull final LoanSumCache loanSumCache) {
        this.validator = validator;
        this.customerDao = customerDao;
        this.loanRequestDao = loanRequestDao;
        this.loanSumCache = loanSumCache;
    }

    @Transactional
    public LoanRequest createLoanRequest(@Nonnull final LoanRequestDto dto) throws ConstraintViolationException {
        throwIfConstrainsAreViolated(dto);

        final Customer customer = getOrPersistCustomer(dto);
        return persistLoanRequest(dto, customer);
    }

    private void throwIfConstrainsAreViolated(@Nonnull final LoanRequestDto dto) {
        final Set<ConstraintViolation<LoanRequestDto>> violations = validator.validate(dto);
        if (violations.isEmpty()) {
            return;
        }

        final String exceptionMessage = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining());

        log.info("The incoming {} with id {} caused following constrain violations: {}",
                LoanRequestDto.class, dto.getId(), exceptionMessage);
        throw new ConstraintViolationException("Following error(s) occurred: " + exceptionMessage, violations);
    }

    private Customer getOrPersistCustomer(@Nonnull final LoanRequestDto dto) throws ConstraintViolationException {
        final Optional<Customer> existingCustomer = customerDao.findById(dto.getCustomerId());
        if (existingCustomer.isPresent()) {
            throwIfExistingCustomerDoesNotMatch(dto, existingCustomer.get());
            return existingCustomer.get();
        }

        final Customer newCustomer = new Customer(dto.getCustomerId(), dto.getCustomerFullName());

        log.debug("Creating new {} with id {}.", Customer.class, newCustomer.getId());
        return customerDao.save(newCustomer);
    }

    private void throwIfExistingCustomerDoesNotMatch(@Nonnull final LoanRequestDto dto, @Nonnull final Customer customer) throws ConstraintViolationException {
        log.debug("Found existing {} with id {} while processing incoming {} with id {}. " +
                        "Checking whether the data matches up.",
                Customer.class, dto.getCustomerId(), LoanRequestDto.class, dto.getId());

        if (!customer.getFullName().equals(dto.getCustomerFullName())) {
            log.info("The existing {} with id {} does not have the same full name as the new {}.",
                    Customer.class, dto.getCustomerId(), LoanRequestDto.class);

            throw CUSTOMER_ALREADY_EXISTS;
        }

        log.debug("The existing {} with id {} matches the one from the incoming {} with id {}.",
                Customer.class, dto.getCustomerId(), LoanRequestDto.class, dto.getId());
    }

    private LoanRequest persistLoanRequest(@Nonnull final LoanRequestDto dto, @Nonnull final Customer customer) {
        final Optional<LoanRequest> maybeExistingRequest = loanRequestDao.findById(dto.getId());
        if (maybeExistingRequest.isPresent()) {
            throwIfExistingLoanRequestDoesNotMatch(dto, maybeExistingRequest.get());
            return maybeExistingRequest.get();
        }

        final LoanRequest newRequest = new LoanRequest(dto.getId(), dto.getAmount(), customer);
        loanSumCache.insertOrAdd(customer.getId(), newRequest.getAmount());

        log.debug("Creating new {} with id {} for {} with id {}.",
                LoanRequest.class, newRequest.getId(), Customer.class, customer.getId());
        return loanRequestDao.save(newRequest);
    }

    private void throwIfExistingLoanRequestDoesNotMatch(@Nonnull final LoanRequestDto dto, @Nonnull final LoanRequest loanRequest) throws ConstraintViolationException {
        log.debug("Found existing {} while processing incoming {} with id {}. " +
                        "Checking whether we processed the incoming request earlier already.",
                LoanRequest.class, LoanRequestDto.class, dto.getId());

        if (dto.getAmount().compareTo(loanRequest.getAmount()) != 0) {
            log.info("Existing {} with id {} does not have the same amount as the new {}.",
                    LoanRequest.class, dto.getId(), LoanRequestDto.class);
            throw LOAN_REQUEST_ALREADY_EXISTS;
        }

        if (dto.getCustomerId() != loanRequest.getCustomer().getId()) {
            log.info("Existing {} with id {} does not belong to the same customer as the new {}.",
                    LoanRequest.class, dto.getId(), LoanRequestDto.class);
            throw LOAN_REQUEST_ALREADY_EXISTS;
        }

        log.debug("The incoming {} with id {} has already been processed earlier.", LoanRequestDto.class, dto.getId());
    }

    @Nonnull
    public BigDecimal getLoanSumByCustomerId(final long customerId) throws NoSuchElementException {
        final Optional<BigDecimal> maybeLoanSum = loanSumCache.get(customerId);
        if (maybeLoanSum.isEmpty()) {
            throw new NoSuchElementException("Customer with id %s not found.".formatted(customerId));
        }

        return maybeLoanSum.get();
    }
}
