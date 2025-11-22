javarepos
=========

[![Java CI with Maven](https://github.com/carstenartur/javarepos/actions/workflows/maven.yml/badge.svg)](https://github.com/carstenartur/javarepos/actions/workflows/maven.yml)
[![codecov](https://codecov.io/gh/carstenartur/javarepos/graph/badge.svg)](https://codecov.io/gh/carstenartur/javarepos)
[![CodeQL](https://github.com/carstenartur/javarepos/actions/workflows/codeql.yml/badge.svg)](https://github.com/carstenartur/javarepos/actions/workflows/codeql.yml)

audioanalyzer

## Project Structure

This project follows the standard Maven directory layout:
- `src/main/java` - Production source code
- `src/test/java` - Test source code

## Build & Test

This project requires **Java 21** or higher.

```bash
mvn clean verify
```

## Code Style

This project uses [Spotless](https://github.com/diffplug/spotless) with [google-java-format](https://github.com/google/google-java-format) to maintain consistent code formatting across all Java sources.

### Formatting Code

To format all code according to the project's style guidelines:

```bash
mvn spotless:apply
```

This will automatically format:
- Java source files (`src/main/java` and `src/test/java`) using google-java-format
- XML files (including `pom.xml`) with consistent indentation
- Markdown documentation files

### Checking Formatting

The build process includes a format check. To verify your code is properly formatted without applying changes:

```bash
mvn spotless:check
```

### Editor Configuration

The project includes an `.editorconfig` file with settings for:
- Character encoding (UTF-8)
- Line endings (LF)
- Indentation (4 spaces for Java, 2 spaces for XML/Markdown)
- Trailing whitespace removal
- Final newline insertion

Most modern IDEs and editors support EditorConfig automatically or via plugins.

## CI/CD

### Continuous Integration

The project uses GitHub Actions for continuous integration with the following features:

- **Multi-version Testing**: Build matrix includes Java 17 and 21
  - **Note**: Java 21 is required for successful builds (maven-enforcer-plugin requirement)
  - Java 17 jobs skip with informational notice
- **Dependency Caching**: Maven dependencies are cached to improve build times
- **Code Coverage**: JaCoCo reports are generated and uploaded to Codecov (current coverage: ~9%)
- **Static Analysis**: The build includes:
  - **Checkstyle**: Google Java Style checks
  - **SpotBugs**: Bug pattern detection
  - **PMD**: Source code analyzer
- **Artifacts**: Test reports and static analysis results are available as build artifacts

### Security Scanning

- **CodeQL**: Automated security scanning runs on push and pull requests to detect vulnerabilities

For more details, see the workflow files in `.github/workflows/`.

## Performance

### Audio Capture Optimizations

The audio capture implementation has been optimized to reduce runtime allocations and GC pressure during continuous audio processing:

- **Buffer Reuse**: The capture loop uses a reusable working buffer (`workingYPoints`) to eliminate per-iteration `int[][]` allocations.
- **Precomputed Constants**: Audio format fields (`bytesPerSample`, `frameSize`) are computed once when the audio line is opened, avoiding repeated calculations in the hot path.
- **Integer Arithmetic**: The `recomputeXValues` method uses integer arithmetic instead of floating-point operations to reduce conversion overhead.
- **Narrow Lock Scope**: Critical sections are minimized; I/O operations and sample decoding happen outside the model lock.

These optimizations maintain the same external behavior while improving throughput and reducing memory churn during live audio capture.

### Performance Benchmarking

An optional JMH (Java Microbenchmark Harness) profile is available for benchmarking sample decoding performance on synthetic buffers (without requiring an audio device).

To build and run the JMH benchmarks:

```bash
# Compile with JMH profile
mvn clean verify -Pjmh

# Run benchmarks
mvn exec:java -Pjmh
```

The JMH profile is **opt-in** and does not affect standard builds. Benchmarks are located in `src/jmh/java` and measure throughput for various audio format decoding paths (8-bit/16-bit, signed/unsigned, little/big endian).
