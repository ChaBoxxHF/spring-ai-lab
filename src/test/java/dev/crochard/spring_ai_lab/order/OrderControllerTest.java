package dev.crochard.spring_ai_lab.order;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import dev.crochard.spring_ai_lab.common.BusinessRuleViolationException;
import dev.crochard.spring_ai_lab.common.ConflictException;
import dev.crochard.spring_ai_lab.common.NotFoundException;
import dev.crochard.spring_ai_lab.order.OrderResponse.OrderLineResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OrderService orderService;

	@Test
	void createReturns201WithLocation() throws Exception {
		when(orderService.create(any())).thenReturn(new OrderResponse(7L, 1L, OrderStatus.DRAFT,
				List.of(new OrderLineResponse(1L, 10L, "KB-01", "Clavier", 2, new BigDecimal("10.00"),
						new BigDecimal("20.00"))),
				new BigDecimal("20.00")));

		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
				{"clientId": 1, "lines": [{"productId": 10, "quantity": 2}]}
				"""))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "/api/orders/7"))
			.andExpect(jsonPath("$.status").value("DRAFT"))
			.andExpect(jsonPath("$.total").value(20.00));
	}

	@Test
	void rejectsOrderWithoutLines() throws Exception {
		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
				{"clientId": 1, "lines": []}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.lines").exists());
		verifyNoInteractions(orderService);
	}

	@ParameterizedTest(name = "quantité {0}")
	@ValueSource(strings = { "0", "-1", "null" })
	void rejectsNonPositiveOrMissingQuantity(String quantity) throws Exception {
		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
				{"clientId": 1, "lines": [{"productId": 10, "quantity": %s}]}
				""".formatted(quantity)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors['lines[0].quantity']").exists());
		verifyNoInteractions(orderService);
	}

	@Test
	void rejectsUnknownStatusValue() throws Exception {
		mockMvc.perform(patch("/api/orders/7/status").contentType(MediaType.APPLICATION_JSON).content("""
				{"status": "SHIPPED"}
				"""))
			.andExpect(status().isBadRequest());
		verifyNoInteractions(orderService);
	}

	@Test
	void mapsConcurrentModificationTo409() throws Exception {
		when(orderService.changeStatus(eq(7L), any()))
			.thenThrow(new ObjectOptimisticLockingFailureException(Order.class, 7L));

		mockMvc.perform(patch("/api/orders/7/status").contentType(MediaType.APPLICATION_JSON).content("""
				{"status": "CONFIRMED"}
				"""))
			.andExpect(status().isConflict());
	}

	@Test
	void rejectsMissingClient() throws Exception {
		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
				{"lines": [{"productId": 10, "quantity": 1}]}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.clientId").exists());
	}

	@Test
	void mapsBusinessRuleViolationTo422() throws Exception {
		when(orderService.create(any())).thenThrow(new BusinessRuleViolationException("produit inactif"));

		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
				{"clientId": 1, "lines": [{"productId": 10, "quantity": 1}]}
				"""))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("produit inactif"));
	}

	@Test
	void mapsForbiddenTransitionTo409() throws Exception {
		when(orderService.changeStatus(eq(7L), eq(OrderStatus.DRAFT)))
			.thenThrow(new ConflictException("Transition de statut interdite : CONFIRMED -> DRAFT"));

		mockMvc.perform(patch("/api/orders/7/status").contentType(MediaType.APPLICATION_JSON).content("""
				{"status": "DRAFT"}
				"""))
			.andExpect(status().isConflict());
	}

	@Test
	void mapsUnknownOrderTo404() throws Exception {
		when(orderService.changeStatus(eq(99L), any())).thenThrow(new NotFoundException("Commande", 99L));

		mockMvc.perform(patch("/api/orders/99/status").contentType(MediaType.APPLICATION_JSON).content("""
				{"status": "CONFIRMED"}
				"""))
			.andExpect(status().isNotFound());
	}

	@Test
	void rejectsMissingStatus() throws Exception {
		mockMvc.perform(patch("/api/orders/7/status").contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest());
		verifyNoInteractions(orderService);
	}

	@Test
	void rejectsMissingLinesField() throws Exception {
		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
				{"clientId": 1}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.errors.lines").exists());
		verifyNoInteractions(orderService);
	}

	@Test
	void rejectsLineWithoutProduct() throws Exception {
		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
				{"clientId": 1, "lines": [{"quantity": 1}]}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors['lines[0].productId']").exists());
		verifyNoInteractions(orderService);
	}

	@Test
	void rejectsNullLine() throws Exception {
		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
				{"clientId": 1, "lines": [null]}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors['lines[0]']").exists());
		verifyNoInteractions(orderService);
	}

	@Test
	void rejectsMalformedJson() throws Exception {
		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("{\"clientId\": 1, "))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
		verifyNoInteractions(orderService);
	}

	@Test
	void mapsUnknownClientOrProductOnCreationTo404() throws Exception {
		when(orderService.create(any())).thenThrow(new NotFoundException("Client", 99L));

		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
				{"clientId": 99, "lines": [{"productId": 10, "quantity": 1}]}
				"""))
			.andExpect(status().isNotFound())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.detail").exists());
	}

	@Test
	void findByIdReturnsOrder() throws Exception {
		when(orderService.findById(7L)).thenReturn(new OrderResponse(7L, 1L, OrderStatus.CONFIRMED,
				List.of(new OrderLineResponse(1L, 10L, "KB-01", "Clavier", 2, new BigDecimal("10.00"),
						new BigDecimal("20.00"))),
				new BigDecimal("20.00")));

		mockMvc.perform(get("/api/orders/7"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CONFIRMED"))
			.andExpect(jsonPath("$.lines[0].productReference").value("KB-01"))
			.andExpect(jsonPath("$.total").value(20.00));
	}

	@Test
	void mapsUnknownOrderOnReadTo404() throws Exception {
		when(orderService.findById(99L)).thenThrow(new NotFoundException("Commande", 99L));

		mockMvc.perform(get("/api/orders/99"))
			.andExpect(status().isNotFound())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
	}

}
