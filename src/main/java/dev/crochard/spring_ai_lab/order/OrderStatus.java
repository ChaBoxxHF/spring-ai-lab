package dev.crochard.spring_ai_lab.order;

import java.util.Set;

public enum OrderStatus {

	DRAFT, CONFIRMED, CANCELLED;

	/**
	 * Transitions autorisées : DRAFT -> CONFIRMED, DRAFT -> CANCELLED, CONFIRMED -> CANCELLED.
	 * Aucune autre transition n'est autorisée.
	 */
	public boolean canTransitionTo(OrderStatus target) {
		return switch (this) {
			case DRAFT -> Set.of(CONFIRMED, CANCELLED).contains(target);
			case CONFIRMED -> target == CANCELLED;
			case CANCELLED -> false;
		};
	}

}
