package dev.crochard.spring_ai_lab.product;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

	boolean existsByReference(String reference);

}
