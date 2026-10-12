Développer une application de gestion de commande :
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
