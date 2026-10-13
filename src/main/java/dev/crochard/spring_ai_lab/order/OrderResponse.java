package dev.crochard.spring_ai_lab.order;

import java.math.BigDecimal;
import java.util.List;

/**
 * Représentation d'une commande renvoyée par l'API.
 * @param id identifiant de la commande
 * @param clientId identifiant du client
 * @param status statut actuel
 * @param lines lignes de la commande
 * @param total somme des totaux de lignes
 */
public record OrderResponse(Long id, Long clientId, OrderStatus status, List<OrderLineResponse> lines,
		BigDecimal total) {

	/**
	 * Représentation d'une ligne de commande renvoyée par l'API.
	 * @param id identifiant de la ligne
	 * @param productId identifiant du produit
	 * @param productReference référence du produit
	 * @param productName libellé du produit
	 * @param quantity quantité commandée
	 * @param unitPrice prix unitaire figé au moment de la commande
	 * @param lineTotal quantité × prix unitaire
	 */
	public record OrderLineResponse(Long id, Long productId, String productReference, String productName,
			int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {

		/**
		 * @param line entité à convertir
		 * @return la représentation API de la ligne
		 */
		static OrderLineResponse from(OrderLine line) {
			return new OrderLineResponse(line.getId(), line.getProduct().getId(), line.getProduct().getReference(),
					line.getProduct().getName(), line.getQuantity(), line.getUnitPrice(), line.lineTotal());
		}

	}

	/**
	 * @param order entité à convertir ; ses lignes et leurs produits doivent être chargés
	 * @return la représentation API de la commande
	 */
	static OrderResponse from(Order order) {
		return new OrderResponse(order.getId(), order.getClient().getId(), order.getStatus(),
				order.getLines().stream().map(OrderLineResponse::from).toList(), order.total());
	}

}
