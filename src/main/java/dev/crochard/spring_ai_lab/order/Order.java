package dev.crochard.spring_ai_lab.order;

import dev.crochard.spring_ai_lab.client.Client;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "client_id", nullable = false)
	private Client client;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "order_id", nullable = false)
	private List<OrderItem> items = new ArrayList<>();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OrderStatus status = OrderStatus.DRAFT;

	protected Order() {
	}

	public Order(Client client) {
		this.client = client;
	}

	public Long getId() {
		return id;
	}

	public Client getClient() {
		return client;
	}

	public List<OrderItem> getItems() {
		return items;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public void addItem(OrderItem item) {
		this.items.add(item);
	}

	public Double getTotal() {
		return items.stream().mapToDouble(OrderItem::getLineTotal).sum();
	}

	public void confirm() {
		transitionTo(OrderStatus.CONFIRMED);
	}

	public void cancel() {
		transitionTo(OrderStatus.CANCELLED);
	}

	private void transitionTo(OrderStatus newStatus) {
		if (!this.status.canTransitionTo(newStatus)) {
			throw new IllegalStateException(
					"Transition not allowed from " + this.status + " to " + newStatus);
		}
		this.status = newStatus;
	}
}
