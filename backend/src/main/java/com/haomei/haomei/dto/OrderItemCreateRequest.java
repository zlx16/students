package com.haomei.haomei.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderItemCreateRequest(
        @NotNull Long dishId,
        @NotBlank String portion,
        @NotNull @Min(1) Integer quantity
) {
}

