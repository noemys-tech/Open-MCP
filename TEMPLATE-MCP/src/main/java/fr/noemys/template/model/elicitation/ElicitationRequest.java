package fr.noemys.template.model.elicitation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Elicitation Request - MCP 2025-06-18
 * Used when a tool needs additional information from the user
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ElicitationRequest {
    
    @JsonProperty("request_id")
    private String requestId;
    
    @JsonProperty("prompt")
    private String prompt;
    
    /**
     * Schema describing the fields being requested from the user
     * Format: JSON Schema with field definitions
     */
    @JsonProperty("fields")
    private Map<String, Object> fields;
}

