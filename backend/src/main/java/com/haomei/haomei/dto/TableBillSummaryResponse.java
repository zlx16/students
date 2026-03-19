package com.haomei.haomei.dto;

import java.math.BigDecimal;
import java.util.List;

public record TableBillSummaryResponse(
        Integer tableNo,
        BigDecimal totalAmount,
        Integer orderCount,
        List<OrderResponse> orders
) {
}

