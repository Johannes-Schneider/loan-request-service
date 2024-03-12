package de.ing.challenge.loanrequestservice;

import de.ing.challenge.loanrequestservice.dto.BadRequestResponseDto;
import de.ing.challenge.loanrequestservice.dto.LoanRequestDto;
import de.ing.challenge.loanrequestservice.dto.LoanSumResponseDto;
import jakarta.annotation.Nonnull;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

@RestController
@RequestMapping(value = "/api/v1/loan-requests", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
public class LoanController {
    @Nonnull
    private final LoanService loanService;

    public LoanController(@Autowired @Nonnull final LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    ResponseEntity<?> createLoanRequest(@RequestBody @Nonnull final LoanRequestDto loanRequest) {
        try {
            loanService.createLoanRequest(loanRequest);
            return ResponseEntity.ok().build();
        } catch (final ConstraintViolationException e) {
            return ResponseEntity.badRequest().body(new BadRequestResponseDto(e.getMessage()));
        } catch (final Exception e) {
            log.error("Caught {} while creating loan request.", e.getClass(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/sum/{customerId}")
    ResponseEntity<?> getLoanSumByCustomerId(@PathVariable("customerId") @Min(0) final long customerId) {
        try {
            final BigDecimal loanSum = loanService.getLoanSumByCustomerId(customerId);
            return ResponseEntity.ok().body(new LoanSumResponseDto(customerId, loanSum));
        } catch (final NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (final Exception e) {
            log.error("Caught {} while getting loan sum.", e.getClass(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
