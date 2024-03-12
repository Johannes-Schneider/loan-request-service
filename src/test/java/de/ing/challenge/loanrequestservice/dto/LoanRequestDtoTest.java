package de.ing.challenge.loanrequestservice.dto;

import jakarta.annotation.Nonnull;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LoanRequestDtoTest {
    private static final long VALID_ID = 42;
    private static final BigDecimal VALID_AMOUNT = BigDecimal.valueOf(1_000.0d);
    private static final String VALID_FULL_NAME = "John Doe";

    @Autowired
    private Validator validator;

    @Test
    void loan_WithoutViolations() {
        assertNoViolation(new LoanRequestDto(VALID_ID, VALID_AMOUNT, VALID_ID, VALID_FULL_NAME));
    }

    @Test
    void loan_WithMissingId_LeadsToViolation() {
        assertViolation(new LoanRequestDto(null, VALID_AMOUNT, VALID_ID, VALID_FULL_NAME));
    }

    @Test
    void loan_WithTooLowId_LeadsToViolation() {
        assertViolation(new LoanRequestDto(-1L, VALID_AMOUNT, VALID_ID, VALID_FULL_NAME));
    }

    @Test
    void loan_WithMissingAmount_LeadsToViolation() {
        assertViolation(new LoanRequestDto(VALID_ID, null, VALID_ID, VALID_FULL_NAME));
    }

    @Test
    void loan_WithTooLowAmount_LeadsToViolation() {
        assertViolation(new LoanRequestDto(VALID_ID, BigDecimal.valueOf(499.99d), VALID_ID, VALID_FULL_NAME));
    }

    @Test
    void loan_WithTooHighAmount_LeadsToViolation() {
        assertViolation(new LoanRequestDto(VALID_ID, BigDecimal.valueOf(12_000.51d), VALID_ID, VALID_FULL_NAME));
    }

    @Test
    void loan_WithTooManyFractionDigitsInAmount_LeadsToViolation() {
        assertViolation(new LoanRequestDto(VALID_ID, BigDecimal.valueOf(1_000.999d), VALID_ID, VALID_FULL_NAME));
    }

    @Test
    void loan_WithMissingCustomerId_LeadsToViolation() {
        assertViolation(new LoanRequestDto(VALID_ID, VALID_AMOUNT, null, VALID_FULL_NAME));
    }

    @Test
    void loan_WithTooLowCustomerId_LeadsToViolation() {
        assertViolation(new LoanRequestDto(VALID_ID, VALID_AMOUNT, -1L, VALID_FULL_NAME));
    }

    @Test
    void loan_WithMissingCustomerFullName_LeadsToViolation() {
        assertViolation(new LoanRequestDto(VALID_ID, VALID_AMOUNT, VALID_ID, null));
    }

    @Test
    void loan_WithBlankCustomerFullName_LeadsToViolation() {
        assertViolation(new LoanRequestDto(VALID_ID, VALID_AMOUNT, VALID_ID, " \n \t"));
    }

    private void assertViolation(@Nonnull final LoanRequestDto sut) {
        final Set<ConstraintViolation<LoanRequestDto>> violations = validator.validate(sut);

        assertThat(violations).isNotEmpty();
    }

    private void assertNoViolation(@Nonnull final LoanRequestDto sut) {
        final Set<ConstraintViolation<LoanRequestDto>> violations = validator.validate(sut);

        assertThat(violations).isEmpty();
    }
}
