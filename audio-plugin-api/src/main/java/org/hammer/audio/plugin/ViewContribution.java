package org.hammer.audio.plugin;

import java.util.function.Supplier;
import javax.swing.JComponent;

/**
 * View contribution supplied by a plugin. The host instantiates the view lazily via {@link
 * #componentFactory()} and is responsible for wrapping it in a window, tab or sidebar.
 *
 * <p>The host application controls the lifecycle of the produced component; plugins must therefore
 * return a fresh component instance per invocation of the factory.
 */
public interface ViewContribution {

  /** Stable identifier for this view (used for logging and de-duplication). */
  String id();

  /** Human-readable title shown to the user (menu label, tab title, window title). */
  String title();

  /** Factory that creates a new Swing component representing this view. */
  Supplier<? extends JComponent> componentFactory();
}
