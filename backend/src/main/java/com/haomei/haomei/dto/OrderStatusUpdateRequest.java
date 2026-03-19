package com.haomei.haomei.dto;

import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
        @NotNull String status
) {
}

