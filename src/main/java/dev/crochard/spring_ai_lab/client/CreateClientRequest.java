package dev.crochard.spring_ai_lab.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClientRequest(
		@NotBlank @Size(max = 255) String firstName,
		@NotBlank @Size(max = 255) String lastName,
		@NotBlank @Email @Size(max = 255) String email) {
}
