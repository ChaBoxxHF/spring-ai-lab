package dev.crochard.spring_ai_lab.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import dev.crochard.spring_ai_lab.client.Client;
import dev.crochard.spring_ai_lab.client.ClientRepository;
import dev.crochard.spring_ai_lab.common.BusinessRuleViolationException;
import dev.crochard.spring_ai_lab.common.ConflictException;
import dev.crochard.spring_ai_lab.common.NotFoundException;
import dev.crochard.spring_ai_lab.order.CreateOrderRequest.OrderLineRequest;
import dev.crochard.spring_ai_lab.product.Product;
import dev.crochard.spring_ai_lab.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private ClientRepository clientRepository;

	@Mock
	private ProductRepository productRepository;

	@InjectMocks
	private OrderService orderService;

	private Client client;

	private Product keyboard;

	private Product mouse;

	@BeforeEach
	void setUp() {
		client = withId(new Client("Ada", "Lovelace", "ada@example.com"), 1L);
		keyboard = withId(new Product("KB-01", "Clavier", new BigDecimal("49.99"), true), 10L);
		mouse = withId(new Product("MS-01", "Souris", new BigDecimal("19.90"), true), 11L);
	}

	@Test
	void createsDraftOrderWithLineAndOrderTotals() {
		when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
		when(productRepository.findById(10L)).thenReturn(Optional.of(keyboard));
		when(productRepository.findById(11L)).thenReturn(Optional.of(mouse));
		when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

		OrderResponse response = orderService.create(new CreateOrderRequest(1L,
				List.of(new OrderLineRequest(10L, 3), new OrderLineRequest(11L, 2))));

		assertThat(response.clientId()).isEqualTo(1L);
		assertThat(response.status()).isEqualTo(OrderStatus.DRAFT);
		assertThat(response.lines()).hasSize(2);
		assertThat(response.lines().get(0).unitPrice()).isEqualByComparingTo("49.99");
		assertThat(response.lines().get(0).lineTotal()).isEqualByComparingTo("149.97");
		assertThat(response.lines().get(1).lineTotal()).isEqualByComparingTo("39.80");
		assertThat(response.total()).isEqualByComparingTo("189.77");
	}

	@Test
	void memorizesProductPriceAtOrderTime() {
		when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
		when(productRepository.findById(10L)).thenReturn(Optional.of(keyboard));
		when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
		orderService.create(new CreateOrderRequest(1L, List.of(new OrderLineRequest(10L, 2))));

		// Le prix catalogue change après la commande : les totaux recalculés utilisent toujours l'ancien prix.
		ReflectionTestUtils.setField(keyboard, "price", new BigDecimal("99.00"));
		OrderResponse after = OrderResponse.from(captureSavedOrder());

		assertThat(after.lines().getFirst().unitPrice()).isEqualByComparingTo("49.99");
		assertThat(after.lines().getFirst().lineTotal()).isEqualByComparingTo("99.98");
		assertThat(after.total()).isEqualByComparingTo("99.98");
	}

	@Test
	void mergesLinesForTheSameProductBySummingQuantities() {
		when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
		when(productRepository.findById(10L)).thenReturn(Optional.of(keyboard));
		when(productRepository.findById(11L)).thenReturn(Optional.of(mouse));
		when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

		OrderResponse response = orderService.create(new CreateOrderRequest(1L, List.of(
				new OrderLineRequest(10L, 2), new OrderLineRequest(11L, 1), new OrderLineRequest(10L, 3))));

		assertThat(response.lines()).extracting(OrderResponse.OrderLineResponse::productId,
				OrderResponse.OrderLineResponse::quantity)
			.containsExactly(org.assertj.core.groups.Tuple.tuple(10L, 5), org.assertj.core.groups.Tuple.tuple(11L, 1));
		assertThat(response.lines().getFirst().lineTotal()).isEqualByComparingTo("249.95");
		assertThat(response.total()).isEqualByComparingTo("269.85");
	}

	@Test
	void rejectsMergedQuantityOverflow() {
		when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

		assertThatThrownBy(() -> orderService.create(new CreateOrderRequest(1L,
				List.of(new OrderLineRequest(10L, Integer.MAX_VALUE), new OrderLineRequest(10L, 1)))))
			.isInstanceOf(BusinessRuleViolationException.class);
		verify(orderRepository, never()).save(any());
	}

	@Test
	void acceptsQuantityOfOne() {
		when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
		when(productRepository.findById(10L)).thenReturn(Optional.of(keyboard));
		when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

		OrderResponse response = orderService
			.create(new CreateOrderRequest(1L, List.of(new OrderLineRequest(10L, 1))));

		assertThat(response.lines().getFirst().quantity()).isEqualTo(1);
		assertThat(response.total()).isEqualByComparingTo("49.99");
	}

	@Test
	void rejectsInactiveProduct() {
		Product inactive = withId(new Product("OLD-01", "Ancien", new BigDecimal("5.00"), false), 12L);
		when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
		when(productRepository.findById(12L)).thenReturn(Optional.of(inactive));

		assertThatThrownBy(() -> orderService
			.create(new CreateOrderRequest(1L, List.of(new OrderLineRequest(12L, 1)))))
			.isInstanceOf(BusinessRuleViolationException.class)
			.hasMessageContaining("inactif");
		verify(orderRepository, never()).save(any());
	}

	@Test
	void rejectsOrderWithoutLines() {
		assertThatThrownBy(() -> orderService.create(new CreateOrderRequest(1L, List.of())))
			.isInstanceOf(BusinessRuleViolationException.class);
		verify(orderRepository, never()).save(any());
	}

	@Test
	void rejectsNonPositiveQuantity() {
		when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

		assertThatThrownBy(() -> orderService
			.create(new CreateOrderRequest(1L, List.of(new OrderLineRequest(10L, 0)))))
			.isInstanceOf(BusinessRuleViolationException.class);
		verify(orderRepository, never()).save(any());
	}

	@Test
	void rejectsUnknownClient() {
		when(clientRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> orderService
			.create(new CreateOrderRequest(99L, List.of(new OrderLineRequest(10L, 1)))))
			.isInstanceOf(NotFoundException.class);
		verify(orderRepository, never()).save(any());
	}

	@Test
	void rejectsUnknownProduct() {
		when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
		when(productRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> orderService
			.create(new CreateOrderRequest(1L, List.of(new OrderLineRequest(99L, 1)))))
			.isInstanceOf(NotFoundException.class);
		verify(orderRepository, never()).save(any());
	}

	@Test
	void rejectsNegativeQuantity() {
		when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

		assertThatThrownBy(() -> orderService
			.create(new CreateOrderRequest(1L, List.of(new OrderLineRequest(10L, -1)))))
			.isInstanceOf(BusinessRuleViolationException.class);
		verify(orderRepository, never()).save(any());
	}

	@Test
	void rejectsMissingQuantity() {
		when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

		assertThatThrownBy(() -> orderService
			.create(new CreateOrderRequest(1L, List.of(new OrderLineRequest(10L, null)))))
			.isInstanceOf(BusinessRuleViolationException.class);
		verify(orderRepository, never()).save(any());
	}

	@Test
	void rejectsMissingLines() {
		assertThatThrownBy(() -> orderService.create(new CreateOrderRequest(1L, null)))
			.isInstanceOf(BusinessRuleViolationException.class);
		verify(orderRepository, never()).save(any());
	}

	@Test
	void rejectsInactiveProductAmongActiveOnes() {
		Product inactive = withId(new Product("OLD-01", "Ancien", new BigDecimal("5.00"), false), 12L);
		when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
		when(productRepository.findById(10L)).thenReturn(Optional.of(keyboard));
		when(productRepository.findById(12L)).thenReturn(Optional.of(inactive));

		assertThatThrownBy(() -> orderService.create(new CreateOrderRequest(1L,
				List.of(new OrderLineRequest(10L, 1), new OrderLineRequest(12L, 1)))))
			.isInstanceOf(BusinessRuleViolationException.class)
			.hasMessageContaining("inactif");
		verify(orderRepository, never()).save(any());
	}

	@Test
	void appliesAllowedTransition() {
		Order order = withId(new Order(client), 5L);
		when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

		OrderResponse response = orderService.changeStatus(5L, OrderStatus.CONFIRMED);

		assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
	}

	@Test
	void rejectsForbiddenTransitionAndKeepsStatus() {
		Order order = withId(new Order(client), 5L);
		order.setStatus(OrderStatus.CANCELLED);
		when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> orderService.changeStatus(5L, OrderStatus.CONFIRMED))
			.isInstanceOf(ConflictException.class)
			.hasMessageContaining("CANCELLED -> CONFIRMED");
		assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
	}

	@ParameterizedTest(name = "{0} -> CANCELLED")
	@EnumSource(value = OrderStatus.class, names = { "DRAFT", "CONFIRMED" })
	void cancelsOrder(OrderStatus from) {
		Order order = withId(new Order(client), 5L);
		order.setStatus(from);
		when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

		OrderResponse response = orderService.changeStatus(5L, OrderStatus.CANCELLED);

		assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
	}

	@Test
	void rejectsDoubleCancellation() {
		Order order = withId(new Order(client), 5L);
		order.setStatus(OrderStatus.CANCELLED);
		when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> orderService.changeStatus(5L, OrderStatus.CANCELLED))
			.isInstanceOf(ConflictException.class)
			.hasMessageContaining("CANCELLED -> CANCELLED");
		assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
	}

	@Test
	void rejectsDoubleConfirmation() {
		Order order = withId(new Order(client), 5L);
		order.setStatus(OrderStatus.CONFIRMED);
		when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> orderService.changeStatus(5L, OrderStatus.CONFIRMED))
			.isInstanceOf(ConflictException.class)
			.hasMessageContaining("CONFIRMED -> CONFIRMED");
		assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
	}

	@ParameterizedTest(name = "{0} -> {1} refusée")
	@CsvSource({ "DRAFT, DRAFT", "CONFIRMED, DRAFT", "CANCELLED, DRAFT" })
	void rejectsOtherForbiddenTransitionsAndKeepsStatus(OrderStatus from, OrderStatus to) {
		Order order = withId(new Order(client), 5L);
		order.setStatus(from);
		when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> orderService.changeStatus(5L, to)).isInstanceOf(ConflictException.class);
		assertThat(order.getStatus()).isEqualTo(from);
	}

	@Test
	void changeStatusOfUnknownOrderFails() {
		when(orderRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> orderService.changeStatus(99L, OrderStatus.CONFIRMED))
			.isInstanceOf(NotFoundException.class);
	}

	private Order captureSavedOrder() {
		org.mockito.ArgumentCaptor<Order> captor = org.mockito.ArgumentCaptor.forClass(Order.class);
		verify(orderRepository).save(captor.capture());
		return captor.getValue();
	}

	private static <T> T withId(T entity, Long id) {
		ReflectionTestUtils.setField(entity, "id", id);
		return entity;
	}

}
