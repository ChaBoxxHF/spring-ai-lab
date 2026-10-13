package dev.crochard.spring_ai_lab.order;

import static dev.crochard.spring_ai_lab.order.OrderStatus.CANCELLED;
import static dev.crochard.spring_ai_lab.order.OrderStatus.CONFIRMED;
import static dev.crochard.spring_ai_lab.order.OrderStatus.DRAFT;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OrderStatusTest {

	@ParameterizedTest(name = "{0} -> {1} autorisée : {2}")
	@CsvSource({
			"DRAFT, CONFIRMED, true",
			"DRAFT, CANCELLED, true",
			"CONFIRMED, CANCELLED, true",
			"DRAFT, DRAFT, false",
			"CONFIRMED, DRAFT, false",
			"CONFIRMED, CONFIRMED, false",
			"CANCELLED, DRAFT, false",
			"CANCELLED, CONFIRMED, false",
			"CANCELLED, CANCELLED, false" })
	void onlyDocumentedTransitionsAreAllowed(OrderStatus from, OrderStatus to, boolean allowed) {
		assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
	}

	@org.junit.jupiter.api.Test
	void everyCombinationIsCoveredByTheTable() {
		// Garde-fou : un nouveau statut obligerait à revoir la table de transitions ci-dessus.
		assertThat(OrderStatus.values()).containsExactly(DRAFT, CONFIRMED, CANCELLED);
	}

}
