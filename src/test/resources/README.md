# BoxLang Servlet Integration Tests

This directory contains integration tests that verify the BoxLang servlet WAR can be deployed and execute BoxLang templates successfully.

## Test Structure

- `ServletIntegrationTest.java` - Main integration test class
- `webroot/index.bxm` - Test BoxLang template that exercises various language features
- `logback-test.xml` - Logging configuration for embedded Jetty server

## Running the Integration Test

### Locally

```bash
# Build the project and run integration tests
./gradlew testServer

# Or run the full build including integration tests
./gradlew build testServer

# Clean build and run integration tests
./gradlew clean testServer
```

### In CI/CD

The integration test is automatically run as part of the GitHub Actions workflow in `tests.yml`.

## What the Test Does

1. **Builds a WAR file** from the current project using `buildRuntime` task
2. **Starts an embedded Jetty server** (Jakarta EE compatible)
3. **Deploys the WAR** to the embedded server with proper extraction
4. **Copies test resources** to the webapp directory
5. **Makes HTTP requests** to BoxLang templates
6. **Verifies responses** contain expected content and structure
7. **Tests BoxLang features** including:
   - Runtime introspection (components, BIFs, interceptors)
   - JSON serialization of BoxLang metadata
   - Servlet environment validation
   - Template execution verification

## Configuration

- **Server**: Embedded Jetty 11.x
- **Server Port**: 9999 (configurable in test)
- **Context Path**: `/boxlang-test`
- **Test Timeout**: 60 seconds per test method (with @Timeout annotation)
- **Startup Timeout**: 3-second initial wait + 10 connection attempts with 1-second delays
- **Logging**: Configured via `logback-test.xml` to reduce verbose HTTP output

## Test Task Configuration

The `testServer` task in `build.gradle` is specifically configured for integration testing:

```gradle
task testServer(type: Test) {
    description = 'Runs integration tests by deploying the WAR to an embedded server'
    group = 'verification'

    // Only run integration tests
    include '**/integration/ServletIntegrationTest.class'

    // Build WAR first
    dependsOn buildRuntime

    // Clean, focused logging
    testLogging {
        showStandardStreams = true
        events 'passed', 'skipped', 'failed', 'standardOut', 'standardError'
        exceptionFormat = 'short'
        showExceptions = true
        showCauses = true
        showStackTraces = false
    }
}
```

## Logging Configuration

The test uses `logback-test.xml` to provide clean output by:
- Setting Jetty loggers to WARN level to reduce HTTP noise
- Keeping essential server lifecycle events at INFO level
- Maintaining clear test output visibility

## Troubleshooting

If tests fail:

1. **WAR Build Issues**: Ensure `./gradlew buildRuntime` succeeds
2. **Dependency Problems**: Check that all required dependencies are available
3. **Server Startup**: Review Jetty initialization logs for specific errors
4. **Port Conflicts**: Ensure port 9999 is available (tests will show connection errors)
5. **File Permissions**: Verify temp directory access for WAR extraction
6. **Resource Copying**: Check that test resources are properly copied to webapp
7. **Response Validation**: Review HTTP response status and content in test output

## Test Output

The integration test provides clean, focused output showing:
- ✅ Server startup confirmation
- ✅ WAR deployment status
- ✅ BoxLang servlet initialization
- ✅ Test resource copying
- ✅ HTTP response validation
- ✅ Test results with clear pass/fail indicators
