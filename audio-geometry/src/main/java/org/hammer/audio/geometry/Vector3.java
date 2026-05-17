package org.hammer.audio.geometry;

/** Immutable three-dimensional vector; units are defined by the API that uses it. */
public record Vector3(double x, double y, double z) {

  /** Zero vector. */
  public static final Vector3 ZERO = new Vector3(0.0, 0.0, 0.0);

  /** Create a finite vector. */
  public Vector3 {
    if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
      throw new IllegalArgumentException("coordinates must be finite");
    }
  }

  /** Convert a 2D vector into the horizontal plane. */
  public static Vector3 from(Vector2 vector) {
    return new Vector3(vector.x(), vector.y(), 0.0);
  }

  /** Return this vector plus {@code other}. */
  public Vector3 plus(Vector3 other) {
    return new Vector3(x + other.x, y + other.y, z + other.z);
  }

  /** Return this vector multiplied by {@code scalar}. */
  public Vector3 scale(double scalar) {
    if (!Double.isFinite(scalar)) {
      throw new IllegalArgumentException("scalar must be finite");
    }
    return new Vector3(x * scalar, y * scalar, z * scalar);
  }

  /** Horizontal projection used by existing 2D tracking filters. */
  public Vector2 xy() {
    return new Vector2(x, y);
  }

  /** Euclidean length. */
  public double length() {
    return Math.sqrt(x * x + y * y + z * z);
  }
}
