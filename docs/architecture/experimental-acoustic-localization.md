# Experimental acoustic localization architecture

## Architecture review

The current project is already layered around acquisition, immutable `AudioBlock` values, ring
buffers, DSP processors, analysis snapshots and Swing visualization. That makes it a suitable host
for experimental acoustic localization, provided the research code stays outside the stable core.

Existing reusable building blocks:

- `org.hammer.audio.core` owns stable normalized audio blocks and format metadata.
- `org.hammer.audio.dsp` owns generic block-to-block processing composition.
- `org.hammer.audio.analysis` owns reusable FFT and analyzer contracts.
- `org.hammer.audio.localization` owns the existing production-adjacent stereo delay diagnostic.
- `org.hammer.audio.signal` owns deterministic signal generation.
- `org.hammer.audio.ui` and `org.hammer` own rendering and Swing boundaries.

## Coupling analysis

The experimental module must not depend on Swing panels, JavaSound device classes or application
frame state. The new dependency direction is:

```text
org.hammer.audio.core
org.hammer.audio.geometry
org.hammer.audio.acquisition
        ▲
        │
org.hammer.audio.experimental.acoustic
        │
        ├─ simulation
        └─ visualization DTOs
```

Core packages do not import `org.hammer.audio.experimental.*`. The plugin imports stable audio,
geometry and acquisition APIs. Visualization output is represented as DTOs so Swing, web or notebook
renderers can consume it without coupling DSP to a UI framework.

## What belongs in core

Core contains abstractions useful beyond insects or research prototypes:

- microphone metadata and synchronized multi-channel source contracts;
- sample clock handling and timestamped `AudioBlock` frames;
- 2D geometry primitives, rays and localization constraints;
- existing FFT, DSP pipeline and analysis contracts;
- recording/replay hooks that operate on `AudioBlock` rather than a concrete device library.

These are intentionally small. They do not encode mosquito wingbeat ranges, beamforming heuristics
or assumptions about a room experiment.

## What belongs in the acoustic plugin

`org.hammer.audio.experimental.acoustic` contains research-specific and replaceable logic:

- wingbeat/narrow-band frequency tracking;
- cross-correlation and GCC-PHAT TDOA experiments;
- delay-and-sum beamforming over candidate grids;
- example mosquito-localization pipeline composition;
- room simulation, moving emitters, reflections and noise;
- visualization-ready debug frames and heatmaps.

This code may evolve quickly and may be benchmarked or replaced without changing the stable
application model.

## Modularization plan

1. Keep the current Maven artifact as the compatibility boundary for now.
2. Treat package roots as modules: stable `audio.*` APIs versus `audio.experimental.*` plugins.
3. Promote only proven generic interfaces from the plugin into core.
4. If dependencies diverge later, split Maven modules in this direction: `audio-core`,
   `audio-acquisition`, `audio-dsp`, `audio-experimental-acoustic`, `audio-app`.

## Rationale

A package-level module is the smallest backwards-compatible refactor for the existing single-module
project. It avoids introducing build complexity before the research surface stabilizes while still
making the dependency direction explicit and enforceable by code review.
