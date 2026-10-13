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

/**
 * Entité JPA d'une commande, persistée dans la table {@code orders}. Une commande appartient à un client et
 * agrège ses lignes, qui sont persistées et supprimées avec elle.
 */
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

	/**
	 * Constructeur réservé à JPA.
	 */
	protected Order() {
	}

	/**
	 * Crée une commande vide au statut {@link OrderStatus#DRAFT}.
	 * @param client client passant la commande
	 */
	public Order(Client client) {
		this.client = client;
		this.status = OrderStatus.DRAFT;
	}

	/**
	 * Ajoute une ligne en figeant le prix courant du produit comme prix unitaire.
	 * @param product produit commandé
	 * @param quantity quantité commandée
	 */
	public void addLine(Product product, int quantity) {
		lines.add(new OrderLine(this, product, quantity, product.getPrice()));
	}

	/**
	 * Total commande = somme des totaux de lignes.
	 * @return le total de la commande, {@link BigDecimal#ZERO} si elle n'a aucune ligne
	 */
	public BigDecimal total() {
		return lines.stream().map(OrderLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	/**
	 * @return l'identifiant, {@code null} tant que la commande n'est pas persistée
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @return le client ayant passé la commande
	 */
	public Client getClient() {
		return client;
	}

	/**
	 * @return le statut actuel
	 */
	public OrderStatus getStatus() {
		return status;
	}

	/**
	 * Modifie le statut sans contrôle : la validité de la transition est vérifiée par {@link OrderService}.
	 * @param status nouveau statut
	 */
	void setStatus(OrderStatus status) {
		this.status = status;
	}

	/**
	 * @return les lignes de la commande, triées par identifiant, en lecture seule
	 */
	public List<OrderLine> getLines() {
		return Collections.unmodifiableList(lines);
	}

}
