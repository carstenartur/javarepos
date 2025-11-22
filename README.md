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
