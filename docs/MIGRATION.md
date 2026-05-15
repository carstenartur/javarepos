# Migration notes — to the modular audio platform

This document describes how to move from the previous `WaveformModel`-centric architecture to
the new layered audio/DSP platform. **No source-level changes are required for existing
callers**: every previously public API still works. The notes below are for new code that
wants to take advantage of the new infrastructure.

## Package map

|                  Old location                  |                                              New location                                              |                                                      Notes                                                       |
|------------------------------------------------|--------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| `org.hammer.audio.AudioCaptureService(Impl)`   | unchanged                                                                                              | Now also exposes `getDescriptor()`, `getLatestBlock()`, `getRingBuffer()`. Pixel scaling no longer happens here. |
| `org.hammer.audio.WaveformModel`               | unchanged                                                                                              | Still produced for legacy Swing rendering. New code should prefer `WaveformSnapshot`.                            |
| pixel scaling inside `AudioCaptureServiceImpl` | `org.hammer.audio.ui.WaveformRenderer`                                                                 | Pure functions over `WaveformSnapshot`.                                                                          |
| (none — sample decoding was inlined)           | `org.hammer.audio.capture.SampleDecoder`                                                               | Stateless, thread-safe; reused by capture and tests.                                                             |
| (none)                                         | `org.hammer.audio.core.AudioFormatDescriptor`                                                          | Audio-domain format descriptor; UI/JavaSound-free.                                                               |
| (none)                                         | `org.hammer.audio.core.AudioBlock`                                                                     | Immutable block with normalized `float[][]` samples.                                                             |
| (none)                                         | `org.hammer.audio.buffer.AudioRingBuffer`                                                              | Lock-free SPSC ring buffer.                                                                                      |
| (none)                                         | `org.hammer.audio.dsp.{DSPProcessor, DSPPipeline}`                                                     | DSP extension points.                                                                                            |
| (none)                                         | `org.hammer.audio.analysis.{AnalysisModule, AnalysisSnapshot, Fft, RmsPeakAnalyzer, SpectrumAnalyzer}` | Analysis layer.                                                                                                  |
| (none)                                         | `org.hammer.audio.signal.{SineGenerator, SquareGenerator, ChirpGenerator}`                             | Deterministic synthetic signals.                                                                                 |
| (none)                                         | `org.hammer.audio.snapshot.{WaveformSnapshot, PhaseScopeSnapshot}`                                     | Immutable visualization snapshots.                                                                               |

## Behaviour changes

|             Aspect             |         Before          |                          After                           |
|--------------------------------|-------------------------|----------------------------------------------------------|
| Internal sample representation | int pixel coordinates   | normalized `float[channels][frames]`                     |
| Audio capture publishes to     | `WaveformModel` only    | `AudioRingBuffer<AudioBlock>` + `WaveformModel` (legacy) |
| Pixel scaling                  | inside capture loop     | `WaveformRenderer` (UI layer)                            |
| FFT / spectrum                 | not available           | `SpectrumAnalyzer` + pure-Java `Fft`                     |
| RMS / peak                     | not available           | `RmsPeakAnalyzer`                                        |
| Headless demos / DSP tests     | required mock JavaSound | use `SignalGenerator` directly                           |

## Recipes

### Get the latest immutable block from any consumer

```java
AudioCaptureService svc = ...;
AudioBlock block = svc.getLatestBlock();
if (block != null) {
    // block is immutable; safe to share
    float[] left = block.channelView(0);
}
```

### Drain a downstream consumer (DSP/analysis) thread

```java
AudioRingBuffer<AudioBlock> rb = svc.getRingBuffer();
SpectrumAnalyzer analyzer = new SpectrumAnalyzer(4096, 0, svc.getDescriptor().sampleRate());

while (running) {
    AudioBlock block = rb.poll();
    if (block == null) {
        Thread.onSpinWait(); // or short sleep
        continue;
    }
    SpectrumSnapshot s = analyzer.analyze(block);
    publishToUI(s);
}
```

### Build a DSP pipeline

```java
DSPProcessor dcBlocker = block -> {
    float[][] in  = block.samples();
    float[][] out = new float[in.length][in[0].length];
    for (int c = 0; c < in.length; c++) {
        double mean = 0; for (float v : in[c]) mean += v;
        mean /= in[c].length;
        for (int i = 0; i < in[c].length; i++) out[c][i] = (float) (in[c][i] - mean);
    }
    return AudioBlock.wrap(block.format(), out, block.frameIndex(), block.timestampNanos());
};

DSPPipeline pipeline = DSPPipeline.of(dcBlocker, DSPProcessor.identity());
AudioBlock processed = pipeline.process(rawBlock);
```

### Run a deterministic test without an audio device

```java
AudioFormatDescriptor fmt = new AudioFormatDescriptor(48_000f, 1, 16);
SineGenerator gen = new SineGenerator(fmt, 1_000.0, 1f);

AudioBlock block = gen.nextBlock(4096);
SpectrumSnapshot s = new SpectrumAnalyzer(4096, 0, 48_000f).analyze(block);
// Assert that the FFT peak is at ~1 kHz.
```

### Render a snapshot to a Swing panel

```java
WaveformSnapshot snap = WaveformSnapshot.fromBlock(svc.getLatestBlock());
int[]   xs = WaveformRenderer.computeXPoints(snap.frames(), panelWidth);
int[][] ys = WaveformRenderer.computeYPointsAllChannels(snap, panelHeight);
g.drawPolyline(xs, ys[0], xs.length);
```

## Removing the legacy `WaveformModel`

The legacy model is still emitted on every capture iteration to keep existing Swing panels
working. When all consumers have migrated to `WaveformSnapshot`, the model can be removed in
a follow-up PR. Until then, both representations are kept in sync (the legacy model is built
from the same `AudioBlock` the rest of the platform sees).

## Compatibility matrix

|                                                    API                                                    |                                Status                                |
|-----------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------|
| `AudioCaptureService.start/stop/isRunning/getLatestModel/getFormat/setDivisor/getDivisor/recomputeLayout` | unchanged                                                            |
| `WaveformModel`                                                                                           | unchanged, still produced                                            |
| `WaveformPanel`, `PhaseDiagramCanvas`, `PhaseDiagramPanel`, `AudioAnalyseFrame`                           | unchanged                                                            |
| `AudioCaptureService.getDescriptor/getLatestBlock/getRingBuffer`                                          | new (default methods, returning `null` for non-impl implementations) |
| Everything under `org.hammer.audio.{core,buffer,dsp,analysis,signal,snapshot,ui,capture}`                 | new                                                                  |

All existing tests continue to pass.
