---
name: neo4j-installer
description: Installs Neo4j Community Edition on Windows from a zip archive and provides configuration for Neo4j tools.
---

# Neo4j Community Edition Installer and Configurator

This skill installs Neo4j Community Edition on a Windows machine and provides configuration for Neo4j `mcp` tools.

## Installation

This skill uses a PowerShell script to download and extract Neo4j.

### Script

`scripts/install-neo4j-community.ps1`

### How to Run

Open a PowerShell terminal and run the following command:

```powershell
./scripts/install-neo4j-community.ps1
```

### Optional Parameters

- `-InstallDir <path>`: Specifies the installation directory.
  - Default: `C:\neo4j-community-5.6.0`
  - Example: `.\scripts\install-neo4j-community.ps1 -InstallDir "C:\my-neo4j"`

## Neo4j Configuration

The following configuration can be used for `mcp_neo4j_cypher`, `mcp_neo4j_memory`, and `mcp_neo4j_data_modeling` tools.

### Configuring Environment Variables for MCP Tools

This skill provides a script to configure your current PowerShell session with Neo4j connection details, which can be used by `mcp` tools.

#### Connecting to the Remote Instance

To connect to the remote Neo4j instance (default), run the following command:
```powershell
./scripts/configure-neo4j-env.ps1
```
This sets the environment variables based on the values in `GEMINI.md`.

#### Connecting to the Local Instance

To connect to the local Neo4j instance that you installed with this skill, use the `-UseLocalInstance` switch. You will also need to provide the password for your local instance.

```powershell
./scripts/configure-neo4j-env.ps1 -UseLocalInstance -LocalPassword "your_local_password"
```

**Note:** For a new local installation, the default username is `neo4j` and the initial password is also `neo4j`. You will be prompted to change it on the first connection.

This script sets `NEO4J_URI`, `NEO4J_USERNAME`, and `NEO4J_PASSWORD` environment variables. These variables are temporary and apply only to the current PowerShell session.

### Remote Neo4j Instance
