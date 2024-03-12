package de.ing.challenge.loanrequestservice.dto;

import jakarta.annotation.Nonnull;
import lombok.Value;

@Value
public class BadRequestResponseDto {
    @Nonnull
    String message;
}
