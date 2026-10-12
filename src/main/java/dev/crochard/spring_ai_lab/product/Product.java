package dev.crochard.spring_ai_lab.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String reference;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private Double price;

	@Column(nullable = false)
	private boolean active;

	protected Product() {
	}

	public Product(String reference, String name, Double price, boolean active) {
		this.reference = reference;
		this.name = name;
		this.price = price;
		this.active = active;
	}

	public Long getId() {
		return id;
	}

	public String getReference() {
		return reference;
	}

	public String getName() {
		return name;
	}

	public Double getPrice() {
		return price;
	}

	public boolean isActive() {
		return active;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPrice(Double price) {
		this.price = price;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
