package dev.crochard.spring_ai_lab.product;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entité JPA d'un produit du catalogue, persistée dans la table {@code products}. La référence est unique.
 */
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

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal price;

	@Column(nullable = false)
	private boolean active;

	/**
	 * Constructeur réservé à JPA.
	 */
	protected Product() {
	}

	/**
	 * @param reference référence unique du produit
	 * @param name libellé
	 * @param price prix unitaire courant
	 * @param active {@code true} si le produit peut être commandé
	 */
	public Product(String reference, String name, BigDecimal price, boolean active) {
		this.reference = reference;
		this.name = name;
		this.price = price;
		this.active = active;
	}

	/**
	 * @return l'identifiant, {@code null} tant que le produit n'est pas persisté
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @return la référence unique
	 */
	public String getReference() {
		return reference;
	}

	/**
	 * @return le libellé
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return le prix unitaire courant
	 */
	public BigDecimal getPrice() {
		return price;
	}

	/**
	 * @return {@code true} si le produit peut être ajouté à une commande
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * @param active {@code true} pour activer le produit, {@code false} pour le désactiver
	 */
	public void setActive(boolean active) {
		this.active = active;
	}

}
