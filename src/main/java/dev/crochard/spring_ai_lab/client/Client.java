package dev.crochard.spring_ai_lab.client;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entité JPA d'un client, persistée dans la table {@code clients}. L'email est unique.
 */
@Entity
@Table(name = "clients")
public class Client {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "first_name", nullable = false)
	private String firstName;

	@Column(name = "last_name", nullable = false)
	private String lastName;

	@Column(nullable = false, unique = true)
	private String email;

	/**
	 * Constructeur réservé à JPA.
	 */
	protected Client() {
	}

	/**
	 * @param firstName prénom
	 * @param lastName nom
	 * @param email adresse email
	 */
	public Client(String firstName, String lastName, String email) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
	}

	/**
	 * @return l'identifiant, {@code null} tant que le client n'est pas persisté
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @return le prénom
	 */
	public String getFirstName() {
		return firstName;
	}

	/**
	 * @return le nom
	 */
	public String getLastName() {
		return lastName;
	}

	/**
	 * @return l'adresse email
	 */
	public String getEmail() {
		return email;
	}

}
