package dev.crochard.spring_ai_lab.order;

public enum OrderStatus {

	DRAFT,
	CONFIRMED,
	CANCELLED;

	public boolean canTransitionTo(OrderStatus newStatus) {
		return switch (this) {
			case DRAFT -> newStatus == CONFIRMED || newStatus == CANCELLED;
			case CONFIRMED -> newStatus == CANCELLED;
			case CANCELLED -> false;
		};
	}
}
