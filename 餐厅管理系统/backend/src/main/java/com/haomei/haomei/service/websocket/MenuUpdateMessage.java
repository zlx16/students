package com.haomei.haomei.service.websocket;

public record MenuUpdateMessage(
        String action,
        Long dishId,
        String name,
        String imageUrl
) {
}

