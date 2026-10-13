package dev.crochard.spring_ai_lab.order;

import java.net.URI;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Point d'entrée REST de la ressource commande ({@code /api/orders}).
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Commandes", description = "Création, suivi du statut et consultation des commandes")
public class OrderController {

	private final OrderService orderService;

	/**
	 * @param orderService service métier des commandes
	 */
	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	/**
	 * Crée une commande au statut {@link OrderStatus#DRAFT}.
	 * @param request client et lignes de la commande
	 * @return la commande créée (201), avec son URI dans l'en-tête {@code Location}
	 */
	@PostMapping
	@Operation(summary = "Créer une commande",
			description = "La commande est créée au statut DRAFT. Elle doit contenir au moins une ligne, chaque quantité "
					+ "doit être strictement positive et seuls les produits actifs sont acceptés. Les lignes portant sur un "
					+ "même produit sont fusionnées. Le prix unitaire de chaque ligne est celui du produit au moment de la "
					+ "commande.")
	@ApiResponse(responseCode = "201", description = "Commande créée")
	@ApiResponse(responseCode = "400", description = "Requête invalide")
	@ApiResponse(responseCode = "404", description = "Client ou produit introuvable")
	@ApiResponse(responseCode = "422",
			description = "Produit inactif, ou quantité cumulée d'un produit dépassant la valeur maximale autorisée")
	public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
		OrderResponse created = orderService.create(request);
		return ResponseEntity.created(URI.create("/api/orders/" + created.id())).body(created);
	}

	/**
	 * Change le statut d'une commande.
	 * @param id identifiant de la commande
	 * @param request statut cible
	 * @return la commande mise à jour
	 * @see OrderStatus#canTransitionTo(OrderStatus)
	 */
	@PatchMapping("/{id}/status")
	@Operation(summary = "Changer le statut d'une commande",
			description = "Transitions autorisées : DRAFT -> CONFIRMED, DRAFT -> CANCELLED, CONFIRMED -> CANCELLED.")
	@ApiResponse(responseCode = "200", description = "Statut modifié")
	@ApiResponse(responseCode = "400", description = "Requête invalide")
	@ApiResponse(responseCode = "404", description = "Commande introuvable")
	@ApiResponse(responseCode = "409", description = "Transition interdite ou modification concurrente")
	public OrderResponse changeStatus(@Parameter(description = "Identifiant de la commande") @PathVariable Long id,
			@Valid @RequestBody UpdateOrderStatusRequest request) {
		return orderService.changeStatus(id, request.status());
	}

	/**
	 * Récupère une commande par son identifiant.
	 * @param id identifiant de la commande
	 * @return la commande avec ses lignes et son total
	 */
	@GetMapping("/{id}")
	@Operation(summary = "Consulter une commande")
	@ApiResponse(responseCode = "200", description = "Commande trouvée")
	@ApiResponse(responseCode = "404", description = "Commande introuvable")
	public OrderResponse findById(@Parameter(description = "Identifiant de la commande") @PathVariable Long id) {
		return orderService.findById(id);
	}

	/**
	 * Liste toutes les commandes.
	 * @return l'ensemble des commandes avec leurs lignes et leur total
	 */
	@GetMapping
	@Operation(summary = "Lister les commandes")
	@ApiResponse(responseCode = "200", description = "Liste des commandes")
	public List<OrderResponse> findAll() {
		return orderService.findAll();
	}

}
