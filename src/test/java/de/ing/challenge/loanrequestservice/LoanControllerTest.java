package de.ing.challenge.loanrequestservice;

import de.ing.challenge.loanrequestservice.dao.LoanRequest;
import de.ing.challenge.loanrequestservice.dto.BadRequestResponseDto;
import de.ing.challenge.loanrequestservice.dto.LoanRequestDto;
import de.ing.challenge.loanrequestservice.dto.LoanSumResponseDto;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoanControllerTest {
    private static final LoanRequestDto LOAN_REQUEST_DTO = new LoanRequestDto(1337L, BigDecimal.valueOf(1_000.0d), 42L, "Customer");

    @Test
    void createLoanRequest_Success() {
        final LoanService loanService = mock(LoanService.class);
        when(loanService.createLoanRequest(any())).thenReturn(mock(LoanRequest.class));

        final LoanController sut = new LoanController(loanService);

        final ResponseEntity<?> response = sut.createLoanRequest(LOAN_REQUEST_DTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void createLoanRequest_HandlesConstraintViolationException() {
        final LoanService loanService = mock(LoanService.class);
        when(loanService.createLoanRequest(any())).thenThrow(new ConstraintViolationException("message", null));

        final LoanController sut = new LoanController(loanService);

        final ResponseEntity<?> response = sut.createLoanRequest(LOAN_REQUEST_DTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isExactlyInstanceOf(BadRequestResponseDto.class).satisfies(body -> {
            final BadRequestResponseDto dto = (BadRequestResponseDto) body;

            assertThat(dto.getMessage()).isEqualTo("message");
        });
    }

    @Test
    void createLoanRequest_HandlesArbitraryException() {
        final LoanService loanService = mock(LoanService.class);
        when(loanService.createLoanRequest(any())).thenThrow(new IllegalStateException("some exception"));

        final LoanController sut = new LoanController(loanService);

        final ResponseEntity<?> response = sut.createLoanRequest(LOAN_REQUEST_DTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        // make sure we are not exposing (potentially) sensitive error information to the caller
        assertThat(response.getBody()).isNull();
    }

    @Test
    void getLoanSumByCustomerId_Success() {
        final long customerId = 42L;

        final LoanService loanService = mock(LoanService.class);
        when(loanService.getLoanSumByCustomerId(anyLong())).thenReturn(BigDecimal.TEN);

        final LoanController sut = new LoanController(loanService);

        final ResponseEntity<?> response = sut.getLoanSumByCustomerId(customerId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isExactlyInstanceOf(LoanSumResponseDto.class).satisfies(body -> {
            final LoanSumResponseDto dto = (LoanSumResponseDto) body;

            assertThat(dto.getCustomerId()).isEqualTo(customerId);
            assertThat(dto.getSum()).isEqualTo(BigDecimal.TEN);
        });
    }

    @Test
    void getLoanSumByCustomerId_HandlesNoSuchElementException() {
        final LoanService loanService = mock(LoanService.class);
        when(loanService.getLoanSumByCustomerId(anyLong())).thenThrow(new NoSuchElementException("customer id not found"));

        final LoanController sut = new LoanController(loanService);

        final ResponseEntity<?> response = sut.getLoanSumByCustomerId(LOAN_REQUEST_DTO.getCustomerId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void getLoanSumByCustomerId_HandlesArbitraryException() {
        final LoanService loanService = mock(LoanService.class);
        when(loanService.getLoanSumByCustomerId(anyLong())).thenThrow(new IllegalStateException("some message"));

        final LoanController sut = new LoanController(loanService);

        final ResponseEntity<?> response = sut.getLoanSumByCustomerId(LOAN_REQUEST_DTO.getCustomerId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNull();
    }
}
