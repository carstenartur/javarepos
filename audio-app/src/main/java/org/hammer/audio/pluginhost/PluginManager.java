package org.hammer.audio.pluginhost;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hammer.audio.plugin.AudioAnalyzerPlugin;

/**
 * Discovers and loads {@link AudioAnalyzerPlugin} implementations via {@link ServiceLoader}.
 *
 * <p>Each plugin is initialized in isolation: a faulty descriptor or constructor cannot prevent
 * other plugins from loading, and never crashes the host application. Failed plugins are still
 * surfaced through the returned {@link PluginRegistry} so the UI/log can show them.
 *
 * <p>This is the only entry point the host application uses to discover plugins. It deliberately
 * does not import or reference any concrete plugin class.
 */
public final class PluginManager {

  private static final Logger LOGGER = Logger.getLogger(PluginManager.class.getName());

  private final ClassLoader classLoader;

  /** Use the current thread's context class loader (or this class' loader as fallback). */
  public PluginManager() {
    this(defaultClassLoader());
  }

  /** Use the supplied class loader for service discovery. */
  public PluginManager(ClassLoader classLoader) {
    this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
  }

  /**
   * Discover all plugins on the configured class loader and return a populated {@link
   * PluginRegistry}. Never throws; failures are isolated into individual {@link PluginLoadResult}
   * entries.
   */
  public PluginRegistry loadPlugins() {
    List<PluginLoadResult> results = new ArrayList<>();
    ServiceLoader<AudioAnalyzerPlugin> loader =
        ServiceLoader.load(AudioAnalyzerPlugin.class, classLoader);
    for (ServiceLoader.Provider<AudioAnalyzerPlugin> provider : loader.stream().toList()) {
      String providerClassName = safeProviderName(provider);
      try {
        AudioAnalyzerPlugin plugin = provider.get();
        // Force descriptor evaluation to surface validation failures here.
        Objects.requireNonNull(plugin.descriptor(), "descriptor");
        results.add(PluginLoadResult.success(plugin));
        LOGGER.log(
            Level.INFO,
            "Loaded plugin: {0} ({1} {2})",
            new Object[] {
              plugin.descriptor().id(), plugin.descriptor().name(), plugin.descriptor().version()
            });
      } catch (ServiceConfigurationError | RuntimeException ex) {
        LOGGER.log(Level.WARNING, "Failed to load plugin " + providerClassName, ex);
        results.add(PluginLoadResult.failure(providerClassName, ex));
      }
    }
    return new PluginRegistry(results);
  }

  private static String safeProviderName(ServiceLoader.Provider<AudioAnalyzerPlugin> provider) {
    try {
      return provider.type().getName();
    } catch (RuntimeException ex) {
      return "<unknown>";
    }
  }

  private static ClassLoader defaultClassLoader() {
    ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
    if (contextLoader != null) {
      return contextLoader;
    }
    return PluginManager.class.getClassLoader();
  }
}
