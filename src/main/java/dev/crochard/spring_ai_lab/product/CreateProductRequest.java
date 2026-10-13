package dev.crochard.spring_ai_lab.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Corps de la requête de création d'un produit.
 * @param reference référence unique, obligatoire, 255 caractères maximum
 * @param name libellé, obligatoire, 255 caractères maximum
 * @param price prix unitaire, obligatoire, strictement positif, 2 décimales maximum
 * @param active {@code true} si le produit peut être commandé, obligatoire
 */
public record CreateProductRequest(
		@NotBlank @Size(max = 255) String reference,
		@NotBlank @Size(max = 255) String name,
		@NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal price,
		@NotNull Boolean active) {
}
