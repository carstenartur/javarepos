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

## Maven modularization

The repository now uses a seven-module Maven reactor. The split is the build-level compatibility
boundary for stable audio APIs, plugin contracts, Swing application code and research-only acoustic
localization code.

- `audio-core` for `AudioBlock`, format metadata and generic immutable domain models;
- `audio-geometry` for reusable 2D geometry primitives and localization constraints;
- `audio-acquisition` for synchronized source and microphone-array contracts;
- `audio-dsp` for reusable FFT, DSP, analyzer, spectrogram, diagnosis and stereo-delay logic;
- `audio-plugin-api` for stable plugin contracts with no dependencies on concrete audio modules,
  host code or plugins;
- `audio-experimental-acoustic` for mosquito/insect-specific research components;
- `audio-app` for Swing UI, JavaSound/demo wiring, export, plugin hosting and packaging.

The acoustic plugin depends on stable modules plus `audio-plugin-api`. The Swing app compiles
against `audio-plugin-api` and includes the concrete acoustic plugin only as a runtime dependency so
Java `ServiceLoader` can discover it.

## Enforced dependency guards

`audio-app/src/test/java/org/hammer/audio/ArchitectureBoundaryTest.java` currently fails the build
if:

- stable modules import `org.hammer.audio.experimental.*`;
- stable modules import UI packages or top-level Swing application packages;
- stable module POMs depend on `audio-app`, `audio-plugin-api` or
  `audio-experimental-acoustic`;
- `audio-plugin-api` imports host or concrete plugin packages;
- `audio-app` has a compile-scope dependency on `audio-experimental-acoustic`.

This keeps experimental acoustic code dependent on stable APIs only, while preventing stable core,
DSP, acquisition or geometry code from depending on the plugin or app/UI layers.

## Rationale

A build-level module boundary is the smallest backwards-compatible refactor that prevents
experimental research code from becoming an implicit dependency of the stable platform. It was done
now, before more acoustic localization experiments are added, so dependency direction is explicit
and enforced by both Maven and automated tests.
