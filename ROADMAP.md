# Roadmap

This roadmap lists open next steps. Completed foundations such as FFT/spectrum, spectrogram,
diagnosis, evidence export, recording/replay, A/B comparison, trigger and spectrum peak-hold are
current capabilities and are documented under [docs/features/](docs/features/README.md).

## Product hardening

- Continue reducing Checkstyle, PMD and SpotBugs findings below the committed CI baseline.
- Raise JaCoCo coverage thresholds gradually after adding behavior-focused tests.
- Add a lightweight documentation link check if it can run without network access.
- Keep the README screenshot workflow reproducible as the Swing dashboard evolves.

## UI and ergonomics

- Exercise the main dashboard at common desktop sizes and HiDPI scale factors.
- Add focused layout tests for measurement and diagnosis panels when adding controls.
- Review long labels/translations before they are added to fixed-width controls.

## Architecture

- Resolve the current `org.hammer.audio` split package before any JPMS migration.
- Decide whether `DemoSignalType` belongs in a stable shared API package or an app-specific package.
- Keep plugin contracts in `audio-plugin-api`; avoid compile-time app dependencies on concrete
  plugins.

## Recording, export and comparison

- Add richer evidence-bundle metadata and reproducibility hints.
- Expand A/B comparison reports with configurable thresholds for regression use cases.
- Add replay-driven integration tests around end-to-end model publication.

## Experimental acoustic localization

The `audio-experimental-acoustic` module remains a research plugin, not a production feature. Open
research work is tracked in the [plugin details](docs/plugins/acoustic-localization/README.md#future-research-directions),
including multi-source separation, sub-sample GCC-PHAT interpolation, calibrated 3D geometry and a
benchmark corpus with real and synthetic recordings.

## Timing and synchronization

- Turn the documented `SampleClock` drift/jitter limitation into an executable test or tracked issue.
- Add calibration-data examples for synchronized microphone arrays before making stronger real-world
  localization claims.

