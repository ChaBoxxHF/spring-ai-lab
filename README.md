# spring-ai-lab

API REST de gestion de commandes — Java 21, Spring Boot 4.1, Spring Data JPA, PostgreSQL, Flyway.

Les règles métier de référence sont décrites dans [`docs/business-rules.md`](docs/business-rules.md).
Les conventions du projet sont décrites dans [`AGENTS.md`](AGENTS.md).

## Architecture

Organisation par fonctionnalité (`client`, `product`, `order`, `common`), avec le flux :

```
Controller -> Service -> Repository
```

- Les contrôleurs ne manipulent que des DTO (`record`) validés par Bean Validation.
- Les services portent la logique métier et les transactions (`@Transactional`).
- Les entités JPA ne sont jamais exposées par l'API.
- Les montants sont des `BigDecimal` (colonnes `NUMERIC(19,2)`).
- Le schéma est géré par Flyway (`src/main/resources/db/migration`), Hibernate ne fait que le valider.

## Prérequis

- JDK 21
- Docker (base PostgreSQL locale et tests d'intégration Testcontainers)

## Lancer l'application

```bash
docker compose up -d          # PostgreSQL 17 sur localhost:5432
./mvnw spring-boot:run        # API sur http://localhost:8080
```

Connexion configurable via `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

## Endpoints

| Méthode | URL                         | Description                                   |
|---------|-----------------------------|-----------------------------------------------|
| POST    | `/api/clients`              | Créer un client (email unique)                |
| GET     | `/api/clients`              | Lister les clients                            |
| GET     | `/api/clients/{id}`         | Consulter un client                           |
| POST    | `/api/products`             | Créer un produit                              |
| PATCH   | `/api/products/{id}/status` | Activer / désactiver un produit               |
| GET     | `/api/products`             | Lister les produits                           |
| GET     | `/api/products/{id}`        | Consulter un produit                          |
| POST    | `/api/orders`               | Créer une commande (statut initial `DRAFT`)   |
| PATCH   | `/api/orders/{id}/status`   | Changer le statut d'une commande              |
| GET     | `/api/orders`               | Lister les commandes                          |
| GET     | `/api/orders/{id}`          | Consulter une commande avec lignes et totaux  |

Exemple :

```bash
curl -X POST localhost:8080/api/orders -H 'Content-Type: application/json' \
  -d '{"clientId": 1, "lines": [{"productId": 1, "quantity": 2}]}'

curl -X PATCH localhost:8080/api/orders/1/status -H 'Content-Type: application/json' \
  -d '{"status": "CONFIRMED"}'
```

### Erreurs (format `ProblemDetail`, RFC 9457)

| Statut | Cas                                                                   |
|--------|-----------------------------------------------------------------------|
| 400    | Corps de requête invalide (champ manquant, aucune ligne, quantité ≤ 0, prix ≤ 0) |
| 404    | Client, produit ou commande introuvable                               |
| 409    | Email ou référence produit déjà utilisés, transition de statut interdite, modification concurrente |
| 422    | Produit inactif ajouté à une commande                                 |

Les emails sont enregistrés en minuscules (unicité insensible à la casse). Les lignes d'une commande
portant sur un même produit sont fusionnées (quantités additionnées).

## Tests

```bash
./mvnw test      # tests unitaires et tests web (@WebMvcTest), sans Docker
./mvnw verify    # + tests d'intégration *IT sur PostgreSQL via Testcontainers (Docker requis)
```
