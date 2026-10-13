package dev.crochard.spring_ai_lab.order;

import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(Long id, Long clientId, OrderStatus status, List<OrderLineResponse> lines,
		BigDecimal total) {

	public record OrderLineResponse(Long id, Long productId, String productReference, String productName,
			int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {

		static OrderLineResponse from(OrderLine line) {
			return new OrderLineResponse(line.getId(), line.getProduct().getId(), line.getProduct().getReference(),
					line.getProduct().getName(), line.getQuantity(), line.getUnitPrice(), line.lineTotal());
		}

	}

	static OrderResponse from(Order order) {
		return new OrderResponse(order.getId(), order.getClient().getId(), order.getStatus(),
				order.getLines().stream().map(OrderLineResponse::from).toList(), order.total());
	}

}
