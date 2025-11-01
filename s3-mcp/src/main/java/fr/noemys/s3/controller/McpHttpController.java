package fr.noemys.s3.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.noemys.s3.model.JsonRpcRequest;
import fr.noemys.s3.model.JsonRpcResponse;
import fr.noemys.s3.model.SessionInfo;
import fr.noemys.s3.model.oauth.ClientRegistration;
import fr.noemys.s3.model.oauth.OAuthMetadata;
import fr.noemys.s3.model.oauth.TokenRequest;
import fr.noemys.s3.model.oauth.TokenResponse;
import fr.noemys.s3.service.McpService;
import fr.noemys.s3.service.SessionService;
import fr.noemys.s3.service.oauth.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Map;

/**
 * MCP HTTP Controller for MCP 2025-06-18 with OAuth 2.1 and HTTP Streaming
 * Production-ready version with strict authentication
 * 
 * @version 1.0.0
 */
@RestController
public class McpHttpController {
    
    private static final Logger log = LoggerFactory.getLogger(McpHttpController.class);
    private static final String SESSION_HEADER = "Mcp-Session-Id";
    
    private final OAuthService oauthService;
    private final SessionService sessionService;
    private final McpService mcpService;
    private final ObjectMapper objectMapper;
    
    public McpHttpController(
            OAuthService oauthService,
            SessionService sessionService,
            McpService mcpService,
            ObjectMapper objectMapper) {
        this.oauthService = oauthService;
        this.sessionService = sessionService;
        this.mcpService = mcpService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * OAuth 2.1 Authorization Server Metadata (RFC 8414)
     */
    @GetMapping("/.well-known/oauth-authorization-server")
    public ResponseEntity<OAuthMetadata> getOAuthMetadata() {
        log.info("GET /.well-known/oauth-authorization-server");
        OAuthMetadata metadata = oauthService.getMetadata();
        return ResponseEntity.ok(metadata);
    }
    
    /**
     * OAuth 2.1 Client Registration (RFC 7591)
     */
    @PostMapping("/oauth/register")
    public ResponseEntity<ClientRegistration> registerClient(@RequestBody ClientRegistration request) {
        log.info("POST /oauth/register - Client: {}", request.getClientName());
        
        try {
            ClientRegistration registration = oauthService.registerClient(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(registration);
        } catch (Exception e) {
            log.error("Error registering client", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    /**
     * OAuth 2.1 Token Endpoint
     */
    @PostMapping("/oauth/token")
    public ResponseEntity<TokenResponse> generateToken(@RequestBody TokenRequest request) {
        log.info("POST /oauth/token - Grant type: {}", request.getGrantType());
        
        try {
            TokenResponse token = oauthService.generateToken(request);
            return ResponseEntity.ok(token);
        } catch (IllegalArgumentException e) {
            log.error("Invalid token request", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error generating token", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Create MCP Session - Requires valid OAuth token
     */
    @PostMapping("/mcp/session")
    public ResponseEntity<Map<String, String>> createSession(
            @RequestHeader(value = "Authorization", required = true) String authorization) {
        log.info("POST /mcp/session");
        
        // Extract access token from Authorization header
        String accessToken = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            accessToken = authorization.substring(7);
        }
        
        if (accessToken == null || !oauthService.validateToken(accessToken)) {
            log.warn("Invalid or missing access token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header("WWW-Authenticate", "Bearer realm=\"MCP Server\"")
                    .build();
        }
        
        String clientId = oauthService.getClientIdFromToken(accessToken);
        SessionInfo session = sessionService.createSession(clientId, accessToken);
        
        return ResponseEntity.ok(Map.of("sessionId", session.getSessionId()));
    }
    
    /**
     * MCP Streaming Endpoint - POST (Send JSON-RPC requests)
     * Production mode: Requires valid session, NO anonymous sessions
     */
    @PostMapping(value = "/mcp", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonRpcResponse> mcpPost(
            @RequestHeader(value = SESSION_HEADER, required = false) String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody String requestBody) {
        
        log.info("POST /mcp - Session: {}, Authorization: {}", 
                sessionId, authorization != null ? "present" : "null");
        
        // Try to create session from Authorization header if no session ID provided
        if (sessionId == null || !sessionService.validateSession(sessionId)) {
            SessionInfo autoSession = createSessionFromAuthorization(authorization);
            
            if (autoSession != null) {
                sessionId = autoSession.getSessionId();
                log.info("Auto-created session from Authorization header: {}", sessionId);
            } else {
                log.warn("No valid session or credentials provided - rejecting request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .header("WWW-Authenticate", "Bearer realm=\"MCP Server\"")
                        .body(JsonRpcResponse.error(null, -32001, 
                                "Authentication required. Please provide Mcp-Session-Id header or valid Authorization Bearer token."));
            }
        }
        
        // Update session last access
        sessionService.updateLastAccess(sessionId);
        
        try {
            // Parse JSON-RPC request
            JsonRpcRequest request = objectMapper.readValue(requestBody, JsonRpcRequest.class);
            
            log.info("Processing JSON-RPC method: {}", request.getMethod());
            
            // Handle the request
            JsonRpcResponse response = handleJsonRpcRequest(request);
            
            // Add session ID to response header for client to use in future requests
            return ResponseEntity.ok()
                    .header(SESSION_HEADER, sessionId)
                    .body(response);
            
        } catch (Exception e) {
            log.error("Error processing MCP request", e);
            JsonRpcResponse errorResponse = JsonRpcResponse.error(null, -32603, 
                    "Internal error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Create session from Authorization header - Production mode (NO anonymous fallback)
     */
    private SessionInfo createSessionFromAuthorization(String authorization) {
        try {
            // Try to extract token from Authorization header
            if (authorization != null && authorization.startsWith("Bearer ")) {
                String accessToken = authorization.substring(7);
                if (oauthService.validateToken(accessToken)) {
                    String clientId = oauthService.getClientIdFromToken(accessToken);
                    return sessionService.createSession(clientId, accessToken);
                }
            }
            
            // Production mode: NO anonymous sessions
            log.error("No valid credentials provided, rejecting request");
            return null;
            
        } catch (Exception e) {
            log.error("Error creating session from authorization", e);
            return null;
        }
    }
    
    /**
     * MCP Streaming Endpoint - GET (Receive server-initiated messages)
     */
    @GetMapping(value = "/mcp", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public ResponseEntity<StreamingResponseBody> mcpGet(
            @RequestHeader(value = SESSION_HEADER, required = false) String sessionId) {
        
        log.info("GET /mcp - Session: {}", sessionId);
        
        // Validate session
        if (sessionId == null || !sessionService.validateSession(sessionId)) {
            log.warn("Invalid or missing session ID");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Update session last access
        sessionService.updateLastAccess(sessionId);
        
        // Stream response body
        StreamingResponseBody stream = outputStream -> {
            try {
                log.info("Streaming connection established for session: {}", sessionId);
                
                // Send a keep-alive message
                String keepAlive = "{\"type\":\"heartbeat\",\"timestamp\":\"" + 
                                  java.time.Instant.now().toString() + "\"}\n";
                outputStream.write(keepAlive.getBytes());
                outputStream.flush();
                
            } catch (Exception e) {
                log.error("Error in streaming response", e);
            }
        };
        
        return ResponseEntity.ok()
                .header("Transfer-Encoding", "chunked")
                .header("X-Content-Type-Options", "nosniff")
                .body(stream);
    }
    
    /**
     * Root endpoint - Server information
     */
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
                "name", "MCP S3 Server",
                "version", "1.0.0",
                "protocol", "MCP 2025-06-18",
                "endpoints", Map.of(
                        "health", "/health",
                        "oauth_metadata", "/.well-known/oauth-authorization-server",
                        "oauth_register", "/oauth/register",
                        "oauth_token", "/oauth/token",
                        "mcp_session", "/mcp/session",
                        "mcp", "/mcp"
                ),
                "documentation", "https://spec.modelcontextprotocol.io/"
        ));
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "version", "1.0.0",
                "protocol", "MCP 2025-06-18"
        ));
    }
    
    /**
     * Debug endpoint - Test tools/list response
     */
    @GetMapping("/debug/tools")
    public ResponseEntity<Map<String, Object>> debugTools() {
        Map<String, Object> tools = mcpService.listTools();
        return ResponseEntity.ok(tools);
    }
    
    /**
     * Handle JSON-RPC request
     */
    private JsonRpcResponse handleJsonRpcRequest(JsonRpcRequest request) {
        String method = request.getMethod();
        Object id = request.getId();
        Map<String, Object> params = request.getParams();
        
        // Handle notifications (methods starting with "notifications/")
        if (method != null && method.startsWith("notifications/")) {
            log.info("Received notification: {} (no response needed)", method);
            return JsonRpcResponse.success(id, Map.of());
        }
        
        try {
            Object result = switch (method) {
                case "initialize" -> mcpService.initialize(params != null ? params : Map.of());
                case "tools/list" -> mcpService.listTools();
                case "tools/call" -> {
                    if (params == null) {
                        throw new IllegalArgumentException("Parameters required for tools/call");
                    }
                    String toolName = (String) params.get("name");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> arguments = (Map<String, Object>) params.getOrDefault("arguments", Map.of());
                    yield mcpService.callTool(toolName, arguments);
                }
                case "ping" -> Map.of("status", "pong"); // MCP heartbeat
                default -> {
                    log.warn("Unknown method: {}", method);
                    throw new IllegalArgumentException("Unknown method: " + method);
                }
            };
            
            return JsonRpcResponse.success(id, result);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            return JsonRpcResponse.error(id, -32602, "Invalid params: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error executing method {}: {}", method, e.getMessage(), e);
            return JsonRpcResponse.error(id, -32603, "Internal error: " + e.getMessage());
        }
    }
}

