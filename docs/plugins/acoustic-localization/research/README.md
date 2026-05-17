# Acoustic localization — research notes

This folder collects the research-oriented documentation for the `audio-experimental-acoustic`
subproject. It complements the user-facing plugin documentation in
[`../README.md`](../README.md) and the runtime tracking architecture described in
[`../tracking.md`](../tracking.md).

The content here is intentionally narrower than a real paper: it is a working outline
that maps research concepts (TDOA, Doppler fusion, frequency tracking) onto the
classes, simulation scenarios and tests that already live in the repository, so the
documentation stays grounded in code that compiles and is exercised by tests.

## Contents

|                                  Document                                  |                                                             Purpose                                                             |
|----------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| [`paper-outline.md`](paper-outline.md)                                     | Sections of a publishable description of the system, with cross-references to existing modules and tests.                       |
| [`experiments.md`](experiments.md)                                         | Reproducible evaluation scenarios, each mapped to a `SimulationScenarios` entry in the `audio-experimental-acoustic` module.    |
| [`evaluation-metrics.md`](evaluation-metrics.md)                           | Definitions of the metrics used by tests and demos, aligned with `TrackingSnapshot` / `AcousticLocalizationSnapshot`.           |
| [`demo-scenarios.md`](demo-scenarios.md)                                   | Demos that can be shown live using the Swing plugin and `AcousticDebugFrame`.                                                   |
| [`hardware/microphone-array-setup.md`](hardware/microphone-array-setup.md) | Practical setup notes for real microphone rigs, complementing the simulator.                                                    |
| [`simulation-datasets/README.md`](simulation-datasets/README.md)           | JSON scenario descriptors with ground truth, derived from `SimulationScenarios`, for benchmark scripts and external evaluation. |

## Related documents

- Subproject overview: [`docs/plugins/acoustic-localization.md`](../../acoustic-localization.md)
- Plugin user guide: [`../README.md`](../README.md)
- Tracking pipeline architecture: [`../tracking.md`](../tracking.md)
- Synchronization assumptions: [`../synchronization.md`](../synchronization.md)
- Repository-level architecture: [`../../../architecture/experimental-acoustic-localization.md`](../../../architecture/experimental-acoustic-localization.md)

## Conventions

- Every research claim must point at a class, scenario or test in the
  `audio-experimental-acoustic` module. If the code is not there yet, the document
  must explicitly mark the item as *future work*.
- Numerical constants (frequencies, positions, velocities) follow
  `SimulationScenarios` so that the documentation and the deterministic tests cannot
  drift apart silently.
- Ground-truth datasets live under [`simulation-datasets/`](simulation-datasets/) as
  JSON so that external benchmark scripts can consume them without depending on the
  Java code.

