package com.example.app.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        String sku,
        BigDecimal price,
        int stockQuantity,
        Instant createdAt,
        Instant updatedAt
) {
}
