package dev.crochard.spring_ai_lab.product;

import jakarta.validation.constraints.NotNull;

public record UpdateProductStatusRequest(@NotNull Boolean active) {
}
