package com.haomei.haomei.dto;

import jakarta.validation.constraints.NotBlank;

public record OrderPayStartRequest(
        @NotBlank String paymentMethod
) {
}

