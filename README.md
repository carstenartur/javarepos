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
  - **Checkstyle**: Custom configuration based on Google Java Style
  - **SpotBugs**: Bug pattern detection with GUI-specific exclusions
  - **PMD**: Curated ruleset for best practices and code quality
- **Artifacts**: Test reports and static analysis results are available as build artifacts

### Security Scanning

- **CodeQL**: Automated security scanning runs on push and pull requests to detect vulnerabilities

For more details, see the workflow files in `.github/workflows/`.

## Static Analysis

The project uses multiple static analysis tools to maintain code quality. All tools are configured to report issues without blocking the build (`failOnViolation=false`).

### Configuration Files

- `checkstyle.xml` - Custom Checkstyle rules based on Google Java Style with relaxed rules for GUI/audio contexts
- `checkstyle-suppressions.xml` - Suppressions for GUI-generated code and Swing patterns
- `spotbugs-exclude.xml` - Exclusions for GUI/serialization false positives
- `pmd-ruleset.xml` - Curated PMD rules focusing on best practices and performance

### Running Static Analysis

Run all static analysis tools as part of the standard build:

```bash
mvn clean verify
```

Run individual tools:

```bash
# Checkstyle - code style checks
mvn checkstyle:check

# SpotBugs - bug pattern detection
mvn spotbugs:check

# PMD - source code analyzer
mvn pmd:check
```

### Viewing Reports

After running `mvn verify`, reports are available in the `target/` directory:

- **Checkstyle**: `target/checkstyle-result.xml`
- **SpotBugs**: `target/spotbugsXml.xml` (detailed XML)
- **PMD**: `target/pmd.xml`

### Contributing Guidelines

When contributing code:

1. **Format First**: Run `mvn spotless:apply` before committing to ensure proper code formatting
2. **Review Warnings**: Check static analysis output for new warnings introduced by your changes
3. **High Severity Issues**: Fix any high-severity issues flagged by SpotBugs or PMD
4. **Low Severity Issues**: Low-severity warnings may be deferred but should be noted in PR comments

The CI build will run all checks automatically and upload reports as artifacts.
