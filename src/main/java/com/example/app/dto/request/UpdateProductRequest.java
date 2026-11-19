package com.example.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Full replacement (PUT semantics): every field is required except the description. */
public record UpdateProductRequest(
        @Schema(example = "Mechanical Keyboard")
        @NotBlank @Size(max = 150)
        String name,

        @Schema(example = "Hot-swappable 75% keyboard")
        @Size(max = 2000)
        String description,

        @Schema(example = "KB-75-BLK")
        @NotBlank @Size(max = 64)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "must contain only letters, digits, '.', '_' or '-'")
        String sku,

        @Schema(example = "119.99")
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2)
        BigDecimal price,

        @Schema(example = "40")
        @NotNull @PositiveOrZero
        Integer stockQuantity
) {
}
