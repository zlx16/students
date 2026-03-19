package com.haomei.haomei.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record OrderPayConfirmRequest(
        @NotBlank String paymentAttemptId,
        @NotNull Boolean success
) {
}

