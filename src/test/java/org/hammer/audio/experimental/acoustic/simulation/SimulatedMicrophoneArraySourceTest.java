package org.hammer.audio.experimental.acoustic.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.hammer.audio.acquisition.Microphone;
import org.hammer.audio.acquisition.MicrophoneArray;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.geometry.Vector2;
import org.junit.jupiter.api.Test;

class SimulatedMicrophoneArraySourceTest {

  @Test
  void producesTimestampedMultichannelBlocks() {
    MicrophoneArray array =
        new MicrophoneArray(
            List.of(new Microphone("a", new Vector2(0.0, 0.0), 0), new Microphone("b", new Vector2(0.2, 0.0), 1)));
    SimulatedMicrophoneArraySource source =
        new SimulatedMicrophoneArraySource(
            new Room2D(2.0, 2.0, 0.0, 0.0),
            array,
            List.of(new SoundEmitter2D(new Vector2(1.0, 1.0), Vector2.ZERO, 440.0, 0.1)),
            8_000.0f,
            0.1,
            1L);

    Optional<AudioBlock> block = source.readBlock(128);

    assertTrue(block.isPresent());
    assertEquals(2, block.orElseThrow().channels());
    assertEquals(128, block.orElseThrow().frames());
    assertEquals(0L, block.orElseThrow().timestampNanos());
  }
}
