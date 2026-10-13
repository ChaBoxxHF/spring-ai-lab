package dev.crochard.spring_ai_lab.client;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {

	boolean existsByEmail(String email);

}
