package dev.crochard.spring_ai_lab.order;

import dev.crochard.spring_ai_lab.client.Client;
import dev.crochard.spring_ai_lab.client.ClientRepository;
import dev.crochard.spring_ai_lab.product.Product;
import dev.crochard.spring_ai_lab.product.ProductRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

	private final OrderRepository orderRepository;
	private final ClientRepository clientRepository;
	private final ProductRepository productRepository;

	public OrderController(OrderRepository orderRepository, ClientRepository clientRepository,
			ProductRepository productRepository) {
		this.orderRepository = orderRepository;
		this.clientRepository = clientRepository;
		this.productRepository = productRepository;
	}

	@GetMapping
	public List<Order> getAll() {
		return orderRepository.findAll();
	}

	@GetMapping("/{id}")
	public Order getById(@PathVariable Long id) {
		return orderRepository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Order create(@RequestBody OrderRequest request) {
		Client client = clientRepository.findById(request.clientId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

		Order order = new Order(client);

		for (OrderRequestItem item : request.items()) {
			Product product = productRepository.findById(item.productId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Product not found: " + item.productId()));

			if (!product.isActive()) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Product is inactive: " + product.getReference());
			}

			order.addItem(new OrderItem(product, item.quantity(), product.getPrice()));
		}

		if (order.getItems().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order must have at least one item");
		}

		return orderRepository.save(order);
	}

	@PostMapping("/{id}/confirm")
	public Order confirm(@PathVariable Long id) {
		Order order = orderRepository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
		order.confirm();
		return orderRepository.save(order);
	}

	@PostMapping("/{id}/cancel")
	public Order cancel(@PathVariable Long id) {
		Order order = orderRepository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
		order.cancel();
		return orderRepository.save(order);
	}

	public record OrderRequest(Long clientId, List<OrderRequestItem> items) {
	}

	public record OrderRequestItem(Long productId, Integer quantity) {
	}
}
