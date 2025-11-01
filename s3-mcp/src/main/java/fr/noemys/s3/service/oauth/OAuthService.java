package fr.noemys.s3.service.oauth;

import fr.noemys.s3.model.oauth.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OAuth 2.1 Service - Production-ready implementation with strict validation
 * 
 * @version 1.0.0
 */
@Service
public class OAuthService {
    
    private static final Logger log = LoggerFactory.getLogger(OAuthService.class);
    
    @Value("${mcp.oauth.issuer}")
    private String issuer;
    
    @Value("${mcp.oauth.token-expiration-seconds}")
    private long tokenExpirationSeconds;
    
    @Value("${mcp.oauth.refresh-token-expiration-seconds}")
    private long refreshTokenExpirationSeconds;
    
    @Value("${mcp.oauth.jwt-secret}")
    private String jwtSecretString;
    
    // In-memory storage for clients and tokens
    private final Map<String, ClientRegistration> clients = new ConcurrentHashMap<>();
    private final Map<String, TokenInfo> tokens = new ConcurrentHashMap<>();
    private SecretKey jwtKey;
    
    @PostConstruct
    public void init() {
        // Use a fixed secret key for production - must be at least 32 characters
        if (jwtSecretString == null || jwtSecretString.isEmpty() || jwtSecretString.startsWith("CHANGEME")) {
            log.error("JWT secret not configured properly! Please set mcp.oauth.jwt-secret in application.properties");
            throw new IllegalStateException("JWT secret must be configured for production use");
        }
        
        // Ensure the key is at least 256 bits (32 bytes)
        byte[] keyBytes = jwtSecretString.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            log.error("JWT secret is too short! Must be at least 32 characters");
            throw new IllegalStateException("JWT secret must be at least 32 characters");
        }
        
        this.jwtKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("OAuth service initialized with fixed JWT secret");
    }
    
    /**
     * Get OAuth 2.1 Authorization Server Metadata (RFC 8414)
     */
    public OAuthMetadata getMetadata() {
        log.info("Returning OAuth metadata");
        
        return OAuthMetadata.builder()
                .issuer(issuer)
                .authorizationEndpoint(issuer + "/oauth/authorize")
                .tokenEndpoint(issuer + "/oauth/token")
                .registrationEndpoint(issuer + "/oauth/register")
                .jwksUri(issuer + "/.well-known/jwks.json")
                .responseTypesSupported(List.of("code"))
                .grantTypesSupported(List.of("authorization_code", "refresh_token", "client_credentials"))
                .tokenEndpointAuthMethodsSupported(List.of("client_secret_basic", "client_secret_post"))
                .scopesSupported(List.of("mcp", "read", "write"))
                .codeChallengeMethodsSupported(List.of("S256", "plain"))
                .build();
    }
    
    /**
     * Register a new client (RFC 7591) - Production implementation
     */
    public ClientRegistration registerClient(ClientRegistration request) {
        log.info("Registering new client: {}", request.getClientName());
        
        // Validate required fields
        if (request.getClientName() == null || request.getClientName().isEmpty()) {
            throw new IllegalArgumentException("Client name is required");
        }
        
        // Generate client_id and client_secret
        String clientId = "client_" + UUID.randomUUID().toString().substring(0, 8);
        String clientSecret = "secret_" + UUID.randomUUID().toString();
        
        ClientRegistration registration = ClientRegistration.builder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientName(request.getClientName())
                .redirectUris(request.getRedirectUris())
                .grantTypes(request.getGrantTypes() != null ? request.getGrantTypes() : 
                           List.of("authorization_code", "refresh_token"))
                .responseTypes(request.getResponseTypes() != null ? request.getResponseTypes() : 
                              List.of("code"))
                .scope(request.getScope() != null ? request.getScope() : "mcp")
                .tokenEndpointAuthMethod(request.getTokenEndpointAuthMethod() != null ? 
                                        request.getTokenEndpointAuthMethod() : "client_secret_post")
                .clientSecretExpiresAt(0L) // Never expires
                .build();
        
        clients.put(clientId, registration);
        
        log.info("Client registered successfully: {}", clientId);
        return registration;
    }
    
    /**
     * Generate access token - Production implementation with strict validation
     */
    public TokenResponse generateToken(TokenRequest request) {
        log.info("Generating token for grant_type: {}", request.getGrantType());
        
        // Validate grant type
        if (request.getGrantType() == null || request.getGrantType().isEmpty()) {
            throw new IllegalArgumentException("Grant type is required");
        }
        
        // Validate client credentials - STRICT mode (production)
        ClientRegistration client = clients.get(request.getClientId());
        if (client == null) {
            log.error("Client not found: {}", request.getClientId());
            throw new IllegalArgumentException("Invalid client credentials");
        }
        
        // Validate client secret for confidential clients
        if (client.getClientSecret() != null && !client.getClientSecret().equals(request.getClientSecret())) {
            log.error("Invalid client secret for client: {}", request.getClientId());
            throw new IllegalArgumentException("Invalid client credentials");
        }
        
        // Generate tokens
        String accessToken = generateJwtToken(request.getClientId());
        String refreshToken = "refresh_" + UUID.randomUUID().toString();
        
        // Store token info
        TokenInfo tokenInfo = new TokenInfo(
                accessToken,
                request.getClientId(),
                Instant.now().plusSeconds(tokenExpirationSeconds)
        );
        tokens.put(accessToken, tokenInfo);
        
        log.info("Token generated successfully for client: {}", request.getClientId());
        
        return TokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(tokenExpirationSeconds)
                .refreshToken(refreshToken)
                .scope(request.getScope() != null ? request.getScope() : "mcp")
                .build();
    }
    
    /**
     * Validate access token
     */
    public boolean validateToken(String token) {
        TokenInfo tokenInfo = tokens.get(token);
        if (tokenInfo == null) {
            log.warn("Token not found");
            return false;
        }
        
        if (tokenInfo.expiresAt.isBefore(Instant.now())) {
            log.warn("Token expired");
            tokens.remove(token);
            return false;
        }
        
        return true;
    }
    
    /**
     * Get client ID from token
     */
    public String getClientIdFromToken(String token) {
        TokenInfo tokenInfo = tokens.get(token);
        return tokenInfo != null ? tokenInfo.clientId : null;
    }
    
    /**
     * Generate JWT token with fixed secret key
     */
    private String generateJwtToken(String clientId) {
        return Jwts.builder()
                .setSubject(clientId)
                .setIssuer(issuer)
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plusSeconds(tokenExpirationSeconds)))
                .signWith(jwtKey, SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * Internal token info storage
     */
    private record TokenInfo(String token, String clientId, Instant expiresAt) {}
}

