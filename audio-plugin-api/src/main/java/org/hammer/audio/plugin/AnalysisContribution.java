package org.hammer.audio.plugin;

/**
 * Marker interface for analysis contributions supplied by a plugin.
 *
 * <p>Concrete contribution types (e.g. an additional {@code AnalysisModule}, a derived snapshot
 * provider) deliberately live outside the stable plugin API to keep this module free of audio
 * domain dependencies. Plugins describe their analysis pieces through implementations of this
 * marker plus a human-readable identifier; the host application typically logs and lists
 * contributions without invoking them directly.
 */
public interface AnalysisContribution {

  /** Short identifier shown to users (e.g. in plugin info dialogs). */
  String id();

  /** Single-sentence description of the analysis contributed by the plugin. */
  String description();
}
