package dev.crochard.spring_ai_lab.client;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {

	Optional<Client> findByEmail(String email);

	boolean existsByEmail(String email);
}
