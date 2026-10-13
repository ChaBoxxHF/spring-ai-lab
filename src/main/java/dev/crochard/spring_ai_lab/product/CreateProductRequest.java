package dev.crochard.spring_ai_lab.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(
		@NotBlank @Size(max = 255) String reference,
		@NotBlank @Size(max = 255) String name,
		@NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal price,
		@NotNull Boolean active) {
}
