package dev.crochard.spring_ai_lab.client;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Accès aux clients persistés.
 */
public interface ClientRepository extends JpaRepository<Client, Long> {

	/**
	 * @param email adresse email recherchée (comparaison exacte)
	 * @return {@code true} si un client possède déjà cet email
	 */
	boolean existsByEmail(String email);

}
