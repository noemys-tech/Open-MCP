package fr.noemys.s3.model.oauth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OAuth 2.1 Authorization Server Metadata (RFC 8414)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuthMetadata {
    
    @JsonProperty("issuer")
    private String issuer;
    
    @JsonProperty("authorization_endpoint")
    private String authorizationEndpoint;
    
    @JsonProperty("token_endpoint")
    private String tokenEndpoint;
    
    @JsonProperty("registration_endpoint")
    private String registrationEndpoint;
    
    @JsonProperty("jwks_uri")
    private String jwksUri;
    
    @JsonProperty("response_types_supported")
    private List<String> responseTypesSupported;
    
    @JsonProperty("grant_types_supported")
    private List<String> grantTypesSupported;
    
    @JsonProperty("token_endpoint_auth_methods_supported")
    private List<String> tokenEndpointAuthMethodsSupported;
    
    @JsonProperty("scopes_supported")
    private List<String> scopesSupported;
    
    @JsonProperty("code_challenge_methods_supported")
    private List<String> codeChallengeMethodsSupported;
}

