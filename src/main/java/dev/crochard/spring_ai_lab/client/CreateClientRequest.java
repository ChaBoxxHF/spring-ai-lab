package dev.crochard.spring_ai_lab.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corps de la requête de création d'un client.
 * @param firstName prénom, obligatoire, 255 caractères maximum
 * @param lastName nom, obligatoire, 255 caractères maximum
 * @param email adresse email valide, obligatoire, 255 caractères maximum
 */
public record CreateClientRequest(
		@NotBlank @Size(max = 255) String firstName,
		@NotBlank @Size(max = 255) String lastName,
		@NotBlank @Email @Size(max = 255) String email) {
}
