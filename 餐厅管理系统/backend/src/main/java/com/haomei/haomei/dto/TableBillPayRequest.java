package com.haomei.haomei.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TableBillPayRequest(
        @NotBlank String tableToken,
        @NotBlank String paymentMethod,
        @NotNull Boolean success
) {
}

