package dev.crochard.spring_ai_lab.product;

import java.math.BigDecimal;

public record ProductResponse(Long id, String reference, String name, BigDecimal price, boolean active) {

	static ProductResponse from(Product product) {
		return new ProductResponse(product.getId(), product.getReference(), product.getName(), product.getPrice(),
				product.isActive());
	}

}
