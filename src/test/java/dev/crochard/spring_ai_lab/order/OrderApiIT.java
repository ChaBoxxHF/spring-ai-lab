package dev.crochard.spring_ai_lab.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import dev.crochard.spring_ai_lab.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Parcours complet de l'API sur une vraie base PostgreSQL (Testcontainers).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrderApiIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Test
	void createsOrderWithTotalsAndMemorizedPrices() throws Exception {
		long clientId = createClient();
		long keyboardId = createProduct("49.99", true);
		long mouseId = createProduct("19.90", true);

		long orderId = idOf(postJson("/api/orders", """
				{"clientId": %d, "lines": [
					{"productId": %d, "quantity": 3},
					{"productId": %d, "quantity": 2}]}
				""".formatted(clientId, keyboardId, mouseId))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("DRAFT"))
			.andExpect(jsonPath("$.lines[0].lineTotal").value(149.97))
			.andExpect(jsonPath("$.lines[1].lineTotal").value(39.80))
			.andExpect(jsonPath("$.total").value(189.77)));

		// Le prix catalogue change après la commande : la commande conserve le prix d'origine.
		jdbcTemplate.update("UPDATE products SET price = 999.00 WHERE id = ?", keyboardId);

		mockMvc.perform(get("/api/orders/{id}", orderId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.lines[0].unitPrice").value(49.99))
			.andExpect(jsonPath("$.total").value(189.77));
	}

	@Test
	void rejectsOrderContainingAnInactiveProductAndPersistsNothing() throws Exception {
		long clientId = createClient();
		long activeId = createProduct("10.00", true);
		long inactiveId = createProduct("5.00", true);
		deactivate(inactiveId);
		int ordersBefore = count("orders");
		int linesBefore = count("order_lines");

		// La ligne active est traitée avant la ligne inactive : rien ne doit subsister.
		postJson("/api/orders", """
				{"clientId": %d, "lines": [
					{"productId": %d, "quantity": 1},
					{"productId": %d, "quantity": 1}]}
				""".formatted(clientId, activeId, inactiveId))
			.andExpect(status().isUnprocessableContent());

		assertThat(count("orders")).isEqualTo(ordersBefore);
		assertThat(count("order_lines")).isEqualTo(linesBefore);
	}

	@Test
	void deactivatingProductDoesNotAffectExistingOrders() throws Exception {
		long clientId = createClient();
		long productId = createProduct("10.00", true);
		long orderId = idOf(postJson("/api/orders", """
				{"clientId": %d, "lines": [{"productId": %d, "quantity": 2}]}
				""".formatted(clientId, productId)).andExpect(status().isCreated()));

		deactivate(productId);

		mockMvc.perform(get("/api/orders/{id}", orderId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.lines[0].unitPrice").value(10.00))
			.andExpect(jsonPath("$.total").value(20.00));
		changeStatus(orderId, "CONFIRMED").andExpect(status().isOk());
	}

	@Test
	void listsOrdersWithLinesOutsideTransaction() throws Exception {
		long orderId = createOrder();

		mockMvc.perform(get("/api/orders"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.id == %d)].lines[0].productReference".formatted(orderId)).exists());
	}

	@Test
	void concurrentStatusChangesAreDetectedByOptimisticLocking() throws Exception {
		long orderId = createOrder();
		EntityManager first = entityManagerFactory.createEntityManager();
		EntityManager second = entityManagerFactory.createEntityManager();
		try {
			first.getTransaction().begin();
			second.getTransaction().begin();
			Order seenByFirst = first.find(Order.class, orderId);
			Order seenBySecond = second.find(Order.class, orderId);

			// Les deux lisent DRAFT : chacune des transitions semble valide isolément.
			seenByFirst.setStatus(OrderStatus.CANCELLED);
			first.getTransaction().commit();
			seenBySecond.setStatus(OrderStatus.CONFIRMED);

			assertThatThrownBy(() -> second.getTransaction().commit()).isInstanceOf(RollbackException.class)
				.hasCauseInstanceOf(OptimisticLockException.class);
		}
		finally {
			first.close();
			second.close();
		}
		mockMvc.perform(get("/api/orders/{id}", orderId)).andExpect(jsonPath("$.status").value("CANCELLED"));
	}

	@Test
	void appliesOnlyAllowedStatusTransitions() throws Exception {
		long orderId = createOrder();

		changeStatus(orderId, "CONFIRMED").andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CONFIRMED"));
		changeStatus(orderId, "DRAFT").andExpect(status().isConflict());
		changeStatus(orderId, "CANCELLED").andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CANCELLED"));
		changeStatus(orderId, "CONFIRMED").andExpect(status().isConflict());

		mockMvc.perform(get("/api/orders/{id}", orderId)).andExpect(jsonPath("$.status").value("CANCELLED"));
	}

	@Test
	void draftOrderCanBeCancelledDirectly() throws Exception {
		long orderId = createOrder();

		changeStatus(orderId, "CANCELLED").andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CANCELLED"));
	}

	@Test
	void rejectsDuplicateClientEmail() throws Exception {
		String email = uniqueEmail();
		String body = """
				{"firstName": "Ada", "lastName": "Lovelace", "email": "%s"}
				""".formatted(email);

		postJson("/api/clients", body).andExpect(status().isCreated());
		postJson("/api/clients", body).andExpect(status().isConflict());
	}

	@Test
	void storesEmailInLowerCaseAndRejectsDuplicateIgnoringCase() throws Exception {
		String email = uniqueEmail();

		postJson("/api/clients", """
				{"firstName": "Ada", "lastName": "Lovelace", "email": "%s"}
				""".formatted(email.toUpperCase()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.email").value(email));
		postJson("/api/clients", """
				{"firstName": "Ada", "lastName": "Lovelace", "email": "%s"}
				""".formatted(email))
			.andExpect(status().isConflict());

		assertThat(jdbcTemplate.queryForObject("SELECT email FROM clients WHERE email = ?", String.class, email))
			.isEqualTo(email);
	}

	@Test
	void rejectsDuplicateProductReference() throws Exception {
		String body = """
				{"reference": "REF-%s", "name": "Produit", "price": 10.00, "active": true}
				""".formatted(UUID.randomUUID());

		postJson("/api/products", body).andExpect(status().isCreated());
		postJson("/api/products", body).andExpect(status().isConflict());
	}

	@Test
	void mergesLinesForTheSameProduct() throws Exception {
		long clientId = createClient();
		long productId = createProduct("10.00", true);

		postJson("/api/orders", """
				{"clientId": %d, "lines": [
					{"productId": %d, "quantity": 2},
					{"productId": %d, "quantity": 3}]}
				""".formatted(clientId, productId, productId))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.lines.length()").value(1))
			.andExpect(jsonPath("$.lines[0].quantity").value(5))
			.andExpect(jsonPath("$.total").value(50.00));
	}

	@Test
	void returns404ForUnknownClientOnOrderCreation() throws Exception {
		long productId = createProduct("10.00", true);

		postJson("/api/orders", """
				{"clientId": 999999, "lines": [{"productId": %d, "quantity": 1}]}
				""".formatted(productId))
			.andExpect(status().isNotFound());
	}

	private long createOrder() throws Exception {
		long clientId = createClient();
		long productId = createProduct("10.00", true);
		return idOf(postJson("/api/orders", """
				{"clientId": %d, "lines": [{"productId": %d, "quantity": 1}]}
				""".formatted(clientId, productId)).andExpect(status().isCreated()));
	}

	private long createClient() throws Exception {
		return idOf(postJson("/api/clients", """
				{"firstName": "Ada", "lastName": "Lovelace", "email": "%s"}
				""".formatted(uniqueEmail())).andExpect(status().isCreated()));
	}

	private long createProduct(String price, boolean active) throws Exception {
		return idOf(postJson("/api/products", """
				{"reference": "REF-%s", "name": "Produit", "price": %s, "active": %s}
				""".formatted(UUID.randomUUID(), price, active)).andExpect(status().isCreated()));
	}

	private void deactivate(long productId) throws Exception {
		mockMvc.perform(patch("/api/products/{id}/status", productId).contentType(MediaType.APPLICATION_JSON)
			.content("{\"active\": false}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.active").value(false));
	}

	private int count(String table) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
	}

	private ResultActions changeStatus(long orderId, String target) throws Exception {
		return mockMvc.perform(patch("/api/orders/{id}/status", orderId).contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\": \"%s\"}".formatted(target)));
	}

	private ResultActions postJson(String uri, String body) throws Exception {
		return mockMvc.perform(post(uri).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private static long idOf(ResultActions result) throws Exception {
		Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
		return id.longValue();
	}

	private static String uniqueEmail() {
		return UUID.randomUUID() + "@example.com";
	}

}
