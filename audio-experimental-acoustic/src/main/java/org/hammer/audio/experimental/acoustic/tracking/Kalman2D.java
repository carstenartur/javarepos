package org.hammer.audio.experimental.acoustic.tracking;

import org.hammer.audio.geometry.Vector2;

/**
 * Constant-velocity 2D Kalman filter for smoothing source position estimates over time.
 *
 * <p>The state is {@code [x, y, vx, vy]^T} in meters and meters/second. The filter assumes
 * piecewise-constant velocity with white acceleration noise; the process noise spectral density is
 * configurable. The measurement model observes only position {@code [x, y]^T}; measurement noise is
 * configurable and isotropic.
 *
 * <p>This is a deliberately small, dependency-free implementation suitable for research, demos and
 * tests. It uses a fixed 4-element state vector and a 4x4 covariance matrix encoded as a {@code
 * double[16]} in row-major order; it does not allocate during {@link #predict(double)} or {@link
 * #update(Vector2)}.
 */
public final class Kalman2D {

  private static final int STATE = 4;

  private final double processNoiseDensity;
  private final double measurementNoiseVariance;

  private final double[] state;
  private final double[] covariance;

  /** Scratch buffers reused between calls to avoid garbage during real-time processing. */
  private final double[] scratchA = new double[STATE * STATE];

  private final double[] scratchB = new double[STATE * STATE];

  /**
   * Construct a filter at the given initial position with zero velocity.
   *
   * @param initialPosition starting {@code (x, y)}
   * @param initialPositionVariance variance of the initial position estimate
   * @param initialVelocityVariance variance of the initial velocity estimate
   * @param processNoiseDensity acceleration process noise spectral density (m^2/s^3)
   * @param measurementNoiseVariance variance of {@code (x, y)} position measurements (m^2)
   */
  public Kalman2D(
      Vector2 initialPosition,
      double initialPositionVariance,
      double initialVelocityVariance,
      double processNoiseDensity,
      double measurementNoiseVariance) {
    if (initialPosition == null) {
      throw new IllegalArgumentException("initialPosition must not be null");
    }
    if (initialPositionVariance < 0.0 || !Double.isFinite(initialPositionVariance)) {
      throw new IllegalArgumentException("initialPositionVariance must be finite and >= 0");
    }
    if (initialVelocityVariance < 0.0 || !Double.isFinite(initialVelocityVariance)) {
      throw new IllegalArgumentException("initialVelocityVariance must be finite and >= 0");
    }
    if (processNoiseDensity < 0.0 || !Double.isFinite(processNoiseDensity)) {
      throw new IllegalArgumentException("processNoiseDensity must be finite and >= 0");
    }
    if (!(measurementNoiseVariance > 0.0) || !Double.isFinite(measurementNoiseVariance)) {
      throw new IllegalArgumentException("measurementNoiseVariance must be finite and > 0");
    }
    this.processNoiseDensity = processNoiseDensity;
    this.measurementNoiseVariance = measurementNoiseVariance;
    this.state = new double[] {initialPosition.x(), initialPosition.y(), 0.0, 0.0};
    this.covariance = new double[STATE * STATE];
    covariance[0] = initialPositionVariance;
    covariance[5] = initialPositionVariance;
    covariance[10] = initialVelocityVariance;
    covariance[15] = initialVelocityVariance;
  }

  /** Advance the state by {@code dtSeconds}. */
  public void predict(double dtSeconds) {
    if (!Double.isFinite(dtSeconds) || dtSeconds < 0.0) {
      throw new IllegalArgumentException("dtSeconds must be finite and >= 0");
    }
    // x = F x ; with F = [[1,0,dt,0],[0,1,0,dt],[0,0,1,0],[0,0,0,1]]
    state[0] += dtSeconds * state[2];
    state[1] += dtSeconds * state[3];

    // P = F P F^T + Q
    double[] f = scratchA;
    fillIdentity(f);
    f[2] = dtSeconds;
    f[7] = dtSeconds;

    multiply(f, covariance, scratchB);
    transposeIntoA(f);
    multiply(scratchB, f, covariance);

    addProcessNoise(dtSeconds);
  }

  /** Update the state with a 2D position measurement. */
  public void update(Vector2 measurement) {
    if (measurement == null) {
      throw new IllegalArgumentException("measurement must not be null");
    }
    // Two scalar updates: first x, then y. H selects one row; R is the scalar measurement
    // variance. We use the canonical form: K = P H^T / (H P H^T + R), x += K (z - H x),
    // P -= K H P.
    applyScalarUpdate(0, measurement.x());
    applyScalarUpdate(1, measurement.y());
  }

  /** Current position estimate. */
  public Vector2 position() {
    return new Vector2(state[0], state[1]);
  }

  /** Current velocity estimate in meters per second. */
  public Vector2 velocity() {
    return new Vector2(state[2], state[3]);
  }

  /** Variance of the current position estimate (mean of x/y variances). */
  public double positionVariance() {
    return 0.5 * (covariance[0] + covariance[5]);
  }

  private void applyScalarUpdate(int observedState, double measurement) {
    double innovationVariance =
        covariance[observedState * STATE + observedState] + measurementNoiseVariance;
    if (innovationVariance <= 0.0) {
      return;
    }
    double[] gain = new double[STATE];
    for (int row = 0; row < STATE; row++) {
      gain[row] = covariance[row * STATE + observedState] / innovationVariance;
    }
    double innovation = measurement - state[observedState];
    for (int row = 0; row < STATE; row++) {
      state[row] += gain[row] * innovation;
    }
    // P = (I - K H) P, where H picks row observedState; equivalent to subtracting
    // gain[row] * P[observedState, c] from P[row, c].
    double[] observedRow = new double[STATE];
    for (int c = 0; c < STATE; c++) {
      observedRow[c] = covariance[observedState * STATE + c];
    }
    for (int row = 0; row < STATE; row++) {
      double factor = gain[row];
      for (int c = 0; c < STATE; c++) {
        covariance[row * STATE + c] -= factor * observedRow[c];
      }
    }
  }

  private void addProcessNoise(double dt) {
    if (processNoiseDensity == 0.0 || dt == 0.0) {
      return;
    }
    double q3 = processNoiseDensity * Math.pow(dt, 3) / 3.0;
    double q2 = processNoiseDensity * Math.pow(dt, 2) / 2.0;
    double q1 = processNoiseDensity * dt;
    covariance[0] += q3;
    covariance[5] += q3;
    covariance[10] += q1;
    covariance[15] += q1;
    covariance[2] += q2;
    covariance[8] += q2;
    covariance[7] += q2;
    covariance[13] += q2;
  }

  private static void fillIdentity(double[] m) {
    for (int i = 0; i < m.length; i++) {
      m[i] = 0.0;
    }
    m[0] = 1.0;
    m[5] = 1.0;
    m[10] = 1.0;
    m[15] = 1.0;
  }

  private static void transposeIntoA(double[] m) {
    for (int r = 0; r < STATE; r++) {
      for (int c = r + 1; c < STATE; c++) {
        double tmp = m[r * STATE + c];
        m[r * STATE + c] = m[c * STATE + r];
        m[c * STATE + r] = tmp;
      }
    }
  }

  private static void multiply(double[] a, double[] b, double[] out) {
    for (int r = 0; r < STATE; r++) {
      for (int c = 0; c < STATE; c++) {
        double sum = 0.0;
        for (int k = 0; k < STATE; k++) {
          sum += a[r * STATE + k] * b[k * STATE + c];
        }
        out[r * STATE + c] = sum;
      }
    }
  }
}
