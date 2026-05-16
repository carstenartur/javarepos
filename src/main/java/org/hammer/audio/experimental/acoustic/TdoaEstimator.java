package org.hammer.audio.experimental.acoustic;

import org.hammer.audio.acquisition.MicrophoneArray;
import org.hammer.audio.core.AudioBlock;

/** Experimental strategy interface for interchangeable TDOA algorithms. */
public interface TdoaEstimator {

  /** Estimate TDOA between two channels. */
  TdoaEstimate estimate(AudioBlock block, MicrophoneArray array, int firstChannel, int secondChannel);
}
