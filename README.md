# 🧠 Noemys Open MCP Servers

Welcome to **Noemys Open MCP**, the open-source initiative by [Noemys Technologies](https://noemys.ai) to provide ready-to-use **Model Context Protocol (MCP)** servers for popular platforms and APIs — 100% open, composable, and compliant with the **MCP 2025-06-18** specification.

---

## 🌍 What is this repository?

This repo is a **hub of MCP servers**, each packaged as an independent directory and following a **strict template** to ensure interoperability, transparency, and easy contribution.

Each folder (e.g., `/jira`, `/s3`, `/onedrive`, `/github`, `/confluence`, `/notion`, etc.) represents an **MCP server** that can be installed in your LLM-based assistant (Noemys, EVA, Cursor, etc.) and exposes standardized tools via MCP JSON schema.

---

## ⚙️ MCP 2025-06-18 Compatibility

Every connector in this repository is fully aligned with the **MCP 2025-06-18 specification**, which includes:

- ✅ HTTP Streaming transport (replaces legacy stdio)
- ✅ OAuth 2.1 token flows (dynamic client registration)
- ✅ JSON Schema tool definitions
- ✅ Discovery endpoint (`/.well-known/mcp-server.json`)
- ✅ Tool metadata (`manifest.json`)
- ✅ Versioned specification (`specVersion: "2025-06-18"`)

---

## 🧱 Repository Structure

```bash
noemys-open-mcp/
│
├── TEMPLATE-MCP/                        # ✅ Official MCP Server Template (Spring Boot)
│   ├── pom.xml                          # Independent module pom
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/fr/noemys/template/
│   │   │   │   ├── TemplateMcpApplication.java
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   ├── model/
│   │   │   │   └── config/
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       └── logback-spring.xml
│   │   └── test/java/fr/noemys/template/
│   │
│   ├── Dockerfile
│   ├── env-template
│   └── README.md
│
├── s3-mcp/                              # AWS S3 / MinIO MCP Server
│   ├── pom.xml                          # Independent module pom
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/fr/noemys/s3/
│   │   │   │   ├── S3McpApplication.java
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   ├── model/
│   │   │   │   └── config/
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       └── logback-spring.xml
│   │   └── test/java/fr/noemys/s3/
│   │
│   ├── Dockerfile
│   ├── env-template
│   └── README.md
│
├── LICENSE
└── README.md

