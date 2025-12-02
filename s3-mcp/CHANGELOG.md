# Changelog - MCP S3 Server

## [1.0.1] - 2025-12-02

### Removed
- **OAuth 2.1 complètement retiré du projet**
  - Suppression des dépendances JWT (jjwt-api, jjwt-impl, jjwt-jackson)
  - Suppression du service `OAuthService`
  - Suppression des modèles OAuth (`ClientRegistration`, `OAuthMetadata`, `TokenRequest`, `TokenResponse`)
  - Suppression des endpoints OAuth (`/.well-known/oauth-authorization-server`, `/oauth/register`, `/oauth/token`)
  - Suppression de la validation des tokens d'accès

### Changed
- **Gestion de session simplifiée**
  - `SessionService.createSession()` ne nécessite plus d'access token
  - Suppression du champ `accessToken` du modèle `SessionInfo`
  - Les sessions sont maintenant créées automatiquement sans authentification
  - Endpoint `/mcp/session` simplifié (aucune authentification requise)

- **Contrôleur MCP simplifié**
  - `McpHttpController` n'utilise plus `OAuthService`
  - Auto-création de sessions pour les requêtes sans session ID
  - Suppression de toutes les vérifications d'autorisation Bearer
  - Les endpoints `/mcp` POST et GET sont maintenant accessibles sans authentification

- **Configuration nettoyée**
  - Suppression des configurations OAuth dans `application.properties`
  - Conservation uniquement de la configuration de timeout de session

### Notes de migration
- **Impact** : Le serveur MCP S3 fonctionne maintenant sans authentification
- **Sécurité** : À utiliser uniquement en environnement de confiance ou derrière un proxy d'authentification
- **Compatibilité** : Les clients doivent maintenant utiliser l'endpoint `/mcp/session` simplifié ou laisser le serveur créer automatiquement une session

## [1.0.0] - Initial Release
- Serveur MCP avec support S3/MinIO
- OAuth 2.1 intégré
- HTTP Streaming support

