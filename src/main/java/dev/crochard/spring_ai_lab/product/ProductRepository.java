package dev.crochard.spring_ai_lab.product;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

	Optional<Product> findByReference(String reference);

	boolean existsByReference(String reference);
}
