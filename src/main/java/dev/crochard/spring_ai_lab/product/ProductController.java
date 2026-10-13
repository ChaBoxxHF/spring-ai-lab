package dev.crochard.spring_ai_lab.product;

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
 * Point d'entrée REST de la ressource produit ({@code /api/products}).
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Produits", description = "Gestion du catalogue de produits")
public class ProductController {

	private final ProductService productService;

	/**
	 * @param productService service métier des produits
	 */
	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	/**
	 * Crée un produit.
	 * @param request données du produit à créer
	 * @return le produit créé (201), avec son URI dans l'en-tête {@code Location}
	 */
	@PostMapping
	@Operation(summary = "Créer un produit",
			description = "La référence doit être unique et le prix strictement positif (2 décimales maximum).")
	@ApiResponse(responseCode = "201", description = "Produit créé")
	@ApiResponse(responseCode = "400", description = "Requête invalide")
	@ApiResponse(responseCode = "409", description = "Un produit existe déjà avec cette référence")
	public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
		ProductResponse created = productService.create(request);
		return ResponseEntity.created(URI.create("/api/products/" + created.id())).body(created);
	}

	/**
	 * Active ou désactive un produit.
	 * @param id identifiant du produit
	 * @param request nouveau statut d'activité
	 * @return le produit mis à jour
	 */
	@PatchMapping("/{id}/status")
	@Operation(summary = "Activer ou désactiver un produit",
			description = "Un produit inactif ne peut plus être ajouté à une nouvelle commande.")
	@ApiResponse(responseCode = "200", description = "Produit mis à jour")
	@ApiResponse(responseCode = "400", description = "Requête invalide")
	@ApiResponse(responseCode = "404", description = "Produit introuvable")
	public ProductResponse updateStatus(@Parameter(description = "Identifiant du produit") @PathVariable Long id,
			@Valid @RequestBody UpdateProductStatusRequest request) {
		return productService.updateStatus(id, request);
	}

	/**
	 * Récupère un produit par son identifiant.
	 * @param id identifiant du produit
	 * @return le produit
	 */
	@GetMapping("/{id}")
	@Operation(summary = "Consulter un produit")
	@ApiResponse(responseCode = "200", description = "Produit trouvé")
	@ApiResponse(responseCode = "404", description = "Produit introuvable")
	public ProductResponse findById(@Parameter(description = "Identifiant du produit") @PathVariable Long id) {
		return productService.findById(id);
	}

	/**
	 * Liste tous les produits, actifs ou non.
	 * @return l'ensemble des produits
	 */
	@GetMapping
	@Operation(summary = "Lister les produits", description = "Renvoie les produits actifs et inactifs.")
	@ApiResponse(responseCode = "200", description = "Liste des produits")
	public List<ProductResponse> findAll() {
		return productService.findAll();
	}

}
