package org.hammer.audio.pluginhost;

import java.util.Objects;
import java.util.Optional;
import org.hammer.audio.plugin.AudioAnalyzerPlugin;
import org.hammer.audio.plugin.PluginDescriptor;

/**
 * Outcome of attempting to load a single plugin.
 *
 * <p>Either {@link #plugin()} is present (successful load) or {@link #failure()} is present
 * (initialization or descriptor failure isolated from the rest of the application). The {@link
 * #descriptor()} is always populated; for failed loads, a synthetic descriptor identifying the
 * failing service-provider class name is used.
 */
public final class PluginLoadResult {

  private final PluginDescriptor descriptor;
  private final AudioAnalyzerPlugin plugin;
  private final Throwable failure;

  private PluginLoadResult(
      PluginDescriptor descriptor, AudioAnalyzerPlugin plugin, Throwable failure) {
    this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
    this.plugin = plugin;
    this.failure = failure;
  }

  /** Construct a successful load result. */
  public static PluginLoadResult success(AudioAnalyzerPlugin plugin) {
    Objects.requireNonNull(plugin, "plugin");
    return new PluginLoadResult(plugin.descriptor(), plugin, null);
  }

  /** Construct a failure result with a synthetic descriptor identifying the failing class. */
  public static PluginLoadResult failure(String providerClassName, Throwable cause) {
    Objects.requireNonNull(providerClassName, "providerClassName");
    Objects.requireNonNull(cause, "cause");
    PluginDescriptor synthetic =
        new PluginDescriptor(
            "failed:" + providerClassName,
            providerClassName,
            "n/a",
            "Plugin failed to load: " + cause.getClass().getSimpleName(),
            null,
            true);
    return new PluginLoadResult(synthetic, null, cause);
  }

  /** Descriptor of the (attempted) plugin. */
  public PluginDescriptor descriptor() {
    return descriptor;
  }

  /** The loaded plugin, if successful. */
  public Optional<AudioAnalyzerPlugin> plugin() {
    return Optional.ofNullable(plugin);
  }

  /** The failure, if loading failed. */
  public Optional<Throwable> failure() {
    return Optional.ofNullable(failure);
  }

  /** {@code true} if the plugin loaded successfully. */
  public boolean isSuccess() {
    return plugin != null;
  }
}
