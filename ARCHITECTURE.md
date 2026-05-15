# Audio Platform — Architecture

## Overview

This project has been refactored from a single-purpose Swing waveform demo into a layered
real-time audio processing platform. The new architecture cleanly separates audio acquisition,
buffering, DSP processing, analysis and visualization, and provides extension points for future
modules (spectrograms, phase scopes, triggered oscilloscopes, recording/replay, plug-in DSP,
alternative UIs, ...).

## Layered architecture

```
┌──────────────────────┐
│   audio device       │   javax.sound.sampled.TargetDataLine
│   (microphone, ...)  │
└──────────┬───────────┘
           │ raw PCM bytes
           ▼
┌──────────────────────────────────────────┐
│  capture                                 │
│   AudioCaptureService(Impl)              │
│     ├─ SampleDecoder  (bytes -> float[])│
│     └─ produces AudioBlock (immutable)  │
└──────────┬───────────────────────────────┘
           │ AudioBlock
           ▼
┌──────────────────────────────────────────┐
│  buffer                                  │
│   AudioRingBuffer<AudioBlock>            │
│   (lock-free SPSC, bounded)              │
└──────────┬───────────────────────────────┘
           │ AudioBlock (consumer thread)
           ▼
┌──────────────────────────────────────────┐
│  dsp                                     │
│   DSPPipeline = [DSPProcessor, ...]      │
│   stateless, immutable                   │
└──────────┬───────────────────────────────┘
           │ AudioBlock
           ▼
┌──────────────────────────────────────────┐
│  analysis                                │
│   AnalysisModule<S extends Snapshot>     │
│     ├─ RmsPeakAnalyzer  -> RmsPeakSnapshot│
│     └─ SpectrumAnalyzer -> SpectrumSnapshot│
│       (uses pure-Java radix-2 Fft)       │
└──────────┬───────────────────────────────┘
           │ Snapshot (immutable)
           ▼
┌──────────────────────────────────────────┐
│  snapshot                                │
│   WaveformSnapshot                       │
│   PhaseScopeSnapshot                     │
│   (UI-friendly, audio-domain only)       │
└──────────┬───────────────────────────────┘
           │ snapshot (or block)
           ▼
┌──────────────────────────────────────────┐
│  ui                                      │
│   WaveformRenderer                       │
│   (the only place that knows pixels)     │
│   Existing Swing panels consume this.    │
│   Future: JavaFX / Web / REST.           │
└──────────────────────────────────────────┘
```

The key invariant: **everything below `ui` stays in normalized `float` audio space.**
No DSP/analysis/buffer code knows about pixels, panel dimensions, Swing or JavaFX.

## Packages

|           Package            |                                   Responsibility                                   |
|------------------------------|------------------------------------------------------------------------------------|
| `org.hammer.audio.core`      | Immutable audio-domain models: `AudioBlock`, `AudioFormatDescriptor`               |
| `org.hammer.audio.capture`   | Sample decoding (`SampleDecoder`); JavaSound bridging in `AudioCaptureServiceImpl` |
| `org.hammer.audio.buffer`    | `AudioRingBuffer<T>` — bounded lock-free SPSC ring buffer                          |
| `org.hammer.audio.dsp`       | `DSPProcessor` extension point + `DSPPipeline` composition                         |
| `org.hammer.audio.analysis`  | `AnalysisModule`, `AnalysisSnapshot`, `Fft`, `RmsPeakAnalyzer`, `SpectrumAnalyzer` |
| `org.hammer.audio.signal`    | Deterministic synthetic generators: `Sine`, `Square`, `Chirp`                      |
| `org.hammer.audio.snapshot`  | UI-friendly immutable snapshots: `WaveformSnapshot`, `PhaseScopeSnapshot`          |
| `org.hammer.audio.ui`        | `WaveformRenderer` — the only pixel-aware code                                     |
| `org.hammer.audio` (legacy)  | `WaveformModel`, `AudioCaptureService(Impl)` (kept for back-compat)                |
| `org.hammer.audio.benchmark` | JMH benchmarks (ring buffer, FFT, signal generators)                               |

## Key design choices

### 1. Immutable audio domain

`AudioFormatDescriptor` and `AudioBlock` are immutable, thread-safe and free of any UI or
JavaSound types. Samples are normalized `float[channels][frames]` in `[-1, 1]`. Each block
carries a monotonic `frameIndex` and a `timestampNanos` so any downstream consumer (analysis,
recording, replay) can correlate it back to the source.

### 2. Lock-free SPSC ring buffer

The audio capture thread is the sole producer; downstream DSP/analysis is the sole consumer.
`AudioRingBuffer` uses two `AtomicLong` sequences with `lazySet` semantics and a power-of-two
mask, avoiding locks on the hot path. The capacity is rounded up to the next power of two.
Two write strategies are exposed:

- `offer(T)` — fail fast if full (caller can decide what to do).
- `offerOverwrite(T)` — drop the oldest element if full (typical for "latest wins" UI feeds).

### 3. Composable DSP pipeline

`DSPProcessor` is a single-method functional interface (`AudioBlock -> AudioBlock`). Pipelines
are immutable lists of stages, threaded sequentially. Plug-in DSP modules implement
`DSPProcessor` (or `AnalysisModule` if they produce a snapshot rather than another block).

### 4. Pure-Java FFT

`Fft` is a dependency-free in-place radix-2 Cooley-Tukey FFT with cached twiddle and
bit-reverse tables. The architecture is more important than absolute FFT performance: callers
who want vectorized or native acceleration can plug in an alternative implementation behind a
custom `AnalysisModule` while keeping the rest of the platform unchanged.

### 5. Deterministic synthetic signals

`SineGenerator`, `SquareGenerator` and `ChirpGenerator` produce repeatable `AudioBlock` streams
with no audio device. They underpin every DSP/analysis test in this PR and enable headless
demos and CI smoke tests.

### 6. UI-only pixel scaling

`WaveformRenderer` is the single place that converts a `WaveformSnapshot` into pixel-space
arrays for a Swing canvas. The legacy `WaveformModel` is now built from a `WaveformSnapshot`
via the renderer, so existing UI code keeps working unchanged.

## Capture lifecycle

```
start()
   │
   ├─ open TargetDataLine
   ├─ allocate decode buffers
   ├─ spawn worker thread (daemon, single-thread executor)
   │
 capture loop (worker):
   │  read raw bytes
   │  -> SampleDecoder.decode -> float[channels][frames]
   │  -> AudioBlock (frameIndex, timestamp)
   │  -> ringBuffer.offerOverwrite(block)
   │  -> latestBlock = block (volatile, for "latest" consumers)
   │  -> latestModel = WaveformRenderer(snapshot, panelWidth, panelHeight)
   │
stop()
   │
   ├─ flag running=false
   ├─ shutdownNow worker
   └─ close TargetDataLine
```

The legacy `WaveformModel` is still produced by the worker so existing Swing panels keep
working without modification. New consumers should prefer `getRingBuffer()` or
`getLatestBlock()`.

## Extension points

|                Want to add                |                           Implement                            |
|-------------------------------------------|----------------------------------------------------------------|
| New DSP stage (filter, gain, ...)         | `DSPProcessor`, plug into a `DSPPipeline`                      |
| New analyzer (loudness, correlation, ...) | `AnalysisModule<MySnapshot>` returning your immutable snapshot |
| New visualization                         | New `Snapshot` + new renderer in `org.hammer.audio.ui`         |
| Alternative FFT backend                   | Replace `SpectrumAnalyzer`'s internal `Fft` with your own      |
| Recording / replay                        | New `DSPProcessor` (writer) and a `SignalGenerator` (reader)   |
| Headless demo / test                      | Use `SignalGenerator` instead of `AudioCaptureServiceImpl`     |

## Concurrency model

- Capture worker thread (single, daemon) — sole producer for the ring buffer.
- DSP / analysis threads — single consumer per ring buffer (SPSC).
- UI threads — read `latestBlock` / `latestModel` via volatile pointers; never mutate.
- Snapshots are immutable — safe to pass between threads without synchronization.

## Build, test, benchmark

```
./mvnw verify           # spotless, build, unit tests, spotbugs, pmd, checkstyle
./mvnw test             # unit tests only
./mvnw -Pjmh package    # JMH benchmarks (org.hammer.audio.benchmark.*)
```

See [`docs/MIGRATION.md`](docs/MIGRATION.md) for migration notes from the previous
`WaveformModel`-centric architecture.
