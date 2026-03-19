package com.haomei.haomei.service.websocket;

import com.haomei.haomei.dto.OrderItemResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderUpdateMessage(
        Long orderId,
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

