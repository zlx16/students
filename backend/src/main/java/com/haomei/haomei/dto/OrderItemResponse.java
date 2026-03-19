package com.haomei.haomei.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long dishId,
        String dishName,
        String portion,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
}

