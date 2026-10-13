Je souhaite que tu construises intégralement le projet AI Spring Lab dans le répertoire courant.

Avant de commencer :

1. Lis et respecte les instructions de "AGENTS.md".
2. Lis "business-rules.md" et considère ce fichier comme la référence pour les exigences métier.
3. Examine les agents personnalisés présents dans ".opencode/agents/" :
    - "reviewer.md"
    - "tester.md"
    - "explainer.md"
4. Analyse les fichiers existants avant toute modification afin de préserver le travail déjà effectué.

Objectif

Construire une API REST de gestion de commandes avec Java 21 et Spring Boot.

Utilise Maven, Spring Web, Spring Data JPA, Bean Validation, PostgreSQL, JUnit 5, Mockito, AssertJ, Spring Boot Test et Testcontainers.

Architecture

Respecte strictement le flux :

Controller → Service → Repository

Applique les conventions et contraintes définies dans "AGENTS.md".

Règles métier

Implémente exclusivement les règles documentées dans "business-rules.md".

N'invente aucune règle métier. Si une ambiguïté empêche une implémentation correcte, demande-moi une clarification avant de poursuivre sur le comportement concerné.

Génération du code

Ne te limite pas à proposer une architecture ou à produire des exemples.

Crée et modifie réellement les fichiers nécessaires dans le répertoire courant, notamment :

- "pom.xml"
- Les classes Java de l'application.
- Les contrôleurs REST, services, repositories, entités et DTO.
- La gestion des exceptions et la validation.
- Les tests unitaires et d'intégration.
- La configuration PostgreSQL et Testcontainers.
- Les fichiers de configuration Spring.
- "docker-compose.yml" si nécessaire.
- "README.md" et ".gitignore".

Adapte le contenu à l'existant et évite les modifications sans rapport avec le projet.

Utilisation des agents

Utilise les agents personnalisés lorsqu'ils sont pertinents :

- "@tester" pour la stratégie de test, les tests et leur exécution.
- "@reviewer" pour examiner les modifications et détecter les défauts.
- "@explainer" pour expliquer l'architecture et les choix techniques.

Consulte leurs instructions et respecte leurs permissions. Ne prétends pas avoir invoqué un agent si tu ne l'as pas réellement fait.

Vérification finale

1. Compile le projet.
2. Exécute les tests unitaires.
3. Exécute les tests d'intégration lorsque l'environnement le permet.
4. Corrige les erreurs détectées.
5. Examine le diff Git.
6. Vérifie la conformité avec "AGENTS.md" et "business-rules.md".

Termine par un compte rendu indiquant les fichiers créés, les fonctionnalités implémentées, les commandes exécutées, les résultats réels des tests et les éventuelles limitations.

Commence par analyser le projet et passe ensuite à son implémentation.