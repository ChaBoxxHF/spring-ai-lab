package dev.crochard.spring_ai_lab.order;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

	@Override
	@EntityGraph(attributePaths = { "lines", "lines.product" })
	Optional<Order> findById(Long id);

	@Override
	@EntityGraph(attributePaths = { "lines", "lines.product" })
	List<Order> findAll();

}
