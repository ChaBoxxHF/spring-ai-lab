package dev.crochard.spring_ai_lab.order;

import java.util.Set;

/**
 * Cycle de vie d'une commande.
 */
public enum OrderStatus {

	/** Commande créée, non encore confirmée. Statut initial. */
	DRAFT,
	/** Commande confirmée. */
	CONFIRMED,
	/** Commande annulée. Statut final. */
	CANCELLED;

	/**
	 * Transitions autorisées : DRAFT -> CONFIRMED, DRAFT -> CANCELLED, CONFIRMED -> CANCELLED.
	 * Aucune autre transition n'est autorisée.
	 * @param target statut cible
	 * @return {@code true} si la transition depuis ce statut vers {@code target} est autorisée
	 */
	public boolean canTransitionTo(OrderStatus target) {
		return switch (this) {
			case DRAFT -> Set.of(CONFIRMED, CANCELLED).contains(target);
			case CONFIRMED -> target == CANCELLED;
			case CANCELLED -> false;
		};
	}

}
