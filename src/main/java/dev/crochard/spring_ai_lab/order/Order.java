package dev.crochard.spring_ai_lab.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.crochard.spring_ai_lab.client.Client;
import dev.crochard.spring_ai_lab.product.Product;
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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "orders")
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "client_id", nullable = false)
	private Client client;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OrderStatus status;

	/** Verrouillage optimiste : empêche deux changements de statut concurrents de contourner les transitions. */
	@Version
	private long version;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id")
	private List<OrderLine> lines = new ArrayList<>();

	protected Order() {
	}

	public Order(Client client) {
		this.client = client;
		this.status = OrderStatus.DRAFT;
	}

	public void addLine(Product product, int quantity) {
		lines.add(new OrderLine(this, product, quantity, product.getPrice()));
	}

	/** Total commande = somme des totaux de lignes. */
	public BigDecimal total() {
		return lines.stream().map(OrderLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public Long getId() {
		return id;
	}

	public Client getClient() {
		return client;
	}

	public OrderStatus getStatus() {
		return status;
	}

	void setStatus(OrderStatus status) {
		this.status = status;
	}

	public List<OrderLine> getLines() {
		return Collections.unmodifiableList(lines);
	}

}
