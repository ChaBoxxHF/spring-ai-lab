# AGENTS.md

Lab de formation « Initiation IA » (CEPN) : une application Spring Boot qui sert de terrain d'expérimentation pour Spring AI. Le code doit rester lisible par des apprenants : privilégier des classes courtes et explicites aux abstractions astucieuses.

## Stack

- Java 21, Maven via le wrapper (`./mvnw`).
- **Spring Boot 4.x** (Spring Framework 7, Jakarta EE). Les starters sont modulaires : `spring-boot-starter-webmvc` (et non `-web`), un starter de test par module (`spring-boot-starter-webmvc-test`, `-data-jpa-test`…), console H2 dans `spring-boot-h2console`. Calquer tout nouvel ajout sur ce découpage plutôt que sur les exemples Boot 3.
- Persistance : Spring Data JPA + H2 en mémoire (données perdues à chaque redémarrage), schéma piloté par Flyway.
- Spring AI n'est pas encore dans le `pom.xml`. Pour l'ajouter : importer `spring-ai-bom` dans `<dependencyManagement>` avec une version compatible Boot 4, puis déclarer les starters sans version.

## Conventions

- Package racine : `dev.crochard.spring_ai_lab` (underscore, le tiret étant invalide en Java).
- Configuration en YAML dans `src/main/resources/application.yaml`.
- Indentation Java par tabulations, comme le code généré existant.
- Toute évolution du schéma passe par une nouvelle migration `src/main/resources/db/migration/V<n>__<description>.sql` ; une migration déjà appliquée reste figée.
- Les clés d'API (OpenAI, Anthropic, etc.) se lisent depuis des variables d'environnement référencées dans le YAML, par exemple `${OPENAI_API_KEY}`, et restent hors du dépôt.

## Vérification

- `./mvnw test` doit passer avant de considérer une tâche terminée.
- `./mvnw spring-boot:run` pour lancer l'application (port 8080, console H2 sur `/h2-console`).
