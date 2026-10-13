package dev.crochard.spring_ai_lab.order;

import jakarta.validation.constraints.NotNull;

/**
 * Corps de la requête de changement de statut d'une commande.
 * @param status statut cible, obligatoire
 */
public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {
}
