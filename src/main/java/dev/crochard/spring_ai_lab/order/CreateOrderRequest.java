package dev.crochard.spring_ai_lab.order;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Corps de la requête de création d'une commande.
 * @param clientId identifiant du client passant la commande, obligatoire
 * @param lines lignes de la commande, au moins une
 */
public record CreateOrderRequest(
		@NotNull Long clientId,
		@NotEmpty List<@Valid @NotNull OrderLineRequest> lines) {

	/**
	 * Ligne demandée dans une commande.
	 * @param productId identifiant du produit, obligatoire
	 * @param quantity quantité, obligatoire et strictement positive
	 */
	public record OrderLineRequest(@NotNull Long productId, @NotNull @Positive Integer quantity) {
	}

}
