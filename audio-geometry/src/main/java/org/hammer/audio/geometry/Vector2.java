package org.hammer.audio.geometry;

/** Immutable two-dimensional vector in meters for reusable geometry and localization APIs. */
public record Vector2(double x, double y) {

  /** Zero vector. */
  public static final Vector2 ZERO = new Vector2(0.0, 0.0);

  /** Create a vector and reject NaN or infinite coordinates. */
  public Vector2 {
    if (!Double.isFinite(x) || !Double.isFinite(y)) {
      throw new IllegalArgumentException("coordinates must be finite");
    }
  }

  /** Return this vector plus {@code other}. */
  public Vector2 plus(Vector2 other) {
    return new Vector2(x + other.x, y + other.y);
  }

  /** Return this vector minus {@code other}. */
  public Vector2 minus(Vector2 other) {
    return new Vector2(x - other.x, y - other.y);
  }

  /** Return this vector multiplied by {@code scalar}. */
  public Vector2 scale(double scalar) {
    if (!Double.isFinite(scalar)) {
      throw new IllegalArgumentException("scalar must be finite");
    }
    return new Vector2(x * scalar, y * scalar);
  }

  /** Dot product with {@code other}. */
  public double dot(Vector2 other) {
    return x * other.x + y * other.y;
  }

  /** Euclidean length in meters. */
  public double length() {
    return Math.hypot(x, y);
  }

  /** Distance to {@code other} in meters. */
  public double distanceTo(Vector2 other) {
    return minus(other).length();
  }

  /** Unit vector in the same direction, or zero for the zero vector. */
  public Vector2 normalized() {
    double len = length();
    return len > 0.0 ? scale(1.0 / len) : ZERO;
  }
}
