package dev.crochard.spring_ai_lab.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import dev.crochard.spring_ai_lab.client.Client;
import dev.crochard.spring_ai_lab.product.Product;
import org.junit.jupiter.api.Test;

class OrderTest {

	private final Client client = new Client("Ada", "Lovelace", "ada@example.com");

	private final Product keyboard = new Product("KB-01", "Clavier", new BigDecimal("49.99"), true);

	private final Product mouse = new Product("MS-01", "Souris", new BigDecimal("19.90"), true);

	@Test
	void newOrderIsDraftWithZeroTotal() {
		Order order = new Order(client);

		assertThat(order.getStatus()).isEqualTo(OrderStatus.DRAFT);
		assertThat(order.getLines()).isEmpty();
		assertThat(order.total()).isEqualByComparingTo("0");
	}

	@Test
	void lineTotalIsQuantityTimesUnitPrice() {
		Order order = new Order(client);
		order.addLine(keyboard, 3);

		OrderLine line = order.getLines().getFirst();
		assertThat(line.getUnitPrice()).isEqualByComparingTo("49.99");
		assertThat(line.lineTotal()).isEqualByComparingTo("149.97");
	}

	@Test
	void orderTotalIsSumOfLineTotals() {
		Order order = new Order(client);
		order.addLine(keyboard, 3);
		order.addLine(mouse, 2);

		assertThat(order.total()).isEqualByComparingTo("189.77");
	}

	@Test
	void totalIsExactWithoutFloatingPointErrors() {
		Product cheap = new Product("CH-01", "Gomme", new BigDecimal("0.10"), true);
		Order order = new Order(client);
		order.addLine(cheap, 3);

		// 3 × 0.10 vaut exactement 0.30 en BigDecimal (0.30000000000000004 en double).
		assertThat(order.total()).isEqualByComparingTo("0.30");
	}

}
