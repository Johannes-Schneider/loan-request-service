package de.ing.challenge.loanrequestservice.dto;

import lombok.Value;

import java.math.BigDecimal;

@Value
public class LoanSumResponseDto {
    long customerId;
    BigDecimal sum;
}
