package dev.crochard.spring_ai_lab.order;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Accès aux commandes persistées. Les lectures chargent les lignes et leurs produits en une requête.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

	/**
	 * @param id identifiant de la commande
	 * @return la commande avec ses lignes et leurs produits, ou vide si elle n'existe pas
	 */
	@Override
	@EntityGraph(attributePaths = { "lines", "lines.product" })
	Optional<Order> findById(Long id);

	/**
	 * @return toutes les commandes avec leurs lignes et leurs produits
	 */
	@Override
	@EntityGraph(attributePaths = { "lines", "lines.product" })
	List<Order> findAll();

}
