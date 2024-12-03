# OWASP Dependency Source Code Retriever

This Kotlin application retrieves source code for modules identified by OWASP Dependency Check as having vulnerabilities.

## Features

- Parses OWASP Dependency Check JSON reports
- Retrieves source code from Maven Central Repository
- Attempts to find GitHub repositories for non-Maven dependencies
- Downloads source JAR files when available
- Handles both Maven and non-Maven dependencies

## Usage

```bash
./gradlew run --args="path/to/dependency-check-report.json [output-directory]"
```

The output directory is optional and defaults to "downloaded-sources".

## Requirements

- JDK 11 or higher
- Gradle 7.x or higher
- Internet connection for downloading sources

## Project Structure

- `Main.kt`: Entry point of the application
- `DependencyCheckParser.kt`: Parses OWASP Dependency Check reports
- `SourceCodeRetriever.kt`: Handles source code retrieval
- `model/`: Data classes for dependencies and vulnerabilities
- `util/`: Utility functions for file operations

## Building

```bash
./gradlew build
```

## Running Tests

```bash
./gradlew test
```