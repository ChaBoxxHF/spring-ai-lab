package dev.crochard.spring_ai_lab.product;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@PostMapping
	public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
		ProductResponse created = productService.create(request);
		return ResponseEntity.created(URI.create("/api/products/" + created.id())).body(created);
	}

	@PatchMapping("/{id}/status")
	public ProductResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateProductStatusRequest request) {
		return productService.updateStatus(id, request);
	}

	@GetMapping("/{id}")
	public ProductResponse findById(@PathVariable Long id) {
		return productService.findById(id);
	}

	@GetMapping
	public List<ProductResponse> findAll() {
		return productService.findAll();
	}

}
