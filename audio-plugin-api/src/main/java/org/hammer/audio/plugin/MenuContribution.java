package org.hammer.audio.plugin;

/**
 * Menu contribution supplied by a plugin.
 *
 * <p>The host application surfaces these entries in a generic <i>Plugins</i> menu, grouped by
 * plugin. The {@link #action()} runnable is executed on the Swing event-dispatch thread.
 */
public interface MenuContribution {

  /** Stable identifier for this menu entry (used for logging and de-duplication). */
  String id();

  /** Human-readable label shown in the menu. */
  String label();

  /** Action to execute when the menu entry is invoked. Must not be {@code null}. */
  Runnable action();
}
