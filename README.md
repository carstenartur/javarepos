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

### Using Maven Wrapper (Recommended)

The project includes a Maven Wrapper (mvnw) that automatically downloads and uses the correct Maven version (3.9.9), eliminating the need to install Maven manually:

```bash
./mvnw clean verify
```

On Windows:

```cmd
mvnw.cmd clean verify
```

### Using System Maven

If you prefer to use your system-installed Maven:

```bash
mvn clean verify
```

### Reproducible Builds

This project is configured for reproducible builds. The JAR artifacts include a fixed timestamp (`project.build.outputTimestamp`) ensuring that repeated builds with the same source code produce byte-for-byte identical artifacts. This improves build verification and supply chain security.

To verify reproducibility:

```bash
./mvnw clean package
sha256sum target/audioin-0.0.1-SNAPSHOT.jar

# Build again
./mvnw clean package
sha256sum target/audioin-0.0.1-SNAPSHOT.jar
# Checksums should match
```

### Updating Maven Wrapper

To regenerate or update the Maven Wrapper:

```bash
mvn -N wrapper:wrapper -Dmaven=3.9.9
```

### Dependency Management

This project uses JUnit BOM (Bill of Materials) for consistent JUnit dependency version management. JUnit dependencies are declared without explicit versions, inheriting from the BOM instead.

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
- **Code Coverage**: JaCoCo reports are generated and uploaded to Codecov (current line coverage: ~9%; target threshold configured at 30% in JaCoCo rule)
- **Static Analysis**: The build includes:
  - **Checkstyle**: Custom configuration based on Google Java Style
  - **SpotBugs**: Bug pattern detection with GUI-specific exclusions
  - **PMD**: Curated ruleset for best practices and code quality
- **Artifacts**: Test reports and static analysis results are available as build artifacts

### Security Scanning

- **CodeQL**: Automated security scanning runs on push and pull requests to detect vulnerabilities

For more details, see the workflow files in `.github/workflows/`.

## Tests

### Viewing JUnit Test Reports

JUnit test results are automatically uploaded as build artifacts in GitHub Actions, making it easy to review test outcomes and debug failures:

**Available Artifacts:**

- **junit-xml**: JUnit XML reports (`.xml` files) containing structured test results
- **surefire-raw**: Raw Surefire console output (`.txt` files) and thread dumps (`.dump` files) for detailed test logs
- **surefire-html**: HTML-formatted test summary report (when successfully generated)

**How to Access:**

1. Navigate to the [Actions tab](https://github.com/carstenartur/javarepos/actions/workflows/maven.yml) in the repository
2. Select a workflow run
3. Scroll to the "Artifacts" section at the bottom of the page
4. Download the desired artifact(s)

**Pull Request Checks:**

When test-reporter is integrated, an additional "JUnit Tests" check appears on pull requests, providing a quick summary of passed/failed tests directly in the PR interface without needing to download artifacts.

**Code Coverage:**

For code coverage metrics, refer to the [![codecov](https://codecov.io/gh/carstenartur/javarepos/graph/badge.svg)](https://codecov.io/gh/carstenartur/javarepos) badge above or visit the [Codecov dashboard](https://codecov.io/gh/carstenartur/javarepos).

## Static Analysis

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

## Performance & Benchmarking

The project implements several performance optimizations for real-time audio processing:

### Key Optimizations

- **Buffer Reuse**: The `datas` byte array is allocated once during initialization and reused across all capture iterations, eliminating repeated allocation overhead
- **Integer Arithmetic**: X-coordinates are computed using integer scaling (`Math.round()`) rather than floating-point operations throughout the rendering pipeline
- **Precomputed Values**: Frame size (`bytesPerSample * channels`) and bytes per sample are computed once per capture session and cached as local variables
- **Working Arrays**: Temporary `tmpY` arrays are allocated per iteration but kept minimal, holding only the current frame's processed samples

### Performance Characteristics

Current implementation processes audio samples in real-time with minimal latency:
- Buffer size is dynamically computed: `line.getBufferSize() / divisor` (minimum 256 bytes)
- Samples are read and processed in a tight loop without blocking operations
- Model updates are atomic (protected by `modelLock`) but fast due to efficient `System.arraycopy`

### Future Benchmarking

While JMH (Java Microbenchmark Harness) is not currently integrated, contributors can add benchmarks for:
- `readSample()` variants (8-bit, 16-bit, big-endian vs little-endian)
- `scaleToPixel()` with different panel heights
- Full capture loop throughput under various sample rates

To add JMH benchmarks, include the JMH dependency and create a `benchmark` profile in `pom.xml`.

## Logging & Observability

The application uses `java.util.logging` for diagnostic output.

### Log Levels

- **INFO**: Lifecycle events (service start/stop, audio line opened)
- **FINE**: Detailed diagnostics for repainting and resizing (data size computation, capture loop lifecycle)
- **WARNING**: Recoverable errors (line already running, line close errors)
- **SEVERE**: Critical failures (capture initialization failure, capture loop exceptions)

### Enabling FINE Logging

To enable detailed logging for diagnostics (e.g., tracking repaint frequency or resize events):

```bash
# Using system property
java -Djava.util.logging.config.file=logging.properties -jar target/audioin-0.0.1-SNAPSHOT.jar
```

Create `logging.properties`:

```properties
# Set root logger to INFO
.level=INFO

# Enable FINE logging for audio capture service
org.hammer.audio.AudioCaptureServiceImpl.level=FINE

# Console handler configuration
handlers=java.util.logging.ConsoleHandler
java.util.logging.ConsoleHandler.level=FINE
java.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter
```

**Note**: Avoid enabling FINE logging in production, as it generates verbose output during real-time audio processing. Use it primarily for development and debugging.

## Audio Configuration

Audio capture is configured via constructor parameters in `AudioCaptureServiceImpl`. Understanding these parameters is essential for customizing audio behavior:

### Parameters

- **`sampleRate`** (float): Sample rate in Hz (e.g., 16000.0f, 44100.0f)
  - Higher rates = better frequency resolution but larger buffer sizes
  - Common values: 8000, 16000, 22050, 44100, 48000
- **`sampleSizeInBits`** (int): Bits per sample (e.g., 8, 16)
  - 8-bit: unsigned typically, range 0-255
  - 16-bit: signed typically, range -32768 to 32767
  - Affects dynamic range and audio quality
- **`channels`** (int): Number of audio channels (1 = mono, 2 = stereo)
  - Mono (1): Single waveform rendered
  - Stereo (2): Separate waveforms for left/right channels
- **`signed`** (boolean): Whether samples are signed integers
  - true: Signed representation (e.g., -32768 to 32767 for 16-bit)
  - false: Unsigned representation (e.g., 0 to 65535 for 16-bit)
- **`bigEndian`** (boolean): Byte order for multi-byte samples
  - true: Most significant byte first (network byte order)
  - false: Least significant byte first (Intel byte order, more common on PC)
- **`divisor`** (int): Buffer size divisor (≥1)
  - Computed buffer size = `line.getBufferSize() / divisor`
  - Larger divisor = smaller buffers, lower latency but higher CPU usage
  - Smaller divisor = larger buffers, higher latency but more efficient

### Example Configuration

```java
// CD-quality mono audio
AudioCaptureServiceImpl service = new AudioCaptureServiceImpl(
    44100.0f,  // sampleRate: 44.1 kHz
    16,        // sampleSizeInBits: 16-bit
    1,         // channels: mono
    true,      // signed: yes
    false,     // bigEndian: little-endian (PC standard)
    8          // divisor: medium buffer size
);
```

## Headless Testing

The project uses headless mode for GUI component testing in CI environments.

### CI Configuration

Tests run with `java.awt.headless=true` (configured in `pom.xml` under `maven-surefire-plugin`):

```xml
<systemPropertyVariables>
  <java.awt.headless>true</java.awt.headless>
</systemPropertyVariables>
```

### Workaround for Resize Events

In headless mode, Swing components don't automatically dispatch `ComponentEvent.COMPONENT_RESIZED` events. Tests that verify resize behavior manually dispatch events:

```java
// From WaveformPanelTest.java
SwingUtilities.invokeAndWait(() -> {
    panel.setSize(300, 150);
    // Manually dispatch ComponentEvent.COMPONENT_RESIZED to trigger componentResized
    panel.dispatchEvent(new ComponentEvent(panel, ComponentEvent.COMPONENT_RESIZED));
});
```

This pattern ensures resize listeners fire correctly in both headless CI and local GUI environments.

### Testing Best Practices

- Use `SwingUtilities.invokeAndWait()` for actions requiring EDT execution
- Mock `AudioCaptureService` with Mockito to isolate UI logic from audio hardware
- Manually dispatch component events when testing resize/repaint behavior
- Verify method invocations (e.g., `verify(svc).recomputeLayout(width, height)`) rather than visual output

## Improving Coverage

Current line coverage: ~9%. JaCoCo enforces a minimum threshold of 30% to pass verification.

### Target Areas for Increased Coverage

To reach and exceed the 30% threshold, focus on these high-impact areas:

1. **`readSample()` Variants**
   - Test 8-bit signed/unsigned sample reading
   - Test 16-bit signed/unsigned, big-endian and little-endian
   - Test fallback path for non-standard sample sizes
2. **`captureLoop()` Buffer Reuse**
   - Verify `datas` array is reused across multiple read cycles
   - Test `System.arraycopy` behavior with partial buffer fills
   - Validate atomic model updates under concurrent access
3. **`recomputeXValues()` Integer Arithmetic**
   - Test boundary conditions (panelWidth = 1, numberOfPoints = 1)
   - Verify even distribution of x-coordinates across panel width
   - Test edge case: numberOfPoints > panelWidth
4. **`scaleToPixel()` Edge Cases**
   - Test signed sample normalization with max/min values
   - Test unsigned sample scaling with boundary values
   - Test panelHeight = 0 (should return y = 0)
5. **Thread Safety and Locking**
   - Test concurrent calls to `setDivisor()` and `recomputeLayout()`
   - Verify `modelLock` prevents race conditions during model updates
   - Test `getLatestModel()` returns consistent snapshot under concurrent writes

### Adding Tests

Create focused unit tests in `src/test/java/org/hammer/audio/`:

```java
@Test
void readSample_8bitSigned_returnsCorrectValue() {
    // Test setup with 8-bit signed audio format
    // Verify sample value extraction
}

@Test
void recomputeXValues_evenDistribution_spansFullWidth() {
    // Verify integer arithmetic distributes points evenly
}
```

Run tests and check coverage:

```bash
mvn clean test
# View coverage report: target/site/jacoco/index.html
```

## Thread Safety

The audio capture service uses a multi-threaded architecture with strict concurrency control.

### Threading Model

- **Main Thread**: Handles service lifecycle (`start()`, `stop()`), configuration updates (`setDivisor()`, `recomputeLayout()`)
- **Worker Thread**: Runs `captureLoop()` continuously, reading audio data and updating the model
- **EDT (Event Dispatch Thread)**: Renders UI components (`WaveformPanel`, `PhaseDiagramPanel`)

### Synchronization Mechanisms

1. **`AtomicBoolean running`**: Lock-free flag for service state
   - Checked by worker thread to determine when to stop
   - Set atomically by `start()` and `stop()` methods
2. **`ReentrantLock modelLock`**: Protects mutable model state
   - Guards: `datas`, `xPoints`, `yPoints`, `tickEveryNSample`, `datasize`, `numberOfPoints`
   - Acquired during: `computeDataSize()`, `recomputeXValues()`, model updates in `captureLoop()`
   - Lock duration: Minimal, only during array allocation and copy operations
3. **`volatile WaveformModel latestModel`**: Immutable snapshot for consumers
   - Worker thread publishes new model after each successful capture
   - UI threads read model without blocking worker thread
   - Immutability eliminates need for defensive copies or locking in readers

### Immutable Snapshot Pattern

`WaveformModel` is **immutable** and **thread-safe**:
- All arrays are defensively copied during construction
- Getters return defensive copies, preventing external modification
- Once published via `latestModel`, the object is never mutated

This design allows:
- **Lock-free reads**: `getLatestModel()` reads volatile reference without acquiring locks
- **Non-blocking updates**: Worker thread publishes new model without waiting for UI threads
- **Consistency**: UI always renders a complete, consistent snapshot (no partial updates visible)

### Concurrency Best Practices

When modifying the service:
- Always acquire `modelLock` before modifying `xPoints`, `yPoints`, or related state
- Keep lock-holding duration minimal (allocate temporary arrays outside lock, copy inside)
- Use `volatile` for simple state flags (`latestModel`) to ensure visibility across threads
- Prefer immutable objects (`WaveformModel`) for shared state to eliminate synchronization complexity
