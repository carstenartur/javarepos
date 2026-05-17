package org.hammer.audio.plugin;

/**
 * Demo-signal contribution supplied by a plugin (e.g. domain-specific synthetic signals such as
 * insect-like bursts or moving sources).
 *
 * <p>This contract is intentionally a marker plus identification metadata; the host enumerates the
 * contributions to log and display them. Actual signal generation is performed by plugin code via
 * its own factory entry points, so this stable API does not need to depend on the audio domain
 * modules.
 */
public interface DemoSignalContribution {

  /** Stable identifier for the demo signal. */
  String id();

  /** Human-readable label for the demo signal. */
  String label();

  /** Single-sentence description of what the signal is intended to demonstrate. */
  String description();
}
