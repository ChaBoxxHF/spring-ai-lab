package dev.crochard.spring_ai_lab.product;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/products")
public class ProductController {

	private final ProductRepository productRepository;

	public ProductController(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	@GetMapping
	public List<Product> getAll(@RequestParam(required = false) Boolean active) {
		if (active != null) {
			return productRepository.findAll().stream().filter(p -> p.isActive() == active).toList();
		}
		return productRepository.findAll();
	}

	@GetMapping("/{id}")
	public Product getById(@PathVariable Long id) {
		return productRepository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Product create(@RequestBody Product product) {
		if (productRepository.existsByReference(product.getReference())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Reference already exists");
		}
		return productRepository.save(product);
	}

	@PutMapping("/{id}")
	public Product update(@PathVariable Long id, @RequestBody Product updated) {
		Product existing = productRepository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
		existing.setReference(updated.getReference());
		existing.setName(updated.getName());
		existing.setPrice(updated.getPrice());
		existing.setActive(updated.isActive());
		return productRepository.save(existing);
	}
}
