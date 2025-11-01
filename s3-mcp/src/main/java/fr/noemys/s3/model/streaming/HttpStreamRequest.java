package fr.noemys.s3.model.streaming;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * HTTP Stream Request - MCP 2025-06-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HttpStreamRequest {
    
    @JsonProperty("method")
    private String method;
    
    @JsonProperty("data")
    private Object data;
}

