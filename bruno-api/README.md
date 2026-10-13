# Collection Bruno

Tests d'API de bout en bout pour l'application, au format [Bruno](https://www.usebruno.com/).

## Organisation

| Dossier     | Contenu                                                                 |
|-------------|-------------------------------------------------------------------------|
| `clients`   | Création, lecture, liste, unicité de l'email (insensible à la casse), validation |
| `products`  | Création, lecture, liste, unicité de la référence, prix, (dés)activation |
| `orders`    | Création (fusion des lignes, totaux), produit inactif, validation, transitions de statut |

Les dossiers s'exécutent dans l'ordre `clients` → `products` → `orders` : les commandes réutilisent
le client et les produits créés précédemment (variables `clientId`, `keyboardId`, `mouseId`,
`inactiveProductId`). Emails et références sont suffixés d'un horodatage, la collection est donc
rejouable sans vider la base.

## Exécution

1. Démarrer la base et l'application depuis la racine du projet :

   ```bash
   docker compose up -d
   ./mvnw spring-boot:run
   ```

2. Lancer la collection :
   - **Bruno (interface)** : ouvrir le dossier `bruno-api`, choisir l'environnement `local`, puis *Run* sur la collection.
   - **CLI** :

     ```bash
     cd bruno-api
     npx @usebruno/cli run -r --env local
     ```

L'URL de l'API se règle dans `environments/local.bru` (`baseUrl`, par défaut `http://localhost:8080`).
