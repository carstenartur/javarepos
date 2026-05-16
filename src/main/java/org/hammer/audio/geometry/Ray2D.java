package org.hammer.audio.geometry;

/** Half-line in two-dimensional space, suitable for visualization-ready localization output. */
public record Ray2D(Vector2 origin, Vector2 direction) {

  /** Create a ray and normalize its direction. */
  public Ray2D {
    if (origin == null || direction == null) {
      throw new IllegalArgumentException("origin and direction must not be null");
    }
    direction = direction.normalized();
  }

  /** Point at non-negative distance {@code meters} along the ray. */
  public Vector2 pointAt(double meters) {
    if (meters < 0.0 || !Double.isFinite(meters)) {
      throw new IllegalArgumentException("meters must be finite and >= 0");
    }
    return origin.plus(direction.scale(meters));
  }
}
