# Audio Capture & Threading

This page documents how to configure audio capture, how the runtime is structured across threads, and how diagnostic logging is wired up.

## Audio Configuration

Audio capture is configured via constructor parameters in `AudioCaptureServiceImpl`:

|     Parameter      |   Type    |                              Description                              |
|--------------------|-----------|-----------------------------------------------------------------------|
| `sampleRate`       | `float`   | Sample rate in Hz (e.g. `16000.0f`, `44100.0f`).                      |
| `sampleSizeInBits` | `int`     | Bits per sample (e.g. `8`, `16`).                                     |
| `channels`         | `int`     | `1` = mono, `2` = stereo.                                             |
| `signed`           | `boolean` | `true` for signed samples, `false` for unsigned.                      |
| `bigEndian`        | `boolean` | Byte order for multi-byte samples.                                    |
| `divisor`          | `int`     | Buffer size divisor (≥ 1). Buffer = `line.getBufferSize() / divisor`. |

Higher sample rates give better frequency resolution. The capture buffer size is `line.getBufferSize() / divisor`, so a larger `divisor` yields smaller buffers (lower latency, higher CPU) while a smaller `divisor` yields larger buffers (higher latency, more efficient). Higher sample rates may require larger buffers to maintain the same time window of audio.

Example — CD-quality mono:

```java
AudioCaptureServiceImpl service = new AudioCaptureServiceImpl(
    44100.0f,  // sampleRate
    16,        // sampleSizeInBits
    1,         // channels
    true,      // signed
    false,     // little-endian
    8          // divisor
);
```

## Threading Model

The audio service uses a multi-threaded architecture with strict concurrency control.

- **Main / EDT threads** — service lifecycle (`start()`, `stop()`), configuration (`setDivisor()`, `recomputeLayout()`), UI rendering (`WaveformPanel`, `PhaseDiagramPanel`).
- **Worker thread** — runs `captureLoop()` continuously, reads audio data, and updates the model.

### Synchronization

1. **`AtomicBoolean running`** — lock-free flag for service state, checked by the worker thread, set atomically by `start()` / `stop()`.
2. **`ReentrantLock modelLock`** — protects mutable state (`datas`, `xPoints`, `yPoints`, `tickEveryNSample`, `datasize`, `numberOfPoints`). Held only for the minimum time required to allocate or copy arrays.
3. **`volatile WaveformModel latestModel`** — immutable snapshot published by the worker. UI threads read it without acquiring locks.

### Immutable Snapshot Pattern

`WaveformModel` is immutable and thread-safe:

- All arrays are defensively copied during construction.
- Getters return defensive copies.
- Once published via `latestModel`, the object is never mutated.

This enables lock-free reads, non-blocking publishes, and consistent rendering snapshots (no torn updates).

### Best Practices When Modifying the Service

- Always acquire `modelLock` before mutating `xPoints`, `yPoints`, or related state.
- Allocate temporary arrays *outside* the lock and copy them in.
- Use `volatile` for simple state flags whose visibility matters.
- Prefer immutable objects for shared state to eliminate synchronization complexity.

## Performance Notes

The capture pipeline is optimized for real-time processing:

- **Buffer reuse** — the `datas` byte array is allocated once and reused across all capture iterations; the working `int[][]` (`workingYPoints`) is reused across loop iterations to eliminate per-iteration allocations.
- **Precomputed constants** — `bytesPerSample` and `frameSize` are computed once when the audio line opens.
- **Integer arithmetic** — `recomputeXValues()` distributes points using integer division (`(long) panelW * i / pointsM1`) instead of floating-point, avoiding `float` conversions and rounding overhead.
- **Narrow lock scope** — sample I/O and decoding happen outside `modelLock`; only model publication is inside.

These optimizations preserve external behavior while reducing GC churn and improving throughput.

## Logging & Observability

The application uses `java.util.logging` for diagnostics.

|   Level   |                               Used for                                |
|-----------|-----------------------------------------------------------------------|
| `INFO`    | Lifecycle events (service start/stop, audio line opened).             |
| `FINE`    | Detailed diagnostics (data size computation, capture loop lifecycle). |
| `WARNING` | Recoverable errors (line already running, line close errors).         |
| `SEVERE`  | Critical failures (capture init failure, capture loop exceptions).    |

### Enabling FINE Logging

```bash
java -Djava.util.logging.config.file=logging.properties \
     -jar audio-app/target/audio-app-0.0.1-SNAPSHOT.jar
```

`logging.properties`:

```properties
.level=INFO
org.hammer.audio.AudioCaptureServiceImpl.level=FINE
handlers=java.util.logging.ConsoleHandler
java.util.logging.ConsoleHandler.level=FINE
java.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter
```

> Avoid enabling `FINE` in production; it generates substantial output during real-time capture.
