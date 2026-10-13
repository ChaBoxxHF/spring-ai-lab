package dev.crochard.spring_ai_lab.product;

import java.util.List;

import dev.crochard.spring_ai_lab.common.BusinessRuleViolationException;
import dev.crochard.spring_ai_lab.common.ConflictException;
import dev.crochard.spring_ai_lab.common.NotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

	private final ProductRepository productRepository;

	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

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

	@Transactional
	public ProductResponse updateStatus(Long id, UpdateProductStatusRequest request) {
		Product product = getProduct(id);
		product.setActive(request.active());
		return ProductResponse.from(product);
	}

	public ProductResponse findById(Long id) {
		return ProductResponse.from(getProduct(id));
	}

	public List<ProductResponse> findAll() {
		return productRepository.findAll().stream().map(ProductResponse::from).toList();
	}

	private Product getProduct(Long id) {
		return productRepository.findById(id).orElseThrow(() -> new NotFoundException("Produit", id));
	}

}
