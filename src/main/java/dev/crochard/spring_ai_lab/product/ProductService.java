package dev.crochard.spring_ai_lab.product;

import java.util.List;

import dev.crochard.spring_ai_lab.common.BusinessRuleViolationException;
import dev.crochard.spring_ai_lab.common.ConflictException;
import dev.crochard.spring_ai_lab.common.NotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logique métier des produits. Les méthodes sont en lecture seule par défaut.
 */
@Service
@Transactional(readOnly = true)
public class ProductService {

	private final ProductRepository productRepository;

	/**
	 * @param productRepository accès aux produits persistés
	 */
	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	/**
	 * Crée un produit.
	 * @param request données du produit à créer
	 * @return le produit créé
	 * @throws BusinessRuleViolationException si le prix n'est pas strictement positif
	 * @throws ConflictException si un produit existe déjà avec cette référence
	 */
	@Transactional
	public ProductResponse create(CreateProductRequest request) {
		if (request.price().signum() <= 0) {
			throw new BusinessRuleViolationException("Le prix d'un produit doit être strictement positif");
		}
		if (productRepository.existsByReference(request.reference())) {
			throw new ConflictException("Un produit existe déjà avec la référence " + request.reference());
		}
		Product product = productRepository
			.save(new Product(request.reference(), request.name(), request.price(), request.active()));
		return ProductResponse.from(product);
	}

	/**
	 * Active ou désactive un produit.
	 * @param id identifiant du produit
	 * @param request nouveau statut d'activité
	 * @return le produit mis à jour
	 * @throws NotFoundException si le produit n'existe pas
	 */
	@Transactional
	public ProductResponse updateStatus(Long id, UpdateProductStatusRequest request) {
		Product product = getProduct(id);
		product.setActive(request.active());
		return ProductResponse.from(product);
	}

	/**
	 * Récupère un produit par son identifiant.
	 * @param id identifiant du produit
	 * @return le produit
	 * @throws NotFoundException si le produit n'existe pas
	 */
	public ProductResponse findById(Long id) {
		return ProductResponse.from(getProduct(id));
	}

	/**
	 * Liste tous les produits, actifs ou non.
	 * @return l'ensemble des produits
	 */
	public List<ProductResponse> findAll() {
		return productRepository.findAll().stream().map(ProductResponse::from).toList();
	}

	/**
	 * Charge l'entité produit.
	 * @param id identifiant du produit
	 * @return l'entité produit
	 * @throws NotFoundException si le produit n'existe pas
	 */
	private Product getProduct(Long id) {
		return productRepository.findById(id).orElseThrow(() -> new NotFoundException("Produit", id));
	}

}
