package org.hammer.audio.plugin;

import java.util.Objects;

/**
 * Immutable metadata describing a plugin.
 *
 * <p>Used by the host application to display, log and route plugin information without depending on
 * the plugin implementation itself.
 *
 * @param id stable, machine-readable plugin identifier, e.g. {@code "acoustic-localization"}
 * @param name human-readable plugin name shown in menus and dialogs
 * @param version plugin version string, free-form (recommended: semantic version)
 * @param description short, single-sentence description of what the plugin does
 * @param documentationPath repository-relative or absolute documentation link (Markdown file or
 *     URL); may be {@code null} when the plugin does not ship documentation
 * @param experimental {@code true} if the plugin is research/experimental code, {@code false} for
 *     production-ready plugins
 */
public record PluginDescriptor(
    String id,
    String name,
    String version,
    String description,
    String documentationPath,
    boolean experimental) {

  /** Validate required descriptor fields. */
  public PluginDescriptor {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(name, "name must not be null");
    Objects.requireNonNull(version, "version must not be null");
    Objects.requireNonNull(description, "description must not be null");
    if (id.isBlank()) {
      throw new IllegalArgumentException("id must not be blank");
    }
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    if (version.isBlank()) {
      throw new IllegalArgumentException("version must not be blank");
    }
  }
}
