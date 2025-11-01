# MCP Template Server

MCP (Model Context Protocol) template server implemented in Java 21 with Spring Boot, compliant with **MCP 2025-06-18** protocol with HTTP Streaming and OAuth 2.1.

## 🚀 Features

- ✅ **MCP 2025-06-18 Protocol** (latest version)
- ✅ **HTTP Streaming** with chunked transfer encoding
- ✅ **OAuth 2.1** with strict client validation
- ✅ **Production-ready**: no anonymous sessions, authentication required
- ✅ **Secure JWT** with configurable fixed key
- ✅ **Java 21** with Spring Boot and Maven
- ✅ **Demo tool**: sayHello

## 📋 Prerequisites

- Java 21 or higher
- Maven 3.6 or higher

## 🔧 Installation and Build

### 1. Configure JWT Secret

**IMPORTANT**: You must configure a secure JWT secret before starting the server.

Generate a secure secret:

```bash
# Linux/Mac
openssl rand -base64 32

# PowerShell (Windows)
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

Configure the secret in `src/main/resources/application.properties`:

```properties
mcp.oauth.jwt-secret=YOUR_GENERATED_SECRET_HERE
```

Or via environment variable:

```bash
export JWT_SECRET="YOUR_GENERATED_SECRET_HERE"
```

### 2. Build the project

```bash
mvn clean package
```

This generates the executable JAR in `target/template-mcp-1.0.0.jar`.

### 3. Start the server

```bash
java -jar target/template-mcp-1.0.0.jar
```

The server starts on port **9091** by default.

## 🔐 OAuth 2.1 - Production Mode

The server implements OAuth 2.1 with strict client validation.

### Step 1: Get OAuth metadata

```bash
curl http://localhost:9091/.well-known/oauth-authorization-server
```

### Step 2: Register a client

```bash
curl -X POST http://localhost:9091/oauth/register \
  -H "Content-Type: application/json" \
  -d '{
    "client_name": "my-client",
    "redirect_uris": ["http://localhost:3000/callback"],
    "grant_types": ["authorization_code", "refresh_token", "client_credentials"]
  }'
```

Response (example):
```json
{
  "client_id": "client_abc12345",
  "client_secret": "secret_xyz67890",
  "client_name": "my-client",
  ...
}
```

### Step 3: Get an access token

```bash
curl -X POST http://localhost:9091/oauth/token \
  -H "Content-Type: application/json" \
  -d '{
    "grant_type": "client_credentials",
    "client_id": "client_abc12345",
    "client_secret": "secret_xyz67890",
    "scope": "mcp"
  }'
```

Response:
```json
{
  "access_token": "eyJhbGc...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_token": "refresh_...",
  "scope": "mcp"
}
```

### Step 4: Create an MCP session

```bash
curl -X POST http://localhost:9091/mcp/session \
  -H "Authorization: Bearer eyJhbGc..."
```

Response:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000"
}
```

## 🌐 HTTP Streaming - MCP Endpoints

### Initialize

```bash
curl -X POST http://localhost:9091/mcp \
  -H "Mcp-Session-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
      "protocolVersion": "2025-06-18",
      "capabilities": {},
      "clientInfo": {
        "name": "test-client",
        "version": "1.0.0"
      }
    }
  }'
```

### List tools

```bash
curl -X POST http://localhost:9091/mcp \
  -H "Mcp-Session-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/list"
  }'
```

### Call a tool

```bash
curl -X POST http://localhost:9091/mcp \
  -H "Mcp-Session-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "tools/call",
    "params": {
      "name": "sayHello",
      "arguments": {}
    }
  }'
```

## 🛠️ Available Tools

### 1. sayHello
- **Description**: Returns "hello world" (demo tool)
- **Parameters**: None

## 📊 Health Check

```bash
curl http://localhost:9091/health
```

Response:
```json
{
  "status": "UP",
  "version": "1.0.0",
  "protocol": "MCP 2025-06-18"
}
```

## 🔌 Integration with Cursor IDE

Create an `mcp-config.json` file in your project:

```json
{
  "mcpServers": {
    "template-mcp": {
      "url": "http://localhost:9091/mcp",
      "transport": "http",
      "headers": {
        "Authorization": "Bearer YOUR_ACCESS_TOKEN_HERE",
        "Content-Type": "application/json"
      },
      "description": "MCP Template Server - Java Spring Boot"
    }
  }
}
```

**Note**: You must first obtain an access token via the OAuth flow described above.

## 🐳 Docker

### Build the image

```bash
docker build -t template-mcp:1.0.0 .
```

### Run

```bash
docker run -d \
  -p 9091:9091 \
  -e JWT_SECRET="your_jwt_secret_here" \
  --name template-mcp \
  template-mcp:1.0.0
```

## 🔒 Production Security

This server is configured in **production mode** with:

✅ **Strict OAuth client validation**: Unregistered clients are rejected
✅ **No anonymous sessions**: Authentication is required
✅ **JWT with fixed key**: Configured via application.properties or environment variable
✅ **In-memory storage**: Simple but volatile (consider a database for scalability)

### Production Checklist

- [ ] Generate and configure a secure JWT secret (min 32 characters)
- [ ] Use HTTPS in production (reverse proxy nginx/traefik)
- [ ] Configure rate limiting
- [ ] Set up monitoring and centralized logs
- [ ] Consider a database for clients/sessions (PostgreSQL, Redis)
- [ ] Configure backup and restore
- [ ] Load test and adjust resources

## 📚 MCP 2025-06-18 Protocol

### Reference

- Model Context Protocol: https://spec.modelcontextprotocol.io/
- RFC 8414 (OAuth Metadata): https://www.rfc-editor.org/rfc/rfc8414
- RFC 7591 (Client Registration): https://www.rfc-editor.org/rfc/rfc7591

## 🛠️ Extending the Template

To add your own tools:

1. Open `src/main/java/fr/noemys/template/service/McpService.java`
2. Add your tool in `listTools()`:
   ```java
   McpTool myTool = McpTool.builder()
       .name("myTool")
       .description("My tool description")
       .inputSchema(Map.of(
           "type", "object",
           "properties", Map.of(
               "param1", Map.of("type", "string", "description", "...")
           ),
           "required", List.of("param1")
       ))
       .build();
   tools.add(myTool);
   ```
3. Add the handler in `callTool()`:
   ```java
   if ("myTool".equals(toolName)) {
       return executeMyTool(arguments);
   }
   ```
4. Implement the `executeMyTool()` method

## 📝 Changelog

### Version 1.0.0
- MCP template server compliant with MCP 2025-06-18
- OAuth 2.1 with strict client validation
- HTTP Streaming
- Demo tool sayHello
- Production mode with secure JWT
- No anonymous sessions

## 📄 License

This project is licensed under the MIT License.

