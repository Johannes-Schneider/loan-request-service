package de.ing.challenge.loanrequestservice.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoanRequestDto {
    @NotNull(message = "The loan request id must not be null.")
    @Min(value = 0, message = "The loan request id must be at least 0.")
    private Long id;

    @DecimalMin("500.00")
    @DecimalMax("12000.50")
    @Digits(integer = 5, fraction = 2)
    @NotNull(message = "The loan request amount must not be null.")
    private BigDecimal amount;

    @NotNull(message = "The customer id must not be null.")
    @Min(value = 0, message = "The customer id must be at least 0.")
    private Long customerId;

    @NotBlank(message = "The customer full name must not be blank.")
    private String customerFullName;
}

