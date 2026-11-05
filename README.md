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



## Limites

La suppression d'une évaluation ne supprime pas ses photos dans S3. Les réponses contiennent les clés des images, sans URL signée. Aussi, les écritures dans la base et dans Lucene ne sont pas dans une transaction commune : en cas d'échec, il peut y avoir un décalage jusqu'à la reconstruction de l'index.

## Lancer le projet

Il faut Java 17 ou une version supérieure et une connexion Internet pour télécharger les dépendances et accéder aux services du cours (Keycloak et S3). Le wrapper Maven est fourni dans le projet.

Depuis le dossier du dépôt, entrer dans le dossier de l'application :

```bash
cd restaurant-api
```

Configurer les deux variables d'environnement avec les identifiants S3 fournis dans le sujet. Les valeurs ci-dessous sont à remplacer ; les véritables identifiants ne sont pas inclus dans le dépôt.

```bash
export S3_ACCESS_KEY='votre-cle-acces'
export S3_SECRET_KEY='votre-cle-secrete'
```

Puis démarrer l'application dans le même terminal :

```bash
./mvnw spring-boot:run
```

Sous Windows, définir ces variables dans l'environnement ou dans la configuration de lancement de l'IDE, puis utiliser `mvnw.cmd spring-boot:run`.

Une fois l'application démarrée :

- API : http://localhost:8080/restaurants
- Swagger : http://localhost:8080/swagger-ui/index.html
- Description OpenAPI : http://localhost:8080/v3/api-docs

La base H2 est stockée dans le fichier `database.mv.db`, dans le dossier de l'application. Elle est vide au premier lancement si aucun fichier de base n'existe, puis conserve les données entre les lancements. L'index Lucene est reconstruit au démarrage dans `index-evaluations`.

Pour les routes protégées, récupérer un jeton d'accès auprès du Keycloak du cours avec un compte fourni dans le sujet, puis ajouter l'en-tête suivant dans Postman :

```text
Authorization: Bearer <jeton>
```

La création d'un restaurant nécessite le rôle ADMIN. Par exemple, envoyer une requête `POST http://localhost:8080/restaurants`, avec le jeton ADMIN et le type de contenu `application/json` :

```json
{
  "nom": "L'Orientale",
  "adresse": "4 impasse de l'Épi"
}
```

Les comptes sont gérés par le Keycloak du cours, pas par cette application. Le serveur configuré est `https://keycloak.bober.ovh/realms/master` et les rôles sont lus dans `resource_access.coursm2.roles`.

Pour lancer les tests, depuis `restaurant-api` et avec les variables S3 définies :

```bash
./mvnw test
```

Pour arrêter l'application lancée dans le terminal, utiliser `Ctrl+C`.
