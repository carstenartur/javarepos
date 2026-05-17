package org.hammer.audio.pluginhost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.hammer.audio.plugin.AudioAnalyzerPlugin;

/**
 * Read-only view over the plugins discovered by {@link PluginManager}.
 *
 * <p>Holds both successful loads and isolated failures so the host UI and log can report a complete
 * picture of the plugin environment.
 */
public final class PluginRegistry {

  private final List<PluginLoadResult> results;

  /** Create a registry from the supplied load results. The list is defensively copied. */
  public PluginRegistry(List<PluginLoadResult> results) {
    Objects.requireNonNull(results, "results");
    this.results = Collections.unmodifiableList(new ArrayList<>(results));
  }

  /** All load results in discovery order. */
  public List<PluginLoadResult> results() {
    return results;
  }

  /** Successfully loaded plugins. */
  public List<AudioAnalyzerPlugin> plugins() {
    List<AudioAnalyzerPlugin> plugins = new ArrayList<>();
    for (PluginLoadResult result : results) {
      result.plugin().ifPresent(plugins::add);
    }
    return Collections.unmodifiableList(plugins);
  }

  /** Failures isolated during loading. */
  public List<PluginLoadResult> failures() {
    List<PluginLoadResult> failed = new ArrayList<>();
    for (PluginLoadResult result : results) {
      if (!result.isSuccess()) {
        failed.add(result);
      }
    }
    return Collections.unmodifiableList(failed);
  }

  /** {@code true} if no plugin (successful or failed) was discovered. */
  public boolean isEmpty() {
    return results.isEmpty();
  }
}
