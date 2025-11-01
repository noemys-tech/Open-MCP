# MCP S3 Server

MCP (Model Context Protocol) server for S3/MinIO implemented in Java 21 with Spring Boot, compliant with **MCP 2025-06-18** protocol with HTTP Streaming and OAuth 2.1.

## 🚀 Features

- ✅ **MCP 2025-06-18 Protocol** (latest version)
- ✅ **HTTP Streaming** with chunked transfer encoding
- ✅ **OAuth 2.1** with strict client validation
- ✅ **Production-ready**: no anonymous sessions, authentication required
- ✅ **Secure JWT** with configurable fixed key
- ✅ **Java 21** with Spring Boot and Maven
- ✅ **Complete S3/MinIO tools**: listBuckets, listObjects, downloadObject, getObjectMetadata
- ✅ **Compatible** with AWS S3, MinIO, and any S3-compatible service

## 📋 Prerequisites

- Java 21 or higher
- Maven 3.6 or higher
- Access to an S3/MinIO server

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

This generates the executable JAR in `target/s3-mcp-1.0.0.jar`.

### 3. Start the server

```bash
java -jar target/s3-mcp-1.0.0.jar
```

The server starts on port **9090** by default.

## 🔐 OAuth 2.1 - Production Mode

The server implements OAuth 2.1 with strict client validation.

### Step 1: Get OAuth metadata

```bash
curl http://localhost:9090/.well-known/oauth-authorization-server
```

### Step 2: Register a client

```bash
curl -X POST http://localhost:9090/oauth/register \
  -H "Content-Type: application/json" \
  -d '{
    "client_name": "my-s3-client",
    "redirect_uris": ["http://localhost:3000/callback"],
    "grant_types": ["authorization_code", "refresh_token", "client_credentials"]
  }'
```

Response (example):
```json
{
  "client_id": "client_abc12345",
  "client_secret": "secret_xyz67890",
  "client_name": "my-s3-client",
  ...
}
```

### Step 3: Get an access token

```bash
curl -X POST http://localhost:9090/oauth/token \
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
curl -X POST http://localhost:9090/mcp/session \
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
curl -X POST http://localhost:9090/mcp \
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
curl -X POST http://localhost:9090/mcp \
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
curl -X POST http://localhost:9090/mcp \
  -H "Mcp-Session-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "tools/call",
    "params": {
      "name": "listBuckets",
      "arguments": {
        "endpoint": "http://localhost:9000",
        "token": "minioadmin",
        "userToken": "minioadmin"
      }
    }
  }'
```

## 🛠️ Available Tools

### 1. sayHello
- **Description**: Returns "hello world" (demo tool)
- **Parameters**: None

### 2. listBuckets
- **Description**: Lists all S3 buckets
- **Parameters**:
  - `token` (string): S3 Access Key ID
  - `endpoint` (string): S3 server URL
  - `userToken` (string): S3 Secret Access Key

### 3. listObjects
- **Description**: Lists objects in a bucket
- **Parameters**:
  - `token` (string): S3 Access Key ID
  - `endpoint` (string): S3 server URL
  - `userToken` (string): S3 Secret Access Key
  - `bucketName` (string): Bucket name
  - `prefix` (string, optional): Prefix to filter objects

### 4. downloadObject
- **Description**: Downloads an object from S3
- **Parameters**:
  - `token` (string): S3 Access Key ID
  - `endpoint` (string): S3 server URL
  - `userToken` (string): S3 Secret Access Key
  - `bucketName` (string): Bucket name
  - `objectKey` (string): Object key

### 5. getObjectMetadata
- **Description**: Retrieves object metadata
- **Parameters**:
  - `token` (string): S3 Access Key ID
  - `endpoint` (string): S3 server URL
  - `userToken` (string): S3 Secret Access Key
  - `bucketName` (string): Bucket name
  - `objectKey` (string): Object key

## 📊 Health Check

```bash
curl http://localhost:9090/health
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
    "s3-mcp": {
      "url": "http://localhost:9090/mcp",
      "transport": "http",
      "headers": {
        "Authorization": "Bearer YOUR_ACCESS_TOKEN_HERE",
        "Content-Type": "application/json"
      },
      "description": "MCP S3 Server - Java Spring Boot"
    }
  }
}
```

**Note**: You must first obtain an access token via the OAuth flow described above.

## 🐳 Docker

### Build the image

```bash
docker build -t s3-mcp:1.0.0 .
```

### Run

```bash
docker run -d \
  -p 9090:9090 \
  -e JWT_SECRET="your_jwt_secret_here" \
  --name s3-mcp \
  s3-mcp:1.0.0
```

## 🧪 Testing with MinIO

### 1. Start MinIO locally

```bash
docker run -d \
  -p 9000:9000 \
  -p 9001:9001 \
  -e "MINIO_ROOT_USER=minioadmin" \
  -e "MINIO_ROOT_PASSWORD=minioadmin" \
  quay.io/minio/minio server /data --console-address ":9001"
```

### 2. Use S3 tools

Default MinIO credentials are:
- **token** (Access Key): `minioadmin`
- **userToken** (Secret Key): `minioadmin`
- **endpoint**: `http://localhost:9000`

## 🔒 Production Security

This server is configured in **production mode** with:

✅ **Strict OAuth client validation**: Unregistered clients are rejected
✅ **No anonymous sessions**: Authentication is required
✅ **JWT with fixed key**: Configured via application.properties or environment variable
✅ **In-memory storage**: Simple but volatile (consider a database for scalability)
✅ **S3 Credentials**: Passed via tool parameters (not stored)

### Production Checklist

- [ ] Generate and configure a secure JWT secret (min 32 characters)
- [ ] Use HTTPS in production (reverse proxy nginx/traefik)
- [ ] Configure rate limiting
- [ ] Set up monitoring and centralized logs
- [ ] Consider a database for clients/sessions (PostgreSQL, Redis)
- [ ] Configure backup and restore
- [ ] Load test and adjust resources
- [ ] Secure S3 credentials (never log keys)

## 📚 MCP 2025-06-18 Protocol

### Reference

- Model Context Protocol: https://spec.modelcontextprotocol.io/
- RFC 8414 (OAuth Metadata): https://www.rfc-editor.org/rfc/rfc8414
- RFC 7591 (Client Registration): https://www.rfc-editor.org/rfc/rfc7591
- AWS S3 API: https://docs.aws.amazon.com/s3/

## 📝 Changelog

### Version 1.0.0
- MCP S3 server compliant with MCP 2025-06-18
- OAuth 2.1 with strict client validation
- HTTP Streaming
- Complete S3 tools: listBuckets, listObjects, downloadObject, getObjectMetadata
- Production mode with secure JWT
- No anonymous sessions
- Compatible with AWS S3, MinIO and S3-compatible services

## 📄 License

This project is licensed under the MIT License.

