package com.haomei.haomei.dto;

public record TableSessionResponse(
        Integer tableNo,
        String token,
        String url,
        String qrDataUrl,
        Boolean inUse,
        Long activeOrderCount,
        String statusLabel
) {
}

