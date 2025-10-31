# BoxLang Servlet Runtime - AI Coding Agent Instructions

## Project Overview

This is the **BoxLang Servlet Runtime** - a Jakarta Servlet 5.0 implementation that runs BoxLang code in servlet containers (Tomcat, Jetty, CommandBox). BoxLang is a modern dynamic JVM language that compiles to bytecode and supports multiple runtimes.

**Key Architecture**: This runtime bridges BoxLang Core (`boxlang-*.jar`) with the servlet world through `BoxLangServlet` and `BoxHTTPServletExchange`, enabling BoxLang templates (`.bx`, `.bxm`, `.cfm`, etc.) to run in servlet containers.

## Core Components

### Main Entry Points
- **`BoxLangServlet`** (`src/main/java/ortus/boxlang/servlet/BoxLangServlet.java`): Main servlet implementing Jakarta `Servlet` interface. Initializes `BoxRuntime` on startup, registers interceptors, and delegates to `WebRequestExecutor` for each request.
- **`BoxHTTPServletExchange`** (`src/main/java/ortus/boxlang/web/exchange/BoxHTTPServletExchange.java`): Implements `IBoxHTTPExchange` to adapt servlet request/response into BoxLang's HTTP abstraction layer.
- **`ServletMappingInterceptor`** (`src/main/java/ortus/boxlang/servlet/ServletMappingInterceptor.java`): Intercepts `onMissingMapping` events to resolve paths using servlet context's `getRealPath()` - handles `..` paths and web root mappings.

### Dependency Model
This project does **NOT** use standard Maven dependencies for BoxLang core. Instead:
- **Local Development**: Checks for `../boxlang/build/libs/boxlang-{version}.jar` and `../boxlang-web-support/build/libs/` first
- **CI/Downloaded**: Falls back to `src/test/resources/libs/boxlang-{version}.jar`
- **Version Sync**: `gradle.properties` keeps `boxlangVersion` and `version` in sync (both bump together)

## Build System (Gradle)

### Critical Build Tasks
```bash
# Download BoxLang dependencies (MUST run before first build)
./gradlew downloadBoxLang

# Full build with Shadow JAR (includes service file merging)
./gradlew build

# Build complete CommandBox engine distribution (WAR + metadata)
./gradlew buildRuntime

# Integration tests with embedded Jetty server
./gradlew testServer

# Verify Shadow JAR service files merged correctly
./gradlew testServiceFileMerging
```

### Build Pipeline Architecture

The build follows a **multi-stage pipeline** orchestrated in `build.gradle`:

1. **`compileJava`** → Compile source code (JDK 21)
2. **`serviceLoaderBuild`** → Auto-generate `META-INF/services/` files for BIFs, Components, Interceptors
3. **`shadowJar`** → Create uber JAR with `mergeServiceFiles()` (CRITICAL for service loader pattern)
4. **`testServiceFileMerging`** → Validate service files were properly merged in Shadow JAR
5. **`build`** → Full build including tests (depends on shadowJar + testServiceFileMerging)
6. **`createWar`** → Assemble WAR structure in `build/engine/`:
   - Moves Shadow JAR to `WEB-INF/lib/`
   - Injects `web.xml` and `box.json` with token replacements (`@build.version@`)
   - Applies `-snapshot` suffix on `development` branch
7. **`packageWar`** → Package `build/engine/` into deployable WAR file
8. **`buildRuntime`** → Final distribution task:
   - Copies WAR to `build/forgebox/` (CommandBox engine format)
   - Generates SHA-256 and MD5 checksums for all `.jar`, `.war`, `.zip` artifacts
   - Creates evergreen aliases: `boxlang-servlet-snapshot.{war,jar}` (development) or `boxlang-servlet-latest.{war,jar}` (releases)

### Build Artifacts
- **Shadow JAR**: `build/distributions/boxlang-servlet-{version}.jar` - uber JAR with all dependencies merged
- **WAR file**: `build/distributions/boxlang-servlet-{version}.war` - deployable servlet archive with `WEB-INF/web.xml`
- **CommandBox Distribution**: `build/forgebox/` contains:
  - `boxlang-servlet-{version}.war`
  - `box.json` (CommandBox engine metadata)
- **Evergreen Snapshots**: `build/evergreen/` contains:
  - `boxlang-servlet-snapshot.war` (development branch)
  - `boxlang-servlet-latest.war` (release branches)
  - Checksums (`.sha-256`, `.md5`) for all artifacts

### Service Loader Pattern
Uses `com.github.harbby.gradle.serviceloader` plugin to auto-generate `META-INF/services/` files for:
- `ortus.boxlang.runtime.bifs.BIF`
- `ortus.boxlang.runtime.components.Component`
- `ortus.boxlang.runtime.events.IInterceptor`
- `ortus.boxlang.runtime.async.tasks.IScheduler`
- `ortus.boxlang.runtime.cache.providers.ICacheProvider`

**IMPORTANT**: Shadow JAR must use `mergeServiceFiles()` to combine service files from all dependencies. Test with `testServiceFileMerging` task.

### Token Replacement System
`build.gradle` uses Ant's `ReplaceTokens` filter to inject build metadata:
- `@build.version@` → `{version}+{buildID}` (CI) or `{version}` (local)
- `@build.date@` → Build timestamp in `yyyy-MM-dd HH:mm:ss` format
- Applied to: `META-INF/version.properties`, `web.xml`, `box.json`

### Specialized Gradle Tasks
- **`downloadBoxLang`**: Downloads BoxLang core JARs from `downloads.ortussolutions.com` when local siblings don't exist
- **`repackBoxLangJar`**: Removes conflicting `GetPageContext` classes from `boxlang-web-support` JAR (finalizer of `downloadBoxLang`)
- **`zipEngine`**: Creates ForgeBox-compatible `.zip` distribution
- **`javadoc` + `zipJavadocs`**: Generates and packages API docs
- **`bumpMajorVersion`, `bumpMinorVersion`, `bumpPatchVersion`**: Custom version bumping that updates BOTH `version` and `boxlangVersion` in `gradle.properties`

## Development Workflow

### Branch Strategy
- **`development`**: Active development branch (all PRs target here)
- **`master`**: Stable release snapshots only
- `-snapshot` suffix auto-applied on `development` branch

### Code Formatting
```bash
# Must run before committing (enforced by Spotless)
./gradlew spotlessApply

# Check formatting without applying
./gradlew spotlessCheck
```
- Java: Uses `.ortus-java-style.xml` Eclipse formatter
- Excludes: `build/**`, `bin/**`, `examples/**`

### Testing
- **Unit Tests**: `./gradlew test` (excludes `integration/**`)
- **Integration Tests**: `./gradlew testServer` deploys WAR to embedded Jetty and runs HTTP tests
- Test class location: `src/test/java/ortus/boxlang/servlet/`
- Uses JUnit 5 (`@Test`), Mockito, Google Truth

### Version Bumping
```bash
./gradlew bumpPatchVersion  # 1.7.0 → 1.7.1
./gradlew bumpMinorVersion  # 1.7.0 → 1.8.0
./gradlew bumpMajorVersion  # 1.7.0 → 2.7.0
```
Bumps **both** `version` and `boxlangVersion` in `gradle.properties`.

## Servlet Configuration

### web.xml Structure
Located in `src/main/resources/boxlang-servlet/web.xml`:
```xml
<servlet-class>ortus.boxlang.servlet.BoxLangServlet</servlet-class>
<!-- Mappings for BoxLang file extensions -->
<url-pattern>*.bx</url-pattern>
<url-pattern>*.bxm</url-pattern>
<url-pattern>*.cfm</url-pattern>
<url-pattern>*.cfc</url-pattern>
```

### Init Parameters (Optional)
- `boxlang-debug`: Enable debug mode (boolean)
- `boxlang-home`: Override default BoxLang home directory (absolute path)
- `boxlang-config-path`: Custom `boxlang.json` location (absolute path)

## Key Conventions

### Package Structure
- `ortus.boxlang.servlet.*`: Servlet-specific implementations
- `ortus.boxlang.web.*`: Web layer abstractions (exchange, BIFs)
  - `web.exchange`: HTTP exchange adapters
  - `web.bifs`: Web-specific Built-In Functions (e.g., `GetPageContext`)

### File Upload Handling
Uses Apache Commons FileUpload 2.0 (`DiskFileItemFactory`, `JakartaServletFileUpload`) for multipart/form-data. Exchange cleans up temp files in `BoxLangServlet.service()` finally block.

### Maven Publishing
- **Group ID**: `io.boxlang` (NOT `ortus.boxlang` for Sonatype Central)
- **Artifact ID**: `boxlang-servlet`
- Publishes to: Maven Central (via `nmcp` plugin), GitHub Packages
- Signing: Uses GPG in-memory keys from `GPG_KEY`/`GPG_PASSWORD` env vars (CI) or local gradle.properties

## Common Pitfalls

1. **Forgetting `downloadBoxLang`**: First build MUST run this task or you'll get missing BoxLang core JARs
2. **Service File Merging**: If adding new BIFs/Components, verify `testServiceFileMerging` passes
3. **Path Resolution**: Servlet's `getRealPath()` doesn't traverse above web root - `ServletMappingInterceptor` handles `..` specially
4. **Version Desync**: Always bump versions together with version bump tasks, not manually
5. **Branch-Specific Builds**: `development` branch auto-appends `-snapshot`, release branches don't

## Documentation & Resources

- **BoxLang Docs**: https://boxlang.ortusbooks.com/
- **Jira Issues**: https://ortussolutions.atlassian.net/browse/BL
- **Community**: https://community.ortussolutions.com/c/boxlang/42
- **Coding Standards**: https://github.com/Ortus-Solutions/coding-standards

## Publishing & CI/CD

- **GitHub Actions**: `.github/workflows/tests.yml` runs on Ubuntu/Windows with JDK 21
- **Evergreen Builds**: `development` creates `boxlang-servlet-snapshot.{war,jar}` in `build/evergreen/`
- **Checksums**: SHA-256 and MD5 auto-generated for all distribution artifacts
- **ForgeBox**: Distribution package in `build/forgebox/` ready for CommandBox publishing
