package dev.crochard.spring_ai_lab.order;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.crochard.spring_ai_lab.client.Client;
import dev.crochard.spring_ai_lab.client.ClientRepository;
import dev.crochard.spring_ai_lab.common.BusinessRuleViolationException;
import dev.crochard.spring_ai_lab.common.ConflictException;
import dev.crochard.spring_ai_lab.common.NotFoundException;
import dev.crochard.spring_ai_lab.order.CreateOrderRequest.OrderLineRequest;
import dev.crochard.spring_ai_lab.product.Product;
import dev.crochard.spring_ai_lab.product.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logique métier des commandes : création, changement de statut et consultation. Les méthodes sont en lecture
 * seule par défaut.
 */
@Service
@Transactional(readOnly = true)
public class OrderService {

	private final OrderRepository orderRepository;

	private final ClientRepository clientRepository;

	private final ProductRepository productRepository;

	/**
	 * @param orderRepository accès aux commandes persistées
	 * @param clientRepository accès aux clients, pour rattacher la commande à son client
	 * @param productRepository accès aux produits, pour constituer les lignes de commande
	 */
	public OrderService(OrderRepository orderRepository, ClientRepository clientRepository,
			ProductRepository productRepository) {
		this.orderRepository = orderRepository;
		this.clientRepository = clientRepository;
		this.productRepository = productRepository;
	}

	/**
	 * Crée une commande au statut {@link OrderStatus#DRAFT}.
	 * <p>
	 * Les lignes portant sur un même produit sont fusionnées. Chaque ligne mémorise le prix courant du produit.
	 * @param request client et lignes de la commande
	 * @return la commande créée
	 * @throws BusinessRuleViolationException si la commande n'a aucune ligne, si une quantité n'est pas strictement
	 * positive, si la quantité cumulée d'un produit déborde ou si un produit est inactif
	 * @throws NotFoundException si le client ou un produit n'existe pas
	 */
	@Transactional
	public OrderResponse create(CreateOrderRequest request) {
		if (request.lines() == null || request.lines().isEmpty()) {
			throw new BusinessRuleViolationException("Une commande doit contenir au moins une ligne");
		}
		Client client = clientRepository.findById(request.clientId())
			.orElseThrow(() -> new NotFoundException("Client", request.clientId()));

		Order order = new Order(client);
		mergeQuantitiesByProduct(request.lines()).forEach((productId, quantity) -> {
			Product product = productRepository.findById(productId)
				.orElseThrow(() -> new NotFoundException("Produit", productId));
			if (!product.isActive()) {
				throw new BusinessRuleViolationException(
						"Le produit " + product.getId() + " est inactif et ne peut pas être ajouté à une commande");
			}
			order.addLine(product, quantity);
		});
		return OrderResponse.from(orderRepository.save(order));
	}

	/**
	 * Les lignes portant sur un même produit sont fusionnées en une seule ligne dont la quantité est la
	 * somme des quantités. L'ordre de première apparition des produits est conservé.
	 * @param lines lignes demandées
	 * @return quantité totale par identifiant de produit
	 * @throws BusinessRuleViolationException si une quantité n'est pas strictement positive ou si la somme dépasse
	 * {@link Integer#MAX_VALUE}
	 */
	private static Map<Long, Integer> mergeQuantitiesByProduct(List<OrderLineRequest> lines) {
		Map<Long, Integer> quantities = new LinkedHashMap<>();
		for (OrderLineRequest line : lines) {
			if (line.quantity() == null || line.quantity() <= 0) {
				throw new BusinessRuleViolationException("La quantité d'une ligne doit être strictement positive");
			}
			try {
				quantities.merge(line.productId(), line.quantity(), Math::addExact);
			}
			catch (ArithmeticException ex) {
				throw new BusinessRuleViolationException(
						"La quantité cumulée du produit " + line.productId() + " dépasse la valeur maximale autorisée");
			}
		}
		return quantities;
	}

	/**
	 * Change le statut d'une commande.
	 * @param id identifiant de la commande
	 * @param target statut cible
	 * @return la commande mise à jour
	 * @throws NotFoundException si la commande n'existe pas
	 * @throws ConflictException si la transition depuis le statut actuel est interdite
	 * @see OrderStatus#canTransitionTo(OrderStatus)
	 */
	@Transactional
	public OrderResponse changeStatus(Long id, OrderStatus target) {
		Order order = getOrder(id);
		if (!order.getStatus().canTransitionTo(target)) {
			throw new ConflictException(
					"Transition de statut interdite : " + order.getStatus() + " -> " + target);
		}
		order.setStatus(target);
		return OrderResponse.from(order);
	}

	/**
	 * Récupère une commande par son identifiant.
	 * @param id identifiant de la commande
	 * @return la commande avec ses lignes et son total
	 * @throws NotFoundException si la commande n'existe pas
	 */
	public OrderResponse findById(Long id) {
		return OrderResponse.from(getOrder(id));
	}

	/**
	 * Liste toutes les commandes.
	 * @return l'ensemble des commandes avec leurs lignes et leur total
	 */
	public List<OrderResponse> findAll() {
		return orderRepository.findAll().stream().map(OrderResponse::from).toList();
	}

	/**
	 * Charge l'entité commande avec ses lignes.
	 * @param id identifiant de la commande
	 * @return l'entité commande
	 * @throws NotFoundException si la commande n'existe pas
	 */
	private Order getOrder(Long id) {
		return orderRepository.findById(id).orElseThrow(() -> new NotFoundException("Commande", id));
	}

}
