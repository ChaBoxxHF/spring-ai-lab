package dev.crochard.spring_ai_lab.product;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Accès aux produits persistés.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

	/**
	 * @param reference référence recherchée
	 * @return {@code true} si un produit possède déjà cette référence
	 */
	boolean existsByReference(String reference);

}
