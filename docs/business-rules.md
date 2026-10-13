Développer une application Java Springboot de gestion de commande :
RÈGLES MÉTIER
Client
Un client possède :

prénom ;
nom ;
email unique.
Produit
Un produit possède :

référence ;
nom ;
prix ;
statut actif/inactif.
Un produit inactif ne peut pas être ajouté à une nouvelle commande.

Commande
Une commande :

appartient à exactement un client ;
contient au moins une ligne ;
possède une quantité strictement positive par ligne ;
mémorise le prix du produit au moment de la commande.
Calcul
Total ligne = quantité × prix unitaire
Total commande = somme des lignes
Statuts
Transitions autorisées
DRAFT      -> CONFIRMED
DRAFT      -> CANCELLED
CONFIRMED  -> CANCELLED
Aucune autre transition n'est autorisée.

CLARIFICATIONS (questions / réponses utilisateur)

Q1. Quel est le statut initial d'une commande ? Proposition : DRAFT, seule origine possible des transitions.
R1. C'est ok : une nouvelle commande est créée au statut DRAFT.

Q2. Un prix nul ou négatif est-il autorisé pour un produit ?
R2. Non : le prix d'un produit doit être strictement positif.

Q3. La référence d'un produit doit-elle être unique ?
R3. Oui : la référence produit est unique.

Q4. L'unicité de l'email client doit-elle ignorer la casse ?
R4. Oui : les emails ignorent la casse. Un email est toujours enregistré en base en minuscules.

Q5. Une commande contient deux lignes pour le même produit (ex. Clavier × 2 et Clavier × 3). Que doit faire l'API ?
R5. Les lignes sont fusionnées en une seule ligne dont la quantité est la somme des quantités (Clavier × 5).
