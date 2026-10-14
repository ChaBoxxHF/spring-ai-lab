# Analyse de couverture : spring-ai-lab (branche `avec-claude`)

*Rapport du 13/10/2026, mis à jour le même jour après l'ajout des tests 🟠 (voir « Suivi des corrections »).*

> **Écarts entre le prompt et le dépôt.** Le contexte fourni décrit un autre projet. Ce dépôt ne contient ni `account`, ni `category`, ni `web/admin`, ni `web/shop`, ni Thymeleaf, ni `CartService`, ni `SecurityConfig`. `docs/REGLE_METIER.md` et `docs/api.md` n'existent pas non plus. Les noms de tests cités (`CustomerServiceTest`, `OrderWorkflowIntegrationTest`…) ne correspondent pas aux tests présents. L'analyse porte donc sur le code réel : les domaines `client`, `product`, `order` et `common`, la référence étant `docs/business-rules.md`. Aucun fichier n'a été modifié, puisque JaCoCo était déjà configuré.

**Exécution :** `./mvnw clean verify` → **BUILD SUCCESS**. Il passe 93 tests unitaires et 23 tests d'intégration (Testcontainers PostgreSQL 17), tous verts. Avant correction, il y en avait 91 et 18.
`SpringAiLabApplication` et `OpenApiConfiguration` sont exclus de la mesure par le `pom.xml`.

## 1. Synthèse

| Indicateur | Unitaires seuls | Intégration seule | **Fusionné** |
| --- | --- | --- | --- |
| Couverture lignes | 173/188 = 92,0 % | 169/188 = 89,9 % | **188/188 = 100 %** |
| Couverture branches | 25/25 = 100 % | 20/25 = 80,0 % | **25/25 = 100 %** |
| Couverture méthodes | 84/94 = 89,4 % | 82/94 = 87,2 % | **94/94 = 100 %** |
| Classes analysées | 25 | 25 | 25 |
| Classes à 0 % | 0 | 0 | 0 |
| Statut | mesuré | mesuré | **mesuré** |

La couverture de code atteint son maximum. Les manques réels se trouvent donc dans les **scénarios** (section 5), et non dans les lignes non exécutées.

## 2. Couverture par domaine fonctionnel (mesuré, rapport fusionné)

| Domaine | Classes | Lignes couvertes / Total | % Lignes | % Branches | Unit. seuls (lignes) | Statut |
| --- | --- | --- | --- | --- | --- | --- |
| account / category / web admin / web shop | 0 | — | — | — | — | **inexistant dans le dépôt** |
| client | 5 | 33/33 | 100 % | 100 % (2/2) | 29/33 (87,9 %) | mesuré |
| product | 6 | 44/44 | 100 % | 100 % (4/4) | 41/44 (93,2 %) | mesuré |
| order | 10 | 93/93 | 100 % | 100 % (19/19) | 85/93 (91,4 %) | mesuré |
| common (exceptions + handler) | 4 | 18/18 | 100 % | — (0 branche) | 18/18 (100 %) | mesuré |
| **TOTAL** | **25** | **188/188** | **100 %** | **100 % (25/25)** | 173/188 (92,0 %) | mesuré |

Contrôle : 33 + 44 + 93 + 18 = 188 lignes et 2 + 4 + 19 = 25 branches. Les totaux concordent.

## 3. Couverture par couche (mesuré)

| Couche | Lignes | % Lignes | % Branches | Commentaire |
| --- | --- | --- | --- | --- |
| Controllers (3) | 23 | 100 % | — | En unitaire seul, 18/23 (78 %). `ClientController.create` (succès), `ProductController.updateStatus` (succès), `OrderController.changeStatus` (succès) et `findAll` ne sont exécutés que par les tests d'intégration. |
| Services (3) | 67 | 100 % | 100 % (20/20) | En unitaire, 65/67 : `OrderService.findById` et `findAll` n'ont pas de test Mockito. |
| Repositories (3) | — | non mesurable | — | Ce sont des interfaces Spring Data sans bytecode propre, testées fonctionnellement par `OrderApiIT`. |
| Entités + enum (5) | 63 | 100 % | 100 % (5/5) | Les constructeurs `protected` réservés à JPA ne sont couverts que par les tests d'intégration. |
| DTO / records (10) | 17 | 100 % | — | — |
| Exceptions / handler (4) | 18 | 100 % | — | En intégration seule, `GlobalExceptionHandler` atteint 6/12 lignes (4/12 avant correction). |
| Sécurité / Config | — | — | — | Pas de sécurité dans le code ; `OpenApiConfiguration` est exclue de la mesure. |
| **TOTAL** | **188** | **100 %** | **100 %** | 23 + 67 + 63 + 17 + 18 = 188 |

## 4. Fonctionnalités insuffisamment couvertes

Les pourcentages de cette section sont des **taux de couverture de scénario**, estimés à partir des tests lus. Les lignes, elles, sont toutes exécutées.

| # | Fonctionnalité | Classe(s) | % actuel | % cible | Criticité | Règle métier concernée | Scénario manquant |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | ~~Réactivation d'un produit, puis commande de ce produit~~ | `ProductService`, `OrderService` | ✅ 100 % (0 % avant) | 100 % | 🟠 | « Produit inactif non commandable » (sa réciproque) | Traité : `ProductServiceTest.activatesProduct`, `OrderApiIT.reactivatedProductCanBeOrderedAgain` |
| 2 | ~~Doublon de référence produit en concurrence~~ | `GlobalExceptionHandler`, `uk_products_reference` | ✅ 100 % (50 % avant) | 100 % | 🟠 | Référence unique (R3) | Traité : `ProductControllerTest.mapsConcurrentDuplicateReferenceTo409WithoutLeakingSqlDetails`, `ConcurrentAccessIT.duplicateReferenceMissedByPreCheckIsRejectedByDatabaseWith409` |
| 3 | ~~Doublon d'email réellement levé par la base~~ | `uk_clients_email` | ✅ 100 % (50 % avant) | 100 % | 🟠 | Email unique (R4) | Traité : `ConcurrentAccessIT.duplicateEmailMissedByPreCheckIsRejectedByDatabaseWith409` |
| 4 | ~~Changement de statut concurrent, de bout en bout~~ | `OrderService`, `Order@Version` | ✅ 100 % (75 % avant) | 100 % | 🟠 | Transitions de statut | Traité : `ConcurrentAccessIT.concurrentStatusChangeThroughApiIsRejectedWith409` |
| 5 | ~~Débordement de quantité fusionnée via HTTP~~ | `OrderService.mergeQuantitiesByProduct` | ✅ 100 % (75 % avant) | 100 % | 🟠 | Fusion des lignes (R5) | Traité : `OrderApiIT.rejectsMergedQuantityOverflowAndPersistsNothing` |
| 6 | Bornes `@Size(max = 255)` | `CreateClientRequest`, `CreateProductRequest` | 0 % | 100 % | 🟡 | Validation | 256 caractères → 400 |
| 7 | Nom de produit vide | `CreateProductRequest` | 75 % (estimé) | 100 % | 🟡 | Validation | `name: ""` → 400 n'est pas vérifié |
| 8 | En-tête `Location` d'un client créé | `ClientController` | 0 % | 100 % | 🔵 | Contrat API | `Location: /api/clients/{id}` n'est jamais vérifié |
| 9 | Produit créé directement inactif | `ProductService` | 0 % | 100 % | 🔵 | — | `createProduct(…, false)` n'est jamais appelé |
| 10 | Tests unitaires de lecture des commandes | `OrderService.findById` / `findAll` | 0 % en unitaire (100 % en IT) | ≥ 80 % en unitaire | 🔵 | — | Tests Mockito |
| 11 | Documentation OpenAPI exposée | `OpenApiConfiguration` | 0 % (exclue) | smoke test | 🔵 | — | `/v3/api-docs` → 200 |

## 5. Tableau complet des scénarios

Barème : 100 % = au moins un test avec assertion significative sur le comportement attendu. 75 % = comportement vérifié, mais pas à toutes les couches exposées. 50 % = seul le mécanisme générique ou le mapping simulé est testé. 0 % = aucun test.

| Scénario | Variante | Règle | Test(s) existant(s) | Couvert ? | % | Criticité | Manque |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Création client | nominale | — | ClientServiceTest.createsClient, OrderApiIT | ✅ | 100 % | 🟡 | — |
| Création client | email en majuscules → stocké en minuscules | R4 | ClientServiceTest.storesEmailInLowerCase, OrderApiIT | ✅ | 100 % | 🔴 | — |
| Création client | email déjà utilisé → 409 | Email unique | ClientServiceTest, ClientControllerTest, OrderApiIT.rejectsDuplicateClientEmail | ✅ | 100 % | 🔴 | — |
| Création client | doublon avec une casse différente → 409 | R4 | ClientServiceTest.rejectsDuplicateEmailIgnoringCase, OrderApiIT | ✅ | 100 % | 🔴 | — |
| Création client | doublon concurrent (contrainte base) → 409 sans détail SQL | Email unique | ClientControllerTest (simulé), ConcurrentAccessIT.duplicateEmailMissedByPreCheck… | ✅ | 100 % | 🟠 | — |
| Création client | email invalide → 400 | Validation | ClientControllerTest.rejectsInvalidEmail | ✅ | 100 % | 🟡 | — |
| Création client | prénom ou nom vide → 400 | Validation | ClientControllerTest.rejectsBlankNames | ✅ | 100 % | 🟡 | — |
| Création client | champ > 255 caractères → 400 | Validation | — | ❌ | 0 % | 🟡 | Test de borne |
| Création client | 201 + `Location` | Contrat API | — | ❌ | 0 % | 🔵 | Test `@WebMvcTest` |
| Lecture client | existant / inexistant → 404 | — | ClientServiceTest, ClientControllerTest | ✅ | 100 % | 🟡 | — |
| Lecture client | id inexistant → 404 | — | idem | ✅ | 100 % | 🟠 | — |
| Liste clients | — | — | ClientServiceTest, ClientControllerTest | ✅ | 100 % | 🔵 | — |
| Création produit | nominale, 201 + `Location` | — | ProductServiceTest, ProductControllerTest | ✅ | 100 % | 🟡 | — |
| Création produit | prix 0 ou négatif → 400 (et 422 côté service) | R2 | ProductControllerTest, ProductServiceTest, contrainte CHECK en IT | ✅ | 100 % | 🔴 | — |
| Création produit | prix à 3 décimales → 400 | Validation | ProductControllerTest | ✅ | 100 % | 🟡 | — |
| Création produit | référence existante → 409 | R3 | ProductServiceTest, OrderApiIT | ✅ | 100 % | 🔴 | — |
| Création produit | doublon concurrent → 409 | R3 | ProductControllerTest.mapsConcurrentDuplicateReference…, ConcurrentAccessIT.duplicateReferenceMissedByPreCheck… | ✅ | 100 % | 🟠 | — |
| Création produit | champs absents ou vides → 400 | Validation | ProductControllerTest.rejectsMissingOrBlankFields (`name` non couvert) | ⚠️ | 75 % | 🟡 | `name` vide |
| Statut produit | désactivation | — | ProductServiceTest.deactivatesProduct, OrderApiIT | ✅ | 100 % | 🟡 | — |
| Statut produit | réactivation | — | ProductServiceTest.activatesProduct, OrderApiIT.reactivatedProductCanBeOrderedAgain | ✅ | 100 % | 🟠 | Règle à confirmer (voir « Points à clarifier ») |
| Statut produit | valeur absente → 400 | Validation | ProductControllerTest | ✅ | 100 % | 🟡 | — |
| Produit | inexistant (lecture / statut) → 404 | — | ProductServiceTest, ProductControllerTest | ✅ | 100 % | 🟠 | — |
| Liste produits | actifs + inactifs | — | ProductServiceTest, ProductControllerTest | ✅ | 100 % | 🔵 | — |
| Création produit | `active: false` dès la création | — | — | ❌ | 0 % | 🔵 | Test de création |
| Création commande | nominale, statut DRAFT | R1 | OrderServiceTest, OrderControllerTest, OrderApiIT | ✅ | 100 % | 🔴 | — |
| Création commande | DRAFT et client persistés en base | R1 | OrderApiIT.createsOrderWithLocation… | ✅ | 100 % | 🔴 | — |
| Calcul | total ligne = quantité × prix unitaire | Calcul | OrderTest, OrderServiceTest, OrderApiIT | ✅ | 100 % | 🔴 | — |
| Calcul | total commande = somme des lignes | Calcul | OrderTest, OrderServiceTest, OrderApiIT | ✅ | 100 % | 🔴 | — |
| Calcul | exactitude BigDecimal (3 × 0,10 = 0,30) | BigDecimal | OrderTest.totalIsExactWithoutFloatingPointErrors | ✅ | 100 % | 🔴 | — |
| Prix figé | changement du prix catalogue après la commande | Prix mémorisé | OrderServiceTest.memorizesProductPriceAtOrderTime, OrderApiIT | ✅ | 100 % | 🔴 | — |
| Prix figé | désactivation du produit après la commande | — | OrderApiIT.deactivatingProductDoesNotAffectExistingOrders | ✅ | 100 % | 🟡 | — |
| Produit inactif | seul dans la commande → 422 | Inactif interdit | OrderServiceTest.rejectsInactiveProduct, OrderControllerTest | ✅ | 100 % | 🔴 | — |
| Produit inactif | parmi des actifs → 422, rien de persisté | Inactif interdit | OrderServiceTest, OrderApiIT | ✅ | 100 % | 🔴 | — |
| Produit réactivé | de nouveau commandable | Inactif interdit (réciproque) | OrderApiIT.reactivatedProductCanBeOrderedAgain | ✅ | 100 % | 🟠 | — |
| Lignes | liste vide → 400 (et 422 côté service) | ≥ 1 ligne | OrderControllerTest, OrderServiceTest | ✅ | 100 % | 🔴 | — |
| Lignes | champ `lines` absent ou null | ≥ 1 ligne | OrderControllerTest, OrderServiceTest.rejectsMissingLines | ✅ | 100 % | 🔴 | — |
| Lignes | ligne `null` → 400 | Validation | OrderControllerTest.rejectsNullLine | ✅ | 100 % | 🟡 | — |
| Quantité | 0, négative ou absente → 400 / 422, plus contrainte CHECK en base | Quantité > 0 | OrderControllerTest, OrderServiceTest, OrderApiIT | ✅ | 100 % | 🔴 | — |
| Quantité | borne = 1 acceptée | Quantité > 0 | OrderServiceTest.acceptsQuantityOfOne | ✅ | 100 % | 🟡 | — |
| Fusion | même produit sur 2 lignes → quantités additionnées | R5 | OrderServiceTest, OrderApiIT | ✅ | 100 % | 🔴 | — |
| Fusion | débordement d'entier → 422 | R5 | OrderServiceTest.rejectsMergedQuantityOverflow, OrderApiIT.rejectsMergedQuantityOverflowAndPersistsNothing | ✅ | 100 % | 🟠 | — |
| Client | `clientId` absent → 400 | 1 client exactement | OrderControllerTest.rejectsMissingClient | ✅ | 100 % | 🔴 | — |
| Client | inexistant → 404 | 1 client exactement | OrderServiceTest, OrderApiIT | ✅ | 100 % | 🟠 | — |
| Produit | inexistant → 404, rien de persisté | — | OrderServiceTest, OrderApiIT | ✅ | 100 % | 🟠 | — |
| Création commande | en-tête `Location` | Contrat API | OrderControllerTest, OrderApiIT | ✅ | 100 % | 🔵 | — |
| Création commande | JSON mal formé → 400 problem+json | Validation | OrderControllerTest.rejectsMalformedJson | ✅ | 100 % | 🟡 | — |
| Lecture commande | existante / inexistante → 404 | — | OrderControllerTest, OrderApiIT | ✅ | 100 % | 🟠 | — |
| Liste commandes | lignes chargées hors transaction | — | OrderApiIT.listsOrdersWithLinesOutsideTransaction | ✅ | 100 % | 🟡 | — |
| Transition | DRAFT → CONFIRMED | Statuts | OrderStatusTest, OrderServiceTest, OrderApiIT | ✅ | 100 % | 🔴 | — |
| Transition | DRAFT → CANCELLED | Statuts | idem + draftOrderCanBeCancelledDirectly | ✅ | 100 % | 🔴 | — |
| Transition | CONFIRMED → CANCELLED | Statuts | idem | ✅ | 100 % | 🔴 | — |
| Transition | les 6 transitions interdites → 409, statut inchangé | Statuts | OrderStatusTest (9 combinaisons), OrderServiceTest (6), OrderApiIT | ✅ | 100 % | 🔴 | — |
| Transition | statut inconnu (SHIPPED) → 400 | Validation | OrderControllerTest | ✅ | 100 % | 🟡 | — |
| Transition | statut absent → 400 | Validation | OrderControllerTest | ✅ | 100 % | 🟡 | — |
| Transition | commande inexistante → 404 | — | OrderServiceTest, OrderControllerTest | ✅ | 100 % | 🟠 | — |
| Transition | modification concurrente → 409 | Statuts | OrderApiIT (niveau JPA), OrderControllerTest (simulé), ConcurrentAccessIT.concurrentStatusChangeThroughApiIsRejectedWith409 | ✅ | 100 % | 🟠 | — |
| Transition | confirmation d'une commande dont un produit est devenu inactif | (déduite) | OrderApiIT | ✅ | 100 % | 🟡 | Règle à confirmer |
| Base de données | contraintes CHECK (prix, email en minuscules, quantité, statut) | Toutes | OrderApiIT.databaseConstraintsRejectInvalidData | ✅ | 100 % | 🟡 | — |
| Erreurs | format ProblemDetail, carte `errors` des champs | Validation | Tests de contrôleur | ✅ | 100 % | 🟡 | — |
| Technique | documentation OpenAPI exposée | — | — | ❌ | 0 % | 🔵 | Smoke test |
| Technique | chargement du contexte | — | SpringAiLabApplicationIT | ✅ | 100 % | 🔵 | — |
| Sécurité | authentification, admin / shop | — | — | n/a | hors total | — | Aucun code de sécurité |
| **TOTAL (61 scénarios)** | 56 ✅ · 1 ⚠️ · 4 ❌ |  |  |  | **93,0 %** (87,3 % avant) |  |  |

Calcul : (56 × 100 + 75) / 61 = 5 675 / 61 = **93,0 %** (avant correction : 5 325 / 61 = 87,3 %).
Toutes les règles 🔴 issues de `business-rules.md` et tous les chemins 🟠 sont couverts à 100 %. Les manques restants concernent de la validation (🟡) ou le contrat d'API (🔵).

## 6. Plan d'action priorisé

Les étapes 1 à 4 sont traitées (voir « Suivi des corrections »). Les étapes 5 à 8 restent à faire.

1. ✅ 🟠 **Réactivation d'un produit.** Test d'intégration Testcontainers : créer un produit, le désactiver, vérifier que la commande renvoie 422, le réactiver (`active: true` → 200 et `$.active == true`), puis vérifier que la commande renvoie 201. Ajouter aussi `ProductServiceTest.activatesProduct`.
2. ✅ 🟠 **Doublons réels en base.** Test d'intégration : insérer un client par JDBC, mettre un mock (`@MockitoSpyBean`) sur `existsByEmail` qui renvoie `false`, puis faire un POST et attendre 409 avec le détail générique. Faire de même pour `reference` côté produit. Ajouter un cas `@WebMvcTest` produit qui simule `DataIntegrityViolationException` et attend 409.
3. ✅ 🟠 **Concurrence sur le statut, de bout en bout.** Test d'intégration avec deux appels `changeStatus` concurrents (deux threads avec `CountDownLatch`, ou un EntityManager qui avance la version entre la lecture et l'écriture). Résultat attendu : un appel en 200, l'autre en 409, et un statut final cohérent.
4. ✅ 🟠 **Débordement via HTTP.** Test d'intégration : deux lignes sur le même produit avec `quantity = 2147483647` puis `1`. Attendu : 422, aucune commande persistée.
5. 🟡 **Bornes de validation.** `@WebMvcTest` paramétré : 255 caractères acceptés, 256 → 400, sur `firstName`, `lastName`, `email`, `reference` et `name`. Ajouter `name: ""` → 400.
6. 🔵 **Contrat API.** `ClientControllerTest.createReturns201WithLocation`. Création d'un produit avec `active: false`, réponse attendue `active == false`.
7. 🔵 **Tests unitaires de lecture.** `OrderServiceTest.findByIdReturnsOrder`, `findByIdFailsForUnknownOrder` et `findAllReturnsOrders` (Mockito).
8. 🔵 **Smoke test OpenAPI.** Test d'intégration : `GET /v3/api-docs` → 200, contenant `/api/orders`.

## Points à clarifier

- **Contexte du prompt** : il décrit un autre projet (domaines `account`, `category`, web Thymeleaf, sécurité). Est-ce le bon dépôt, ou ce prompt est-il un modèle générique ?
- **Sécurité** : l'application n'a aucune authentification. Est-ce volontaire pour ce projet ?
- **Commande dont un produit est désactivé après sa création** : `OrderApiIT` vérifie qu'elle reste confirmable. Le texte « nouvelle commande » de la règle le laisse penser, mais la règle n'est pas écrite explicitement.
- **Réactivation d'un produit** : l'API la permet, mais la documentation ne dit rien. Les tests ajoutés vérifient le comportement actuel, sans en faire une règle métier.
- **Unicité de la référence produit** : doit-elle ignorer la casse, comme l'email (R4) ? Aujourd'hui, la comparaison est exacte.
- **Quantité maximale par ligne** : seule la limite technique `Integer.MAX_VALUE` existe, sans règle métier.
- **Arrondi et devise des totaux** : non documentés. Les prix ont 2 décimales, mais rien n'est précisé pour les totaux.
- **Collection Bruno** : elle couvre certains manques (la réactivation, par exemple), mais Maven ne l'exécute pas, et elle n'est donc pas comptée ici.

## Suivi des corrections (13/10/2026)

Tests ajoutés pour les 5 manques 🟠 ; aucun fichier de `src/main` modifié, aucun défaut du code de production découvert.

| Manque traité | Test(s) ajouté(s) | Résultat | % du scénario |
| --- | --- | --- | --- |
| Réactivation d'un produit, puis commande | `ProductServiceTest.activatesProduct`, `OrderApiIT.reactivatedProductCanBeOrderedAgain` | ✅ vert | 0 % → 100 % |
| Doublon de référence produit (contrainte base) | `ProductControllerTest.mapsConcurrentDuplicateReferenceTo409WithoutLeakingSqlDetails`, `ConcurrentAccessIT.duplicateReferenceMissedByPreCheckIsRejectedByDatabaseWith409` | ✅ vert | 50 % → 100 % |
| Doublon d'email (contrainte base) | `ConcurrentAccessIT.duplicateEmailMissedByPreCheckIsRejectedByDatabaseWith409` | ✅ vert | 50 % → 100 % |
| Changement de statut concurrent via l'API | `ConcurrentAccessIT.concurrentStatusChangeThroughApiIsRejectedWith409` | ✅ vert | 75 % → 100 % |
| Débordement de quantité fusionnée via HTTP | `OrderApiIT.rejectsMergedQuantityOverflowAndPersistsNothing` | ✅ vert | 75 % → 100 % |

**Technique de simulation (`ConcurrentAccessIT`).** Des espions `@MockitoSpyBean` sur les repositories reproduisent de façon déterministe l'entrelacement de deux requêtes, sans `sleep` ni threads :

- doublons : `existsByEmail` / `existsByReference` forcés à `false`, si bien que l'INSERT viole réellement la contrainte `UNIQUE` ;
- statut : juste après la lecture de la commande par le service, une transaction séparée (`REQUIRES_NEW`) annule la commande et incrémente `version`. La mise à jour du service échoue alors sur le verrou optimiste. Comme les repositories sont des proxys JDK, l'espion ne peut pas appeler la méthode réelle : la commande est lue via l'`EntityManager` de la transaction du service.

**Vérification :** `./mvnw clean verify` → BUILD SUCCESS, 93 tests unitaires et 23 tests d'intégration. Couverture fusionnée inchangée (100 %) ; couverture d'intégration seule passée de 87,2 % à 89,9 % des lignes.
