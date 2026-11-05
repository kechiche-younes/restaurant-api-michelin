# API restaurants Michelin

## Quelques choix dans le projet

- Pour la moyenne, le calcul se fait avec `average()` sur les notes et `orElse(-1)` si le restaurant n'a pas d'évaluation. Elle n'est pas stockée en base pour éviter de devoir la modifier à chaque ajout ou suppression de note.

- Pour vérifier le propriétaire d'une évaluation, la comparaison utilise le `sub` du JWT plutôt que le nom. Deux utilisateurs peuvent avoir le même nom. ADMIN peut passer ce contrôle pour modifier toutes les évaluations.

- Pour la recherche avec plusieurs mots, Lucene utilise `AND` : tous les mots doivent être dans le commentaire. `updateDocument` remplace l'ancien commentaire quand une évaluation change, pour ne pas garder deux versions dans la recherche.

- L'index est reconstruit au démarrage depuis la base. Comme ça, si le dossier de l'index est supprimé, la recherche peut être recréée avec les données enregistrées.

- Pour les images, la clé utilise un UUID pour éviter que deux fichiers avec le même nom se remplacent. Le même traitement d'envoi est utilisé pour les deux buckets, seul le nom du bucket change.

- Les images sont ajoutées après la création, avec une route séparée. Ça permet d'envoyer le JSON puis le fichier. Leur ajout reste facultatif dans cette version.

- Le nom de l'évaluateur vient du JWT. Sa validation est dans le service car il ne passe pas par le DTO de la requête.

Swagger : http://localhost:8080/swagger-ui/index.html

La base H2 est créée au démarrage. Au début elle est vide, il faut créer un restaurant avec ADMIN. Les comptes sont ceux du sujet. Les requêtes protégées utilisent le JWT dans `Authorization: Bearer <jeton>`.

## Lancer le projet

Depuis la racine du dépôt :

```bash
cd restaurant-api
./mvnw spring-boot:run
```
