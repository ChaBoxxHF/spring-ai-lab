package dev.crochard.spring_ai_lab.order;

import dev.crochard.spring_ai_lab.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(nullable = false)
	private Integer quantity;

	@Column(nullable = false)
	private Double unitPrice;

	protected OrderItem() {
	}

	public OrderItem(Product product, Integer quantity, Double unitPrice) {
		if (quantity == null || quantity <= 0) {
			throw new IllegalArgumentException("Quantity must be strictly positive");
		}
		this.product = product;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
	}

	public Long getId() {
		return id;
	}

	public Product getProduct() {
		return product;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public Double getUnitPrice() {
		return unitPrice;
	}

	public Double getLineTotal() {
		return quantity * unitPrice;
	}
}
