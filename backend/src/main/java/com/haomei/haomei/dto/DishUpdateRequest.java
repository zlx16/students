package com.haomei.haomei.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record DishUpdateRequest(
        @NotNull String name,
        String description,
        @NotNull @Positive BigDecimal price,
        @NotNull Boolean available,
        @NotNull String category,
        String portionOption
) {
}

