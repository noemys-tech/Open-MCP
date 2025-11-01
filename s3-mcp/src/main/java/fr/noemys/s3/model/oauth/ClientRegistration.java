package fr.noemys.s3.model.oauth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OAuth 2.1 Client Registration (RFC 7591)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientRegistration {
    
    @JsonProperty("client_id")
    private String clientId;
    
    @JsonProperty("client_secret")
    private String clientSecret;
    
    @JsonProperty("client_name")
    private String clientName;
    
    @JsonProperty("redirect_uris")
    private List<String> redirectUris;
    
    @JsonProperty("grant_types")
    private List<String> grantTypes;
    
    @JsonProperty("response_types")
    private List<String> responseTypes;
    
    @JsonProperty("scope")
    private String scope;
    
    @JsonProperty("token_endpoint_auth_method")
    private String tokenEndpointAuthMethod;
    
    @JsonProperty("client_secret_expires_at")
    private Long clientSecretExpiresAt;
}

