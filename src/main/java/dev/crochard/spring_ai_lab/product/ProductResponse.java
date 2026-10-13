package dev.crochard.spring_ai_lab.product;

import java.math.BigDecimal;

/**
 * Représentation d'un produit renvoyée par l'API.
 * @param id identifiant du produit
 * @param reference référence unique
 * @param name libellé
 * @param price prix unitaire courant
 * @param active {@code true} si le produit peut être commandé
 */
public record ProductResponse(Long id, String reference, String name, BigDecimal price, boolean active) {

	/**
	 * @param product entité à convertir
	 * @return la représentation API du produit
	 */
	static ProductResponse from(Product product) {
		return new ProductResponse(product.getId(), product.getReference(), product.getName(), product.getPrice(),
				product.isActive());
	}

}
