package org.hammer.audio.experimental.acoustic.tracking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.hammer.audio.geometry.Vector3;
import org.hammer.audio.geometry.Vector2;

/**
 * Maintains a set of {@link TrackedSource}s over time, providing identity persistence and temporal
 * smoothing on top of per-frame frequency clusters and localized positions.
 *
 * <p>For each new frame the tracker is fed a list of observations, where every observation pairs a
 * {@link FrequencyCluster} with its localized 2D position. The tracker matches every observation to
 * the closest existing track in frequency space (within {@link #frequencyMatchHz} Hz tolerance),
 * updates the matched track's Kalman filter, creates a new track for unmatched observations, and
 * drops tracks that have not been observed for {@link #missingFramesToDrop} frames.
 *
 * <p>The tracker does not depend on audio types directly; it consumes only positions and
 * frequencies. This makes it equally usable from unit tests with synthetic data and from the full
 * {@link TrackingPipeline} with real audio.
 */
public final class SourceTracker {

  private final double frequencyMatchHz;
  private final int missingFramesToDrop;
  private final double processNoiseDensity;
  private final double measurementNoiseVariance;
  private final double initialPositionVariance;
  private final double initialVelocityVariance;
  private final double confidenceDecay;
  private final double confidenceGain;
  private final double dopplerVelocityWeight;

  private final List<Track> tracks = new ArrayList<>();
  private int nextId;
  private long lastFrameIndex = Long.MIN_VALUE;
  private double lastTimestampSeconds = Double.NaN;

  /**
   * Construct a tracker with explicit smoothing parameters.
   *
   * @param frequencyMatchHz maximum frequency distance for matching observations to tracks (Hz)
   * @param missingFramesToDrop number of consecutive missed frames after which a track is dropped
   * @param processNoiseDensity Kalman acceleration spectral density
   * @param measurementNoiseVariance Kalman position-measurement variance
   * @param initialPositionVariance initial position variance assigned to new tracks
   * @param initialVelocityVariance initial velocity variance assigned to new tracks
   * @param confidenceDecay multiplicative confidence decay applied on missed frames (in {@code
   *     [0,1]}, 0 means immediate forget, 1 means never decay)
   * @param confidenceGain weight added to confidence on every observation (in {@code [0,1]})
   */
  public SourceTracker(
      double frequencyMatchHz,
      int missingFramesToDrop,
      double processNoiseDensity,
      double measurementNoiseVariance,
      double initialPositionVariance,
      double initialVelocityVariance,
      double confidenceDecay,
      double confidenceGain) {
    this(
        frequencyMatchHz,
        missingFramesToDrop,
        processNoiseDensity,
        measurementNoiseVariance,
        initialPositionVariance,
        initialVelocityVariance,
        confidenceDecay,
        confidenceGain,
        0.35);
  }

  /**
   * Construct a tracker with explicit smoothing parameters and a Doppler/position velocity blend.
   */
  public SourceTracker(
      double frequencyMatchHz,
      int missingFramesToDrop,
      double processNoiseDensity,
      double measurementNoiseVariance,
      double initialPositionVariance,
      double initialVelocityVariance,
      double confidenceDecay,
      double confidenceGain,
      double dopplerVelocityWeight) {
    if (!(frequencyMatchHz > 0.0) || !Double.isFinite(frequencyMatchHz)) {
      throw new IllegalArgumentException("frequencyMatchHz must be finite and > 0");
    }
    if (missingFramesToDrop < 0) {
      throw new IllegalArgumentException("missingFramesToDrop must be >= 0");
    }
    if (!Double.isFinite(confidenceDecay) || confidenceDecay < 0.0 || confidenceDecay > 1.0) {
      throw new IllegalArgumentException("confidenceDecay must be in [0,1]");
    }
    if (!Double.isFinite(confidenceGain) || confidenceGain < 0.0 || confidenceGain > 1.0) {
      throw new IllegalArgumentException("confidenceGain must be in [0,1]");
    }
    if (!Double.isFinite(dopplerVelocityWeight)
        || dopplerVelocityWeight < 0.0
        || dopplerVelocityWeight > 1.0) {
      throw new IllegalArgumentException("dopplerVelocityWeight must be in [0,1]");
    }
    this.frequencyMatchHz = frequencyMatchHz;
    this.missingFramesToDrop = missingFramesToDrop;
    this.processNoiseDensity = processNoiseDensity;
    this.measurementNoiseVariance = measurementNoiseVariance;
    this.initialPositionVariance = initialPositionVariance;
    this.initialVelocityVariance = initialVelocityVariance;
    this.confidenceDecay = confidenceDecay;
    this.confidenceGain = confidenceGain;
    this.dopplerVelocityWeight = dopplerVelocityWeight;
  }

  /** Default configuration suitable for typical insect-source experiments. */
  public static SourceTracker withDefaults() {
    return new SourceTracker(40.0, 3, 0.5, 0.04, 1.0, 1.0, 0.8, 0.3);
  }

  /**
   * Update the tracker with the observations of one frame.
   *
   * @param frameIndex monotonically increasing frame index
   * @param timestampSeconds capture timestamp in seconds (used as the Kalman time step source)
   * @param observations frequency-cluster + position pairs for the current frame
   * @return immutable snapshot of all currently active tracks (sorted by id)
   */
  public List<TrackedSource> update(
      long frameIndex, double timestampSeconds, List<Observation> observations) {
    Objects.requireNonNull(observations, "observations");
    if (frameIndex < lastFrameIndex) {
      throw new IllegalArgumentException("frameIndex must be non-decreasing");
    }
    double dt =
        Double.isNaN(lastTimestampSeconds)
            ? 0.0
            : Math.max(0.0, timestampSeconds - lastTimestampSeconds);
    for (Track track : tracks) {
      track.filter.predict(dt);
      track.consecutiveMissedFrames++;
    }

    int existingTrackCount = tracks.size();
    boolean[] taken = new boolean[existingTrackCount];
    for (Observation observation : observations) {
      int matched = findClosestTrack(observation.frequencyHz(), taken);
      if (matched >= 0) {
        Track track = tracks.get(matched);
        track.filter.update(observation.position());
        track.frequencyHz = observation.frequencyHz();
        track.observedFrequencyHz = observation.observedFrequencyHz();
        track.radialVelocityMetersPerSecond = observation.radialVelocityMetersPerSecond();
        track.frequencyVarianceHzSquared = observation.frequencyVarianceHzSquared();
        track.fusedVelocityMetersPerSecond3d =
            blendVelocity(track.filter.velocity(), observation.velocityMetersPerSecond3d());
        track.lastUpdatedFrameIndex = frameIndex;
        track.consecutiveMissedFrames = 0;
        track.observationCount++;
        track.confidence = Math.min(1.0, track.confidence * confidenceDecay + confidenceGain);
        taken[matched] = true;
      } else {
        Track track = new Track();
        track.id = nextId++;
        track.frequencyHz = observation.frequencyHz();
        track.observedFrequencyHz = observation.observedFrequencyHz();
        track.filter =
            new Kalman2D(
                observation.position(),
                initialPositionVariance,
                initialVelocityVariance,
                processNoiseDensity,
                measurementNoiseVariance);
        track.lastUpdatedFrameIndex = frameIndex;
        track.consecutiveMissedFrames = 0;
        track.observationCount = 1;
        track.confidence = confidenceGain;
        track.fusedVelocityMetersPerSecond3d = observation.velocityMetersPerSecond3d();
        track.radialVelocityMetersPerSecond = observation.radialVelocityMetersPerSecond();
        track.frequencyVarianceHzSquared = observation.frequencyVarianceHzSquared();
        tracks.add(track);
      }
    }
    // Decay confidence for missed tracks.
    for (int i = 0; i < existingTrackCount; i++) {
      if (!taken[i]) {
        tracks.get(i).confidence *= confidenceDecay;
      }
    }
    // Drop stale tracks.
    Iterator<Track> iterator = tracks.iterator();
    while (iterator.hasNext()) {
      Track track = iterator.next();
      if (track.consecutiveMissedFrames > missingFramesToDrop) {
        iterator.remove();
      }
    }

    lastFrameIndex = frameIndex;
    lastTimestampSeconds = timestampSeconds;
    return snapshot();
  }

  /** Snapshot of currently active tracks, sorted by track id for stable rendering. */
  public List<TrackedSource> snapshot() {
    List<TrackedSource> sources = new ArrayList<>(tracks.size());
    for (Track track : tracks) {
      sources.add(
          new TrackedSource(
              track.id,
              track.frequencyHz,
              track.observedFrequencyHz,
              track.filter.position(),
              track.fusedVelocityMetersPerSecond3d.xy(),
              track.fusedVelocityMetersPerSecond3d,
              track.radialVelocityMetersPerSecond,
              track.frequencyVarianceHzSquared,
              track.confidence,
              track.lastUpdatedFrameIndex,
              track.observationCount));
    }
    sources.sort((left, right) -> Integer.compare(left.id(), right.id()));
    return Collections.unmodifiableList(sources);
  }

  /** Reset all tracker state (used by tests and scenario setups). */
  public void reset() {
    tracks.clear();
    nextId = 0;
    lastFrameIndex = Long.MIN_VALUE;
    lastTimestampSeconds = Double.NaN;
  }

  private int findClosestTrack(double frequencyHz, boolean[] taken) {
    int best = -1;
    double bestDistance = frequencyMatchHz;
    int upperBound = Math.min(tracks.size(), taken.length);
    for (int i = 0; i < upperBound; i++) {
      if (taken[i]) {
        continue;
      }
      double distance = Math.abs(tracks.get(i).frequencyHz - frequencyHz);
      if (distance <= bestDistance) {
        bestDistance = distance;
        best = i;
      }
    }
    return best;
  }

  private Vector3 blendVelocity(Vector2 positionDeltaVelocity, Vector3 dopplerVelocity) {
    Vector3 positional = Vector3.from(positionDeltaVelocity);
    return positional
        .scale(1.0 - dopplerVelocityWeight)
        .plus(dopplerVelocity.scale(dopplerVelocityWeight));
  }

  /** One frame observation: cluster frequency + localized 2D position. */
  public record Observation(
      double frequencyHz,
      double observedFrequencyHz,
      Vector2 position,
      Vector3 velocityMetersPerSecond3d,
      double radialVelocityMetersPerSecond,
      double frequencyVarianceHzSquared) {

    /** Create an observation without Doppler data. */
    public Observation(double frequencyHz, Vector2 position) {
      this(frequencyHz, frequencyHz, position, Vector3.ZERO, 0.0, 0.0);
    }

    /** Validate fields. */
    public Observation {
      if (!Double.isFinite(frequencyHz) || frequencyHz < 0.0) {
        throw new IllegalArgumentException("frequencyHz must be finite and >= 0");
      }
      if (!Double.isFinite(observedFrequencyHz) || observedFrequencyHz < 0.0) {
        throw new IllegalArgumentException("observedFrequencyHz must be finite and >= 0");
      }
      Objects.requireNonNull(position, "position");
      Objects.requireNonNull(velocityMetersPerSecond3d, "velocityMetersPerSecond3d");
      if (!Double.isFinite(radialVelocityMetersPerSecond)) {
        throw new IllegalArgumentException("radialVelocityMetersPerSecond must be finite");
      }
      if (!Double.isFinite(frequencyVarianceHzSquared) || frequencyVarianceHzSquared < 0.0) {
        throw new IllegalArgumentException("frequencyVarianceHzSquared must be finite and >= 0");
      }
    }
  }

  /** Internal mutable track record. */
  private static final class Track {
    int id;
    double frequencyHz;
    double observedFrequencyHz;
    Kalman2D filter;
    Vector3 fusedVelocityMetersPerSecond3d = Vector3.ZERO;
    double radialVelocityMetersPerSecond;
    double frequencyVarianceHzSquared;
    long lastUpdatedFrameIndex;
    int consecutiveMissedFrames;
    int observationCount;
    double confidence;
  }
}
