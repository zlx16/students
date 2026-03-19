package com.haomei.haomei.dto;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(
        Long id,
        Integer tableNo,
        Integer diners,
        String customerName,
        String remark,
        String status,
        Instant createdAt,
        BigDecimal amount,
        String paymentMethod,
        Boolean paid,
        String paymentStatus,
        Instant paymentExpiresAt,
        String paymentAttemptId,
        Instant paymentStartedAt,
        List<OrderItemResponse> items
) {
}

