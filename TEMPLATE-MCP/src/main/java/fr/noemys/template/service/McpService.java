package fr.noemys.template.service;

import fr.noemys.template.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * MCP Service implementing the Model Context Protocol
 * Template version with demo sayHello tool
 * 
 * @version 1.0.0
 */
@Service
public class McpService {
    
    private static final Logger log = LoggerFactory.getLogger(McpService.class);
    
    private static final String SERVER_NAME = "template-mcp-server";
    private static final String SERVER_VERSION = "1.0.0";
    
    /**
     * Handle initialize request - MCP 2025-06-18
     */
    public Map<String, Object> initialize(Map<String, Object> params) {
        log.info("Initializing MCP server - Protocol 2025-06-18");
        
        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", "2025-06-18");
        result.put("serverInfo", ServerInfo.builder()
                .name(SERVER_NAME)
                .version(SERVER_VERSION)
                .build());
        
        // Capabilities for MCP 2025-06-18
        Map<String, Object> capabilities = new HashMap<>();
        
        // Tools capability with listChanged support
        Map<String, Object> toolsCapability = new HashMap<>();
        toolsCapability.put("listChanged", false); // Tools list is static
        capabilities.put("tools", toolsCapability);
        
        // Elicitation capability  
        Map<String, Object> elicitationCapability = new HashMap<>();
        capabilities.put("elicitation", elicitationCapability);
        
        result.put("capabilities", capabilities);
        
        log.info("MCP server initialized successfully with protocol 2025-06-18");
        return result;
    }
    
    /**
     * List all available tools - MCP 2025-06-18
     */
    public Map<String, Object> listTools() {
        log.info("Listing available tools");
        
        List<McpTool> tools = new ArrayList<>();
        
        // Define sayHello tool
        McpTool sayHelloTool = McpTool.builder()
                .name("sayHello")
                .description("Returns a hello world message")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(),
                        "required", List.of()
                ))
                .build();
        
        tools.add(sayHelloTool);
        
        Map<String, Object> result = new HashMap<>();
        result.put("tools", tools);
        
        log.info("Found {} tool(s)", tools.size());
        return result;
    }
    
    /**
     * Call a tool by name
     */
    public Map<String, Object> callTool(String toolName, Map<String, Object> arguments) {
        log.info("Calling tool: {}", toolName);
        
        if ("sayHello".equals(toolName)) {
            return executeSayHello();
        }
        
        log.error("Unknown tool: {}", toolName);
        throw new IllegalArgumentException("Unknown tool: " + toolName);
    }
    
    /**
     * Execute sayHello tool
     */
    private Map<String, Object> executeSayHello() {
        log.info("Executing sayHello tool");
        
        Map<String, Object> content = new HashMap<>();
        content.put("type", "text");
        content.put("text", "hello world");
        
        Map<String, Object> result = new HashMap<>();
        result.put("content", List.of(content));
        
        log.info("sayHello tool executed successfully");
        return result;
    }
    
    /**
     * Request elicitation from user - MCP 2025-06-18
     */
    public Map<String, Object> requestElicitation(String prompt, Map<String, Object> fieldsSchema) {
        log.info("Requesting elicitation: {}", prompt);
        
        String requestId = UUID.randomUUID().toString();
        
        Map<String, Object> elicitationRequest = new HashMap<>();
        elicitationRequest.put("request_id", requestId);
        elicitationRequest.put("prompt", prompt);
        elicitationRequest.put("fields", fieldsSchema);
        
        Map<String, Object> result = new HashMap<>();
        result.put("type", "elicitation");
        result.put("data", elicitationRequest);
        
        return result;
    }
    
    /**
     * Handle elicitation response from user - MCP 2025-06-18
     */
    public void handleElicitationResponse(String requestId, Map<String, Object> values) {
        log.info("Handling elicitation response for request: {}", requestId);
        log.info("Elicitation values received: {}", values);
    }
}

