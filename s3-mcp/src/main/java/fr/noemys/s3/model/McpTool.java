package fr.noemys.s3.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * MCP Tool definition - MCP 2025-06-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpTool {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("inputSchema")
    private Map<String, Object> inputSchema;
    
    /**
     * Tool annotations for MCP 2025-06-18
     * Possible values: "readOnly", "destructive", "idempotent"
     */
    @JsonProperty("annotations")
    private List<String> annotations;
    
    /**
     * Output schema for structured tool output (MCP 2025-06-18)
     */
    @JsonProperty("outputSchema")
    private Map<String, Object> outputSchema;
}

