package fr.noemys.template.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Session information for MCP HTTP streaming
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionInfo {
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("clientId")
    private String clientId;
    
    @JsonProperty("accessToken")
    private String accessToken;
    
    @JsonProperty("creationTime")
    private Instant creationTime;
    
    @JsonProperty("lastAccessTime")
    private Instant lastAccessTime;
    
    @JsonProperty("expiresAt")
    private Instant expiresAt;
}

