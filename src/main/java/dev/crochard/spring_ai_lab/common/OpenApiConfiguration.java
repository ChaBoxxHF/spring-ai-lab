package dev.crochard.spring_ai_lab.common;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

import org.springframework.context.annotation.Configuration;

/**
 * Métadonnées de la documentation OpenAPI exposée par springdoc.
 * <p>
 * La spécification est disponible sur {@code /v3/api-docs} et l'interface Swagger UI sur
 * {@code /swagger-ui.html}.
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "Spring AI Lab - API de gestion de commandes", version = "0.0.1",
		description = "Gestion des clients, des produits et des commandes. Les erreurs sont renvoyées au format "
				+ "Problem Details (RFC 9457)."))
public class OpenApiConfiguration {

}
