package fr.noemys.template.model.elicitation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Elicitation Response - MCP 2025-06-18
 * User's response to an elicitation request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ElicitationResponse {
    
    @JsonProperty("request_id")
    private String requestId;
    
    /**
     * Map of field names to their provided values
     */
    @JsonProperty("values")
    private Map<String, Object> values;
}

