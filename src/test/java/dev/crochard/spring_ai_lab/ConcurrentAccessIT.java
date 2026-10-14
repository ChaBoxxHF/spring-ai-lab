package dev.crochard.spring_ai_lab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import dev.crochard.spring_ai_lab.client.ClientRepository;
import dev.crochard.spring_ai_lab.order.Order;
import dev.crochard.spring_ai_lab.order.OrderRepository;
import dev.crochard.spring_ai_lab.product.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Accès concurrents simulés de façon déterministe sur une vraie base PostgreSQL (Testcontainers) : les espions
 * sur les repositories reproduisent l'entrelacement de deux requêtes simultanées.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ConcurrentAccessIT {

	private static final String INTEGRITY_VIOLATION_DETAIL = "Violation d'une contrainte d'intégrité des données";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PlatformTransactionManager transactionManager;

	/** EntityManager partagé, lié à la transaction en cours (celle du service pendant la requête). */
	@PersistenceContext
	private EntityManager entityManager;

	@MockitoSpyBean
	private ClientRepository clientRepository;

	@MockitoSpyBean
	private ProductRepository productRepository;

	@MockitoSpyBean
	private OrderRepository orderRepository;

	@Test
	void duplicateEmailMissedByPreCheckIsRejectedByDatabaseWith409() throws Exception {
		String email = UUID.randomUUID() + "@example.com";
		String body = """
				{"firstName": "Ada", "lastName": "Lovelace", "email": "%s"}
				""".formatted(email);
		postJson("/api/clients", body).andExpect(status().isCreated());

		// Une requête concurrente a vérifié l'unicité avant que la première ne soit enregistrée.
		doReturn(false).when(clientRepository).existsByEmail(email);

		postJson("/api/clients", body).andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value(INTEGRITY_VIOLATION_DETAIL));
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM clients WHERE email = ?", Integer.class, email))
			.isEqualTo(1);
	}

	@Test
	void duplicateReferenceMissedByPreCheckIsRejectedByDatabaseWith409() throws Exception {
		String reference = "REF-" + UUID.randomUUID();
		String body = """
				{"reference": "%s", "name": "Produit", "price": 10.00, "active": true}
				""".formatted(reference);
		postJson("/api/products", body).andExpect(status().isCreated());

		// Une requête concurrente a vérifié l'unicité avant que la première ne soit enregistrée.
		doReturn(false).when(productRepository).existsByReference(reference);

		postJson("/api/products", body).andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value(INTEGRITY_VIOLATION_DETAIL));
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products WHERE reference = ?", Integer.class,
				reference))
			.isEqualTo(1);
	}

	@Test
	void concurrentStatusChangeThroughApiIsRejectedWith409() throws Exception {
		long orderId = createOrder();
		TransactionTemplate concurrentTransaction = new TransactionTemplate(transactionManager);
		concurrentTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		// Juste après la lecture de la commande (DRAFT) par la requête, une autre requête l'annule et valide.
		// Le repository étant un proxy JDK, l'espion ne peut pas appeler la méthode réelle : la commande est lue
		// via l'EntityManager de la transaction du service, ce qui revient au même.
		doAnswer(invocation -> {
			Optional<Order> order = Optional.ofNullable(entityManager.find(Order.class, orderId));
			concurrentTransaction.executeWithoutResult(transaction -> jdbcTemplate
				.update("UPDATE orders SET status = 'CANCELLED', version = version + 1 WHERE id = ?", orderId));
			return order;
		}).when(orderRepository).findById(orderId);

		mockMvc.perform(patch("/api/orders/{id}/status", orderId).contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\": \"CONFIRMED\"}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("La ressource a été modifiée simultanément, veuillez réessayer"));

		// La modification concurrente validée l'emporte : la confirmation n'a pas été appliquée.
		Map<String, Object> order = jdbcTemplate.queryForMap("SELECT status, version FROM orders WHERE id = ?",
				orderId);
		assertThat(order.get("status")).isEqualTo("CANCELLED");
		assertThat(((Number) order.get("version")).longValue()).isEqualTo(1L);
	}

	private long createOrder() throws Exception {
		long clientId = idOf(postJson("/api/clients", """
				{"firstName": "Ada", "lastName": "Lovelace", "email": "%s@example.com"}
				""".formatted(UUID.randomUUID())).andExpect(status().isCreated()));
		long productId = idOf(postJson("/api/products", """
				{"reference": "REF-%s", "name": "Produit", "price": 10.00, "active": true}
				""".formatted(UUID.randomUUID())).andExpect(status().isCreated()));
		return idOf(postJson("/api/orders", """
				{"clientId": %d, "lines": [{"productId": %d, "quantity": 1}]}
				""".formatted(clientId, productId)).andExpect(status().isCreated()));
	}

	private ResultActions postJson(String uri, String body) throws Exception {
		return mockMvc.perform(post(uri).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private static long idOf(ResultActions result) throws Exception {
		Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
		return id.longValue();
	}

}
