package org.hammer.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

/**
 * Default implementation of AudioLineProvider that uses AudioSystem to acquire lines.
 *
 * <p>This is the production implementation used by AudioCaptureServiceImpl's public constructor.
 *
 * @author refactoring
 */
class DefaultAudioLineProvider implements AudioLineProvider {

  private final Mixer.Info mixerInfo;

  DefaultAudioLineProvider() {
    this(null);
  }

  DefaultAudioLineProvider(Mixer.Info mixerInfo) {
    this.mixerInfo = mixerInfo;
  }

  @Override
  public TargetDataLine acquireLine(AudioFormat format) {
    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

    if (mixerInfo != null) {
      return acquireLineFromMixer(format, info);
    }

    if (!AudioSystem.isLineSupported(info)) {
      throw new IllegalStateException("TargetDataLine not supported for format: " + format);
    }

    try {
      TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
      line.open(format);
      return line;
    } catch (LineUnavailableException ex) {
      throw new IllegalStateException("Unable to open TargetDataLine", ex);
    }
  }

  private TargetDataLine acquireLineFromMixer(AudioFormat format, DataLine.Info info) {
    Mixer mixer = AudioSystem.getMixer(mixerInfo);
    if (!mixer.isLineSupported(info)) {
      throw new IllegalStateException(
          "TargetDataLine not supported by " + mixerInfo.getName() + " for format: " + format);
    }

    try {
      TargetDataLine line = (TargetDataLine) mixer.getLine(info);
      line.open(format);
      return line;
    } catch (LineUnavailableException ex) {
      throw new IllegalStateException("Unable to open TargetDataLine on " + mixerInfo.getName(), ex);
    }
  }
}
