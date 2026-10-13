package dev.crochard.spring_ai_lab.order;

import java.math.BigDecimal;

import dev.crochard.spring_ai_lab.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entité JPA d'une ligne de commande, persistée dans la table {@code order_lines}. Elle est créée via
 * {@link Order#addLine(Product, int)}.
 */
@Entity
@Table(name = "order_lines")
public class OrderLine {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(nullable = false)
	private int quantity;

	/** Prix du produit mémorisé au moment de la commande. */
	@Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
	private BigDecimal unitPrice;

	/**
	 * Constructeur réservé à JPA.
	 */
	protected OrderLine() {
	}

	/**
	 * @param order commande parente
	 * @param product produit commandé
	 * @param quantity quantité commandée
	 * @param unitPrice prix unitaire figé au moment de la commande
	 */
	OrderLine(Order order, Product product, int quantity, BigDecimal unitPrice) {
		this.order = order;
		this.product = product;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
	}

	/**
	 * Total ligne = quantité × prix unitaire.
	 * @return le total de la ligne
	 */
	public BigDecimal lineTotal() {
		return unitPrice.multiply(BigDecimal.valueOf(quantity));
	}

	/**
	 * @return l'identifiant, {@code null} tant que la ligne n'est pas persistée
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @return le produit commandé
	 */
	public Product getProduct() {
		return product;
	}

	/**
	 * @return la quantité commandée
	 */
	public int getQuantity() {
		return quantity;
	}

	/**
	 * @return le prix unitaire figé au moment de la commande
	 */
	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

}
