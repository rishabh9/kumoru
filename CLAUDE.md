# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Build and Test
- **Build**: `./gradlew clean build`
- **Run tests**: `./gradlew test`
- **Run application locally**: `./gradlew run`
- **Code formatting (Spotless)**: `./gradlew spotlessApply`
- **Check code style**: `./gradlew checkstyleMain checkstyleTest`

### Docker Operations
- **Build Docker image**: `./gradlew jibDockerBuild`
- **Run in Docker**: `docker run -p 8888:8888 -v /tmp/repo:/srv/repo rishabh9/kumoru`

### Development with Hot Reload
The `./gradlew run` command includes automatic redeployment on file changes in `src/**/*`.

## Architecture Overview

Kumoru is a lightweight Maven repository manager built with Vert.x that proxies artifacts from Maven Central, JCenter, and JitPack repositories.

### Core Components

**MainVerticle** (`src/main/java/com/github/rishabh9/kumoru/MainVerticle.java`)
- Entry point that deploys three main verticles
- Manages deployment lifecycle and scaling based on CPU cores

**WebServer Verticle** (`src/main/java/com/github/rishabh9/kumoru/web/WebServer.java`)
- Handles HTTP requests on port 8888
- Routes GET requests through a chain of handlers for artifact resolution
- Handles PUT requests for artifact uploads
- Scales to number of CPU cores as worker verticles

**Handler Chain Architecture** (`src/main/java/com/github/rishabh9/kumoru/web/handlers/`)
The request processing follows this handler chain:
1. `ValidRequestHandler` - validates incoming requests
2. `LocalResourceHandler` - checks local cache first
3. `RepositoryHandler` (multiple) - tries each configured repository in order
4. `SendFileHandler` - sends the resolved file
5. `FinalHandler` - handles 404s

**Snapshot Management**
- `SnapshotUpdateChecker` - periodic verticle that checks for snapshot updates every 12 hours
- `ArtifactDownloader` - worker verticle pool for downloading artifacts

### Configuration System

**KumoruConfig** (`src/main/java/com/github/rishabh9/kumoru/common/KumoruConfig.java`)
- Singleton configuration loaded from environment variables and `repositories.json`
- Key environment variables:
  - `KUMORU_PORT` (default: 8888)
  - `KUMORU_ACCESS_LOG` (default: false)
  - `KUMORU_BODY_LIMIT` (default: 50MB)

**Repository Configuration** (`src/main/resources/repositories.json`)
- Defines repository mirrors and snapshot repositories
- Supports basic auth and bearer token authentication
- Lookup order: Maven2, JCenter, JitPack

### Key Dependencies and Frameworks
- **Vert.x 3.9.1**: Reactive web framework and event bus
- **Log4j2**: Async logging with disruptor
- **Lombok**: Code generation for getters/setters
- **Jackson**: JSON parsing for configuration
- **Apache Tika**: MIME type detection
- **Aalto XML**: Fast XML parsing for Maven metadata

### File Structure
- `/srv/repo` - Local artifact cache directory (Docker volume mount)
- Request path directly maps to Maven repository structure
- Snapshots are automatically updated based on metadata timestamps

### Testing
- Uses JUnit 5 with Vert.x test extensions
- Test configuration mirrors main resources
- Key test: `MetadataAsyncXmlParserTest` for XML parsing