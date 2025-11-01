package fr.noemys.s3.service;

import fr.noemys.s3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * MCP Service implementing the Model Context Protocol
 * S3 version with full S3 tools
 * 
 * @version 1.0.0
 */
@Service
public class McpService {
    
    private static final Logger log = LoggerFactory.getLogger(McpService.class);
    
    private static final String SERVER_NAME = "s3-mcp-server";
    private static final String SERVER_VERSION = "1.0.0";
    
    private final S3Service s3Service;
    
    @Autowired
    public McpService(S3Service s3Service) {
        this.s3Service = s3Service;
    }
    
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
        
        // Define listBuckets tool
        Map<String, Object> listBucketsProperties = new HashMap<>();
        listBucketsProperties.put("token", Map.of(
                "type", "string",
                "description", "S3 Access Key ID"
        ));
        listBucketsProperties.put("endpoint", Map.of(
                "type", "string",
                "description", "S3 server URL"
        ));
        listBucketsProperties.put("userToken", Map.of(
                "type", "string",
                "description", "S3 Secret Access Key"
        ));
        
        McpTool listBucketsTool = McpTool.builder()
                .name("listBuckets")
                .description("Lists all S3 buckets")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", listBucketsProperties,
                        "required", List.of("token", "endpoint", "userToken")
                ))
                .build();
        
        tools.add(listBucketsTool);
        
        // Define listObjects tool
        Map<String, Object> listObjectsProperties = new HashMap<>();
        listObjectsProperties.put("token", Map.of(
                "type", "string",
                "description", "S3 Access Key ID"
        ));
        listObjectsProperties.put("endpoint", Map.of(
                "type", "string",
                "description", "S3 server URL"
        ));
        listObjectsProperties.put("userToken", Map.of(
                "type", "string",
                "description", "S3 Secret Access Key"
        ));
        listObjectsProperties.put("bucketName", Map.of(
                "type", "string",
                "description", "Bucket name"
        ));
        listObjectsProperties.put("prefix", Map.of(
                "type", "string",
                "description", "Prefix to filter objects"
        ));
        
        McpTool listObjectsTool = McpTool.builder()
                .name("listObjects")
                .description("Lists objects in an S3 bucket")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", listObjectsProperties,
                        "required", List.of("token", "endpoint", "userToken", "bucketName")
                ))
                .build();
        
        tools.add(listObjectsTool);
        
        // Define downloadObject tool
        Map<String, Object> downloadObjectProperties = new HashMap<>();
        downloadObjectProperties.put("token", Map.of(
                "type", "string",
                "description", "S3 Access Key ID"
        ));
        downloadObjectProperties.put("endpoint", Map.of(
                "type", "string",
                "description", "S3 server URL"
        ));
        downloadObjectProperties.put("userToken", Map.of(
                "type", "string",
                "description", "S3 Secret Access Key"
        ));
        downloadObjectProperties.put("bucketName", Map.of(
                "type", "string",
                "description", "Bucket name"
        ));
        downloadObjectProperties.put("objectKey", Map.of(
                "type", "string",
                "description", "Object key to download"
        ));
        
        McpTool downloadObjectTool = McpTool.builder()
                .name("downloadObject")
                .description("Downloads an object from an S3 bucket")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", downloadObjectProperties,
                        "required", List.of("token", "endpoint", "userToken", "bucketName", "objectKey")
                ))
                .build();
        
        tools.add(downloadObjectTool);
        
        // Define getObjectMetadata tool
        Map<String, Object> getObjectMetadataProperties = new HashMap<>();
        getObjectMetadataProperties.put("token", Map.of(
                "type", "string",
                "description", "S3 Access Key ID"
        ));
        getObjectMetadataProperties.put("endpoint", Map.of(
                "type", "string",
                "description", "S3 server URL"
        ));
        getObjectMetadataProperties.put("userToken", Map.of(
                "type", "string",
                "description", "S3 Secret Access Key"
        ));
        getObjectMetadataProperties.put("bucketName", Map.of(
                "type", "string",
                "description", "Bucket name"
        ));
        getObjectMetadataProperties.put("objectKey", Map.of(
                "type", "string",
                "description", "Object key"
        ));
        
        McpTool getObjectMetadataTool = McpTool.builder()
                .name("getObjectMetadata")
                .description("Retrieves metadata for an S3 object")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", getObjectMetadataProperties,
                        "required", List.of("token", "endpoint", "userToken", "bucketName", "objectKey")
                ))
                .build();
        
        tools.add(getObjectMetadataTool);
        
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
        } else if ("listBuckets".equals(toolName)) {
            return executeListBuckets(arguments);
        } else if ("listObjects".equals(toolName)) {
            return executeListObjects(arguments);
        } else if ("downloadObject".equals(toolName)) {
            return executeDownloadObject(arguments);
        } else if ("getObjectMetadata".equals(toolName)) {
            return executeGetObjectMetadata(arguments);
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
     * Execute listBuckets tool
     */
    private Map<String, Object> executeListBuckets(Map<String, Object> arguments) {
        log.info("Executing listBuckets tool");
        
        String token = (String) arguments.get("token");
        String endpoint = (String) arguments.get("endpoint");
        String userToken = (String) arguments.get("userToken");
        
        Map<String, Object> s3Result = s3Service.listBuckets(endpoint, token, userToken);
        
        // Build a user-friendly text that includes bucket names
        List<String> bucketNames = new ArrayList<>();
        Object bucketsObj = s3Result.get("buckets");
        if (bucketsObj instanceof List<?>) {
            for (Object item : (List<?>) bucketsObj) {
                if (item instanceof Map<?, ?> map) {
                    Object name = map.get("name");
                    if (name != null) {
                        bucketNames.add(name.toString());
                    }
                }
            }
        }
        String summaryText = bucketNames.isEmpty()
                ? "Buckets retrieved: 0"
                : "Buckets (" + bucketNames.size() + "): " + String.join(", ", bucketNames);
        
        Map<String, Object> content = new HashMap<>();
        content.put("type", "text");
        content.put("text", summaryText);
        
        Map<String, Object> result = new HashMap<>();
        result.put("content", List.of(content));
        result.put("data", s3Result);
        
        log.info("listBuckets tool executed successfully");
        return result;
    }
    
    /**
     * Execute listObjects tool
     */
    private Map<String, Object> executeListObjects(Map<String, Object> arguments) {
        log.info("Executing listObjects tool");
        
        String token = (String) arguments.get("token");
        String endpoint = (String) arguments.get("endpoint");
        String userToken = (String) arguments.get("userToken");
        String bucketName = (String) arguments.get("bucketName");
        String prefix = (String) arguments.getOrDefault("prefix", "");
        
        Map<String, Object> s3Result = s3Service.listObjects(endpoint, token, userToken, bucketName, prefix);
        
        // Build a user-friendly text that includes object keys
        List<String> objectKeys = new ArrayList<>();
        Object objectsObj = s3Result.get("objects");
        if (objectsObj instanceof List<?>) {
            for (Object item : (List<?>) objectsObj) {
                if (item instanceof Map<?, ?> map) {
                    Object key = map.get("key");
                    if (key != null) {
                        objectKeys.add(key.toString());
                    }
                }
            }
        }
        String summaryText = objectKeys.isEmpty()
                ? "Objects retrieved: 0"
                : "Objects (" + objectKeys.size() + "): " + String.join(", ", objectKeys);
        
        Map<String, Object> content = new HashMap<>();
        content.put("type", "text");
        content.put("text", summaryText);
        
        Map<String, Object> result = new HashMap<>();
        result.put("content", List.of(content));
        result.put("data", s3Result);
        
        log.info("listObjects tool executed successfully");
        return result;
    }
    
    /**
     * Execute downloadObject tool
     */
    private Map<String, Object> executeDownloadObject(Map<String, Object> arguments) {
        log.info("Executing downloadObject tool");
        
        String token = (String) arguments.get("token");
        String endpoint = (String) arguments.get("endpoint");
        String userToken = (String) arguments.get("userToken");
        String bucketName = (String) arguments.get("bucketName");
        String objectKey = (String) arguments.get("objectKey");
        
        Map<String, Object> s3Result = s3Service.downloadObject(endpoint, token, userToken, bucketName, objectKey);
        
        Map<String, Object> content = new HashMap<>();
        content.put("type", "text");
        content.put("text", "Object downloaded: " + objectKey);
        
        Map<String, Object> result = new HashMap<>();
        result.put("content", List.of(content));
        result.put("data", s3Result);
        
        log.info("downloadObject tool executed successfully");
        return result;
    }
    
    /**
     * Execute getObjectMetadata tool
     */
    private Map<String, Object> executeGetObjectMetadata(Map<String, Object> arguments) {
        log.info("Executing getObjectMetadata tool");
        
        String token = (String) arguments.get("token");
        String endpoint = (String) arguments.get("endpoint");
        String userToken = (String) arguments.get("userToken");
        String bucketName = (String) arguments.get("bucketName");
        String objectKey = (String) arguments.get("objectKey");
        
        Map<String, Object> s3Result = s3Service.getObjectMetadata(endpoint, token, userToken, bucketName, objectKey);
        
        Map<String, Object> content = new HashMap<>();
        content.put("type", "text");
        content.put("text", "Metadata retrieved for: " + objectKey);
        
        Map<String, Object> result = new HashMap<>();
        result.put("content", List.of(content));
        result.put("data", s3Result);
        
        log.info("getObjectMetadata tool executed successfully");
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

