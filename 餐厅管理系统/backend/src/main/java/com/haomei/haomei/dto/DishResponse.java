package com.haomei.haomei.dto;

import java.math.BigDecimal;

public record DishResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Boolean available,
        String category,
        String portionOption,
        String imageUrl
) {
}

