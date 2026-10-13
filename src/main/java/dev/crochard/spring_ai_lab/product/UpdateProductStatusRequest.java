package dev.crochard.spring_ai_lab.product;

import jakarta.validation.constraints.NotNull;

/**
 * Corps de la requête d'activation / désactivation d'un produit.
 * @param active {@code true} pour activer le produit, {@code false} pour le désactiver ; obligatoire
 */
public record UpdateProductStatusRequest(@NotNull Boolean active) {
}
