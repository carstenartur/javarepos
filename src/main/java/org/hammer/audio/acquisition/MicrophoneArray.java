package org.hammer.audio.acquisition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Immutable description of a microphone array tied to a synchronized multichannel source. */
public final class MicrophoneArray {

  private final List<Microphone> microphones;

  /** Create a microphone array from unique microphone ids and channels. */
  public MicrophoneArray(List<Microphone> microphones) {
    if (microphones == null || microphones.isEmpty()) {
      throw new IllegalArgumentException("microphones must not be empty");
    }
    List<Microphone> copy = new ArrayList<>(microphones);
    copy.sort(Comparator.comparingInt(Microphone::channel));
    for (int i = 0; i < copy.size(); i++) {
      Microphone mic = copy.get(i);
      if (mic.channel() != i) {
        throw new IllegalArgumentException("channels must be contiguous from 0");
      }
      for (int j = i + 1; j < copy.size(); j++) {
        if (mic.id().equals(copy.get(j).id())) {
          throw new IllegalArgumentException("duplicate microphone id: " + mic.id());
        }
      }
    }
    this.microphones = List.copyOf(copy);
  }

  /** Return microphones ordered by channel. */
  public List<Microphone> microphones() {
    return microphones;
  }

  /** Number of synchronized channels. */
  public int channels() {
    return microphones.size();
  }

  /** Microphone metadata for a channel. */
  public Microphone microphone(int channel) {
    return microphones.get(channel);
  }
}
