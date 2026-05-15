package org.hammer.audio.analysis;

import org.hammer.audio.core.AudioBlock;

/**
 * Computes per-channel RMS (root-mean-square) and peak (max absolute) values for an audio block.
 *
 * <p>The RMS of an N-frame signal {@code x[i]} is
 *
 * <pre>{@code rms = sqrt( (1/N) * sum( x[i]^2 ) )}</pre>
 *
 * and the peak is {@code max(|x[i]|)}. Both are reported in the same normalized linear units as the
 * input samples ({@code [0, 1]} for a unit-amplitude signal).
 *
 * <p>This analyzer is stateless and thread-safe: a single instance can be safely shared between
 * threads.
 *
 * @author refactoring
 */
public final class RmsPeakAnalyzer implements AnalysisModule<RmsPeakSnapshot> {

  @Override
  public RmsPeakSnapshot analyze(AudioBlock block) {
    int channels = block.channels();
    int frames = block.frames();
    float[] rms = new float[channels];
    float[] peak = new float[channels];

    for (int c = 0; c < channels; c++) {
      float[] samples = block.channelView(c);
      double sumSq = 0.0;
      float p = 0f;
      for (int i = 0; i < frames; i++) {
        float s = samples[i];
        float a = s < 0 ? -s : s;
        if (a > p) {
          p = a;
        }
        sumSq += (double) s * (double) s;
      }
      rms[c] = frames == 0 ? 0f : (float) Math.sqrt(sumSq / frames);
      peak[c] = p;
    }

    return new RmsPeakSnapshot(block.frameIndex(), block.timestampNanos(), rms, peak);
  }
}
