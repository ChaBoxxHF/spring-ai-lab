package dev.crochard.spring_ai_lab.order;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping
	public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
		OrderResponse created = orderService.create(request);
		return ResponseEntity.created(URI.create("/api/orders/" + created.id())).body(created);
	}

	@PatchMapping("/{id}/status")
	public OrderResponse changeStatus(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
		return orderService.changeStatus(id, request.status());
	}

	@GetMapping("/{id}")
	public OrderResponse findById(@PathVariable Long id) {
		return orderService.findById(id);
	}

	@GetMapping
	public List<OrderResponse> findAll() {
		return orderService.findAll();
	}

}
