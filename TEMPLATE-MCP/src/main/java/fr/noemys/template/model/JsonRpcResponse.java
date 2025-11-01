package fr.noemys.template.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JSON-RPC 2.0 Response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonRpcResponse {
    
    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";
    
    @JsonProperty("id")
    private Object id;
    
    @JsonProperty("result")
    private Object result;
    
    @JsonProperty("error")
    private JsonRpcError error;
    
    public static JsonRpcResponse success(Object id, Object result) {
        JsonRpcResponse response = new JsonRpcResponse();
        response.setJsonrpc("2.0");
        response.setId(id);
        response.setResult(result);
        return response;
    }
    
    public static JsonRpcResponse error(Object id, int code, String message) {
        JsonRpcResponse response = new JsonRpcResponse();
        response.setJsonrpc("2.0");
        response.setId(id);
        response.setError(new JsonRpcError(code, message, null));
        return response;
    }
}

