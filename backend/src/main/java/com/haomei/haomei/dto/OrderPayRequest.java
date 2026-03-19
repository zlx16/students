package com.haomei.haomei.dto;

import jakarta.validation.constraints.NotBlank;

public record OrderPayRequest(
        @NotBlank String paymentMethod
) {
}

