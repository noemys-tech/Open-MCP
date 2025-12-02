package fr.noemys.s3.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.noemys.s3.model.JsonRpcRequest;
import fr.noemys.s3.model.JsonRpcResponse;
import fr.noemys.s3.model.SessionInfo;
import fr.noemys.s3.service.McpService;
import fr.noemys.s3.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Map;

/**
 * MCP HTTP Controller for MCP 2025-06-18 with HTTP Streaming (without OAuth)
 * 
 * @version 1.0.1 - OAuth removed, simplified session management
 */
@RestController
public class McpHttpController {
    
    private static final Logger log = LoggerFactory.getLogger(McpHttpController.class);
    private static final String SESSION_HEADER = "Mcp-Session-Id";
    
    private final SessionService sessionService;
    private final McpService mcpService;
    private final ObjectMapper objectMapper;
    
    public McpHttpController(
            SessionService sessionService,
            McpService mcpService,
            ObjectMapper objectMapper) {
        this.sessionService = sessionService;
        this.mcpService = mcpService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Create MCP Session - Simplified (no OAuth required)
     */
    @PostMapping("/mcp/session")
    public ResponseEntity<Map<String, String>> createSession(
            @RequestBody(required = false) Map<String, String> request) {
        log.info("POST /mcp/session");
        
        String clientId = request != null ? request.getOrDefault("clientId", "anonymous") : "anonymous";
        SessionInfo session = sessionService.createSession(clientId);
        
        return ResponseEntity.ok(Map.of("sessionId", session.getSessionId()));
    }
    
    /**
     * MCP Streaming Endpoint - POST (Send JSON-RPC requests)
     * Simplified: Auto-creates session if not provided
     */
    @PostMapping(value = "/mcp", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonRpcResponse> mcpPost(
            @RequestHeader(value = SESSION_HEADER, required = false) String sessionId,
            @RequestBody String requestBody) {
        
        log.info("POST /mcp - Session: {}", sessionId);
        
        // Auto-create session if not provided or invalid
        if (sessionId == null || !sessionService.validateSession(sessionId)) {
            SessionInfo autoSession = sessionService.createSession("anonymous");
            sessionId = autoSession.getSessionId();
            log.info("Auto-created session: {}", sessionId);
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
     * MCP Streaming Endpoint - GET (Receive server-initiated messages)
     */
    @GetMapping(value = "/mcp", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public ResponseEntity<StreamingResponseBody> mcpGet(
            @RequestHeader(value = SESSION_HEADER, required = false) String sessionId) {
        
        log.info("GET /mcp - Session: {}", sessionId);
        
        // Auto-create session if not provided
        if (sessionId == null || !sessionService.validateSession(sessionId)) {
            SessionInfo autoSession = sessionService.createSession("anonymous");
            sessionId = autoSession.getSessionId();
            log.info("Auto-created session for streaming: {}", sessionId);
        }
        
        // Update session last access
        sessionService.updateLastAccess(sessionId);
        
        final String finalSessionId = sessionId;
        
        // Stream response body
        StreamingResponseBody stream = outputStream -> {
            try {
                log.info("Streaming connection established for session: {}", finalSessionId);
                
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
                .header(SESSION_HEADER, finalSessionId)
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
                "version", "1.0.1",
                "protocol", "MCP 2025-06-18",
                "endpoints", Map.of(
                        "health", "/health",
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
                "version", "1.0.1",
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
