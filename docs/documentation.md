# Documentation — spring-ai-lab

API REST de gestion de commandes : clients, catalogue de produits et commandes avec cycle de vie.

- **Stack** : Java 21, Spring Boot 4.1, Spring Web MVC, Spring Data JPA, Bean Validation, PostgreSQL 17, Flyway, springdoc-openapi (Swagger UI).
- **Règles métier de référence** : [`business-rules.md`](business-rules.md).
- **Conventions de développement** : [`../AGENTS.md`](../AGENTS.md).

---

## Sommaire

1. [Démarrage rapide](#1-démarrage-rapide)
2. [Configuration](#2-configuration)
3. [Architecture](#3-architecture)
4. [Modèle de données](#4-modèle-de-données)
5. [Règles métier](#5-règles-métier)
6. [Référence de l'API](#6-référence-de-lapi)
7. [Gestion des erreurs](#7-gestion-des-erreurs)
8. [Tests](#8-tests)
9. [Outils de test manuel de l'API](#9-outils-de-test-manuel-de-lapi)

---

## 1. Démarrage rapide

### Prérequis

- JDK 21
- Docker (base PostgreSQL locale et tests d'intégration Testcontainers)

### Lancement

```bash
docker compose up -d          # PostgreSQL 17 sur localhost:5432
./mvnw spring-boot:run        # API sur http://localhost:8080
```

Au démarrage, Flyway applique les migrations de `src/main/resources/db/migration`, puis Hibernate valide le schéma (`ddl-auto: validate`).

### Points d'accès utiles

| URL                                     | Contenu                                  |
|-----------------------------------------|------------------------------------------|
| `http://localhost:8080/swagger-ui.html` | Interface Swagger UI                     |
| `http://localhost:8080/v3/api-docs`     | Spécification OpenAPI (JSON)             |
| `http://localhost:8080/actuator/health` | État de santé de l'application (Actuator) |

---

## 2. Configuration

Fichier : `src/main/resources/application.yaml`.

| Variable d'environnement | Valeur par défaut                                   |
|--------------------------|-----------------------------------------------------|
| `DB_URL`                 | `jdbc:postgresql://localhost:5432/spring_ai_lab`    |
| `DB_USERNAME`            | `spring_ai_lab`                                     |
| `DB_PASSWORD`            | `spring_ai_lab`                                     |

Autres réglages notables :

- `spring.jpa.open-in-view: false` — aucune session JPA n'est ouverte dans la couche web ; les données nécessaires sont chargées dans les services.
- `spring.jpa.hibernate.ddl-auto: validate` — le schéma est exclusivement géré par Flyway.

Le fichier `docker-compose.yml` démarre un conteneur `postgres:17-alpine` (base, utilisateur et mot de passe `spring_ai_lab`) avec un volume persistant `postgres-data` et un healthcheck `pg_isready`.

---

## 3. Architecture

### Flux de dépendances

```
Controller  ->  Service  ->  Repository
```

- Les **contrôleurs** reçoivent et renvoient uniquement des DTO (`record`), validés par Bean Validation (`@Valid`). Ils n'accèdent jamais aux repositories.
- Les **services** portent la logique métier et les frontières transactionnelles : `@Transactional(readOnly = true)` au niveau de la classe, `@Transactional` sur les méthodes d'écriture.
- Les **repositories** sont des interfaces Spring Data JPA.
- Les **entités JPA** ne sont jamais exposées par l'API : chaque `*Response` possède une méthode `from(entité)`.

### Organisation des paquets (par fonctionnalité)

```
dev.crochard.spring_ai_lab
├── SpringAiLabApplication.java
├── client/     Client, ClientController, ClientService, ClientRepository,
│               CreateClientRequest, ClientResponse
├── product/    Product, ProductController, ProductService, ProductRepository,
│               CreateProductRequest, UpdateProductStatusRequest, ProductResponse
├── order/      Order, OrderLine, OrderStatus, OrderController, OrderService, OrderRepository,
│               CreateOrderRequest, UpdateOrderStatusRequest, OrderResponse
└── common/     GlobalExceptionHandler, NotFoundException, ConflictException,
                BusinessRuleViolationException, OpenApiConfiguration
```

### Choix techniques

- **Montants** : `BigDecimal` partout (colonnes `NUMERIC(19,2)`), jamais `double` ni `float`.
- **Verrouillage optimiste** : l'entité `Order` porte un champ `@Version`, ce qui empêche deux changements de statut concurrents de contourner les règles de transition.
- **Chargement des commandes** : `OrderRepository.findById` et `findAll` utilisent un `@EntityGraph` (`lines`, `lines.product`) pour charger commande, lignes et produits en une seule requête.
- **Erreurs** : format Problem Details (RFC 9457) via `GlobalExceptionHandler`.

---

## 4. Modèle de données

Migration : `V1__create_clients_products_orders.sql`.

```
clients 1 ──── * orders 1 ──── * order_lines * ──── 1 products
```

| Table         | Colonnes                                                        | Contraintes                                                                 |
|---------------|-----------------------------------------------------------------|------------------------------------------------------------------------------|
| `clients`     | `id`, `first_name`, `last_name`, `email`                        | `email` unique, `email = LOWER(email)`                                       |
| `products`    | `id`, `reference`, `name`, `price NUMERIC(19,2)`, `active`      | `reference` unique, `price > 0`                                              |
| `orders`      | `id`, `client_id`, `status`, `version`                          | FK vers `clients`, `status IN ('DRAFT','CONFIRMED','CANCELLED')`            |
| `order_lines` | `id`, `order_id`, `product_id`, `quantity`, `unit_price NUMERIC(19,2)` | FK vers `orders` et `products`, `quantity > 0`                       |

Index : `idx_orders_client_id`, `idx_order_lines_order_id`.

Les règles critiques sont donc doublées en base (unicité, prix positif, quantité positive, statuts valides, email en minuscules).

---

## 5. Règles métier

Résumé de [`business-rules.md`](business-rules.md) et de son implémentation.

### Client

- Prénom, nom et email obligatoires.
- L'email est **unique sans tenir compte de la casse** : il est converti en minuscules avant enregistrement (`ClientService.create`).

### Produit

- Référence (unique), nom, prix et statut actif/inactif.
- Prix **strictement positif**, 2 décimales maximum.
- Un produit **inactif ne peut pas être ajouté à une nouvelle commande**.
- Un produit peut être activé ou désactivé à tout moment (`PATCH /api/products/{id}/status`).

### Commande

- Appartient à **exactement un client** existant.
- Contient **au moins une ligne**, chaque quantité étant **strictement positive**.
- Les lignes portant sur un **même produit sont fusionnées** (quantités additionnées, ordre de première apparition conservé). Si la somme dépasse `Integer.MAX_VALUE`, la commande est refusée.
- Chaque ligne **mémorise le prix du produit au moment de la commande** (`unitPrice`) : une modification ultérieure du prix du produit n'affecte pas les commandes existantes.
- Statut initial : `DRAFT`.

### Calculs

```
Total ligne    = quantité × prix unitaire
Total commande = somme des totaux de lignes
```

### Cycle de vie d'une commande

```
           ┌────────────► CONFIRMED ─────┐
  DRAFT ───┤                              ├──► CANCELLED
           └──────────────────────────────┘
```

| Depuis \ Vers | `DRAFT` | `CONFIRMED` | `CANCELLED` |
|---------------|:-------:|:-----------:|:-----------:|
| `DRAFT`       |    ✗    |      ✓      |      ✓      |
| `CONFIRMED`   |    ✗    |      ✗      |      ✓      |
| `CANCELLED`   |    ✗    |      ✗      |      ✗      |

Toute autre transition est refusée (`409 Conflict`). La règle est portée par `OrderStatus.canTransitionTo`.

---

## 6. Référence de l'API

Toutes les requêtes et réponses sont en JSON. Les créations renvoient `201 Created` avec un en-tête `Location`.

### 6.1 Clients — `/api/clients`

| Méthode | URL                 | Description         | Succès | Erreurs        |
|---------|---------------------|---------------------|--------|----------------|
| POST    | `/api/clients`      | Créer un client     | 201    | 400, 409       |
| GET     | `/api/clients`      | Lister les clients  | 200    | —              |
| GET     | `/api/clients/{id}` | Consulter un client | 200    | 404            |

**Requête de création** (`CreateClientRequest`)

| Champ       | Type   | Contraintes                          |
|-------------|--------|--------------------------------------|
| `firstName` | string | obligatoire, ≤ 255 caractères        |
| `lastName`  | string | obligatoire, ≤ 255 caractères        |
| `email`     | string | obligatoire, email valide, ≤ 255 caractères |

```json
{ "firstName": "Jean", "lastName": "Dupont", "email": "Jean.Dupont@Example.com" }
```

**Réponse** (`ClientResponse`)

```json
{ "id": 1, "firstName": "Jean", "lastName": "Dupont", "email": "jean.dupont@example.com" }
```

### 6.2 Produits — `/api/products`

| Méthode | URL                         | Description                          | Succès | Erreurs   |
|---------|-----------------------------|--------------------------------------|--------|-----------|
| POST    | `/api/products`             | Créer un produit                     | 201    | 400, 409  |
| PATCH   | `/api/products/{id}/status` | Activer / désactiver un produit      | 200    | 400, 404  |
| GET     | `/api/products`             | Lister les produits (actifs et inactifs) | 200 | —         |
| GET     | `/api/products/{id}`        | Consulter un produit                 | 200    | 404       |

**Requête de création** (`CreateProductRequest`)

| Champ       | Type    | Contraintes                                          |
|-------------|---------|------------------------------------------------------|
| `reference` | string  | obligatoire, ≤ 255 caractères, unique                |
| `name`      | string  | obligatoire, ≤ 255 caractères                        |
| `price`     | decimal | obligatoire, > 0, 17 chiffres entiers et 2 décimales max |
| `active`    | boolean | obligatoire                                          |

```json
{ "reference": "KB-001", "name": "Clavier", "price": 49.90, "active": true }
```

**Requête de changement de statut** (`UpdateProductStatusRequest`)

```json
{ "active": false }
```

**Réponse** (`ProductResponse`)

```json
{ "id": 1, "reference": "KB-001", "name": "Clavier", "price": 49.90, "active": true }
```

### 6.3 Commandes — `/api/orders`

| Méthode | URL                       | Description                        | Succès | Erreurs            |
|---------|---------------------------|------------------------------------|--------|--------------------|
| POST    | `/api/orders`             | Créer une commande (statut `DRAFT`) | 201   | 400, 404, 422      |
| PATCH   | `/api/orders/{id}/status` | Changer le statut d'une commande   | 200    | 400, 404, 409      |
| GET     | `/api/orders`             | Lister les commandes               | 200    | —                  |
| GET     | `/api/orders/{id}`        | Consulter une commande             | 200    | 404                |

**Requête de création** (`CreateOrderRequest`)

| Champ                | Type    | Contraintes                     |
|----------------------|---------|---------------------------------|
| `clientId`           | long    | obligatoire                     |
| `lines`              | array   | obligatoire, au moins une ligne |
| `lines[].productId`  | long    | obligatoire                     |
| `lines[].quantity`   | integer | obligatoire, > 0                |

```json
{
  "clientId": 1,
  "lines": [
    { "productId": 1, "quantity": 2 },
    { "productId": 2, "quantity": 1 },
    { "productId": 1, "quantity": 3 }
  ]
}
```

**Requête de changement de statut** (`UpdateOrderStatusRequest`)

```json
{ "status": "CONFIRMED" }
```

Valeurs possibles : `DRAFT`, `CONFIRMED`, `CANCELLED`.

**Réponse** (`OrderResponse`) — ici après fusion des deux lignes du produit 1 :

```json
{
  "id": 1,
  "clientId": 1,
  "status": "DRAFT",
  "lines": [
    { "id": 1, "productId": 1, "productReference": "KB-001", "productName": "Clavier",
      "quantity": 5, "unitPrice": 49.90, "lineTotal": 249.50 },
    { "id": 2, "productId": 2, "productReference": "MS-001", "productName": "Souris",
      "quantity": 1, "unitPrice": 19.90, "lineTotal": 19.90 }
  ],
  "total": 269.40
}
```

### Exemples `curl`

```bash
curl -X POST localhost:8080/api/clients -H 'Content-Type: application/json' \
  -d '{"firstName": "Jean", "lastName": "Dupont", "email": "jean.dupont@example.com"}'

curl -X POST localhost:8080/api/products -H 'Content-Type: application/json' \
  -d '{"reference": "KB-001", "name": "Clavier", "price": 49.90, "active": true}'

curl -X POST localhost:8080/api/orders -H 'Content-Type: application/json' \
  -d '{"clientId": 1, "lines": [{"productId": 1, "quantity": 2}]}'

curl -X PATCH localhost:8080/api/orders/1/status -H 'Content-Type: application/json' \
  -d '{"status": "CONFIRMED"}'
```

---

## 7. Gestion des erreurs

Toutes les erreurs sont renvoyées au format **Problem Details** (RFC 9457), `Content-Type: application/problem+json`.

| Statut | Origine                                   | Cas                                                                                  |
|--------|-------------------------------------------|--------------------------------------------------------------------------------------|
| 400    | Bean Validation / désérialisation         | Champ manquant ou invalide, aucune ligne, quantité ≤ 0, prix ≤ 0 ou > 2 décimales, statut inconnu |
| 404    | `NotFoundException`                       | Client, produit ou commande introuvable                                              |
| 409    | `ConflictException`                       | Email ou référence déjà utilisés, transition de statut interdite                     |
| 409    | `OptimisticLockingFailureException`       | Modification concurrente d'une commande                                              |
| 409    | `DataIntegrityViolationException`         | Violation d'une contrainte en base (filet de sécurité, détail technique non exposé)  |
| 422    | `BusinessRuleViolationException`          | Produit inactif dans une commande, quantité cumulée d'un produit trop grande         |

Pour les erreurs de validation, la propriété `errors` associe chaque champ invalide à son premier message :

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Requête invalide",
  "instance": "/api/products",
  "errors": { "price": "doit être supérieur à 0" }
}
```

---

## 8. Tests

```bash
./mvnw test      # tests unitaires et tests web (@WebMvcTest), sans Docker
./mvnw verify    # + tests d'intégration *IT sur PostgreSQL via Testcontainers (Docker requis)
```

Outils : JUnit 5, Mockito, AssertJ, Spring Boot Test, Testcontainers (PostgreSQL). Les tests `*IT` sont exécutés par le plugin `maven-failsafe-plugin`.

| Classe                         | Type                     | Tests | Portée                                                                         |
|--------------------------------|--------------------------|:-----:|--------------------------------------------------------------------------------|
| `ClientServiceTest`            | Unitaire (Mockito)       | 7     | Création, unicité et mise en minuscules de l'email, lectures                   |
| `ProductServiceTest`           | Unitaire (Mockito)       | 10    | Création, unicité de la référence, prix, (dés)activation, lectures             |
| `OrderServiceTest`             | Unitaire (Mockito)       | 24    | Création, quantités invalides, fusion des lignes, produit inactif ou inexistant, toutes les transitions |
| `OrderStatusTest`              | Unitaire                 | 10    | Matrice des transitions de statut                                              |
| `OrderTest`                    | Unitaire                 | 4     | Calculs du total de ligne et du total de commande                              |
| `ClientControllerTest`         | Web (`@WebMvcTest`)      | 7     | Validation, codes HTTP et lectures de `/api/clients`                           |
| `ProductControllerTest`        | Web (`@WebMvcTest`)      | 10    | Validation, codes HTTP et lectures de `/api/products`                          |
| `OrderControllerTest`          | Web (`@WebMvcTest`)      | 19    | Validation, codes HTTP, format `application/problem+json` de `/api/orders`     |
| `SpringAiLabApplicationIT`     | Intégration              | 1     | Démarrage du contexte complet sur PostgreSQL                                   |
| `OrderApiIT`                   | Intégration              | 17    | Scénarios de bout en bout sur l'API, contenu de la base et contraintes PostgreSQL |

Total : 91 tests unitaires et web, 18 tests d'intégration. Les tests paramétrés comptent une exécution par jeu de données.

`TestcontainersConfiguration` fournit le conteneur PostgreSQL partagé par les tests d'intégration.

### Couverture de code

La couverture est mesurée par le plugin `jacoco-maven-plugin` (version définie par la propriété `jacoco.version` du `pom.xml`). Aucun seuil n'est imposé : les rapports servent uniquement à observer la couverture.

| Commande         | Rapport HTML généré                                                   |
|------------------|-----------------------------------------------------------------------|
| `./mvnw test`    | `target/site/jacoco/index.html` — tests unitaires et web              |
| `./mvnw verify`  | `target/site/jacoco/index.html` — tests unitaires et web              |
|                  | `target/site/jacoco-it/index.html` — tests d'intégration              |
|                  | `target/site/jacoco-merged/index.html` — fusion des deux              |

Les données brutes sont écrites dans `target/jacoco.exec`, `target/jacoco-it.exec` et `target/jacoco-merged.exec`. Un fichier `jacoco.csv` et un fichier `jacoco.xml` accompagnent chaque rapport HTML.

Sont exclues du calcul : `SpringAiLabApplication` (point d'entrée) et `OpenApiConfiguration` (métadonnées OpenAPI).

#### Couverture mesurée (état au 13/10/2026)

Mesures obtenues avec `./mvnw clean verify`, exprimées en lignes / branches.

| Paquet    | Unitaires       | Intégration     | Fusionné           |
|-----------|-----------------|-----------------|--------------------|
| `client`  | 87,9 % / 100 %  | 81,8 % / 100 %  | **100 % / 100 %**  |
| `common`  | 100 % / —       | 55,6 % / —      | **100 % / —**      |
| `order`   | 91,4 % / 100 %  | 94,6 % / 78,9 % | **100 % / 100 %**  |
| `product` | 93,2 % / 100 %  | 88,6 % / 75 %   | **100 % / 100 %**  |
| **Total** | 92 % / 100 %    | 87,2 % / 80 %   | **100 % (188/188) / 100 % (25/25)** |

Avant l'ajout des tests manquants, la couverture fusionnée était de 96,3 % des lignes (181/188) et 92 % des branches (23/25).

### Couverture fonctionnelle des commandes (état au 13/10/2026)

Une couverture de lignes élevée ne garantit pas que chaque scénario métier soit testé au bon niveau. Le tableau ci-dessous fait correspondre chaque scénario attendu aux tests qui le vérifient.

| Cas de test            | Type        | Tests                                                                                              |
|------------------------|-------------|----------------------------------------------------------------------------------------------------|
| Client inexistant      | Unit        | `OrderServiceTest.rejectsUnknownClient`                                                            |
| Produit inexistant     | Unit        | `OrderServiceTest.rejectsUnknownProduct` (vérifie aussi que rien n'est enregistré)                 |
| Produit inactif        | Unit        | `OrderServiceTest.rejectsInactiveProduct`, `rejectsInactiveProductAmongActiveOnes`                 |
| Quantité = 0           | Unit        | `OrderServiceTest.rejectsNonPositiveQuantity`                                                      |
| Quantité négative      | Unit        | `OrderServiceTest.rejectsNegativeQuantity`                                                         |
| Quantité absente       | Unit        | `OrderServiceTest.rejectsMissingQuantity`                                                          |
| Aucune ligne           | Unit        | `OrderServiceTest.rejectsOrderWithoutLines`, `rejectsMissingLines`                                 |
| Une ligne              | Unit        | `OrderServiceTest.acceptsQuantityOfOne`, `memorizesProductPriceAtOrderTime`                        |
| Plusieurs lignes       | Unit        | `OrderServiceTest.createsDraftOrderWithLineAndOrderTotals`, `mergesLinesForTheSameProductBySummingQuantities` |
| Calcul total           | Unit        | `OrderTest` (total de ligne, total de commande, commande vide, exactitude `BigDecimal`)            |
| Annulation             | Unit        | `OrderServiceTest.cancelsOrder` (`DRAFT → CANCELLED`, `CONFIRMED → CANCELLED`)                     |
| Double annulation      | Unit        | `OrderServiceTest.rejectsDoubleCancellation` (409, statut inchangé)                                |
| Confirmation           | Unit        | `OrderServiceTest.appliesAllowedTransition`, `rejectsDoubleConfirmation`                           |
| Autres transitions interdites | Unit | `OrderServiceTest.rejectsForbiddenTransitionAndKeepsStatus`, `rejectsOtherForbiddenTransitionsAndKeepsStatus` ; `OrderStatusTest` |
| Création REST complète | Integration | `OrderApiIT.createsOrderWithTotalsAndMemorizedPrices`, `createsOrderWithLocationAndPersistsLinesWithMemorizedPrice` (en-tête `Location`, tables `orders` et `order_lines`) |
| Erreurs de bout en bout | Integration | `OrderApiIT.returns404ForUnknownClientOnOrderCreation`, `returns404ForUnknownProductAndPersistsNothing`, `rejectsOrderContainingAnInactiveProductAndPersistsNothing`, `rejectsDoubleCancellation`, `returns404ForUnknownOrder` |
| PostgreSQL réel        | Integration | `OrderApiIT` + `TestcontainersConfiguration` (`postgres:17-alpine`, Flyway) ; `databaseConstraintsRejectInvalidData` (contraintes CHECK de prix, email, quantité et statut) |
| Validation HTTP        | API         | `OrderControllerTest` : sans ligne, champ `lines` absent, ligne `null`, ligne sans `productId`, quantité 0/-1/null, `clientId` absent, JSON mal formé, statut inconnu ou absent |
| Erreurs HTTP           | API         | `OrderControllerTest` : 404 à la création et à la lecture, 409, 422, format `application/problem+json` |

Les lectures `GET /api/clients`, `GET /api/clients/{id}`, `GET /api/products` et `GET /api/products/{id}` sont couvertes par `ClientControllerTest`, `ClientServiceTest`, `ProductControllerTest` et `ProductServiceTest`.

---

## 9. Outils de test manuel de l'API

L'application doit être démarrée (voir [Démarrage rapide](#1-démarrage-rapide)).

### Client HTTP IntelliJ — `rest.http`

Scénarios à exécuter de haut en bas : les commandes réutilisent le client et les produits créés plus haut. Chaque requête comporte des assertions (`client.test`).

### Collection Bruno — `bruno-api/`

Mêmes scénarios, organisés en trois dossiers exécutés dans l'ordre `clients` → `products` → `orders`. Emails et références sont suffixés d'un horodatage : la collection est rejouable sans vider la base.

```bash
cd bruno-api
npx @usebruno/cli run -r --env local
```

L'URL de l'API se règle dans `bruno-api/environments/local.bru` (`baseUrl`, par défaut `http://localhost:8080`). Détails dans [`bruno-api/README.md`](../bruno-api/README.md).
