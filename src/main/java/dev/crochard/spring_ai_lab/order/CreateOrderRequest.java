package dev.crochard.spring_ai_lab.order;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderRequest(
		@NotNull Long clientId,
		@NotEmpty List<@Valid @NotNull OrderLineRequest> lines) {

	public record OrderLineRequest(@NotNull Long productId, @NotNull @Positive Integer quantity) {
	}

}
