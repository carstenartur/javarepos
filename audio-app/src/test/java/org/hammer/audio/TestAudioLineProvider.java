package org.hammer.audio;

import static org.mockito.Mockito.*;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.TargetDataLine;

/**
 * Test utility that provides a mock TargetDataLine for unit testing.
 *
 * <p>This implementation returns a Mockito mock configured with basic behaviors needed for testing
 * AudioCaptureServiceImpl without requiring actual audio hardware.
 *
 * @author refactoring
 */
class TestAudioLineProvider implements AudioLineProvider {

  private final TargetDataLine mockLine;
  private final byte[] dataToReturn;

  /**
   * Create a TestAudioLineProvider with a specific buffer size and test data.
   *
   * @param bufferSize the buffer size to return from getBufferSize()
   * @param dataToReturn the byte array to return when read() is called
   */
  TestAudioLineProvider(int bufferSize, byte[] dataToReturn) {
    this.mockLine = mock(TargetDataLine.class);
    this.dataToReturn = dataToReturn;

    when(mockLine.getBufferSize()).thenReturn(bufferSize);

    // Configure read() to copy dataToReturn into the buffer
    when(mockLine.read(any(byte[].class), anyInt(), anyInt()))
        .thenAnswer(
            invocation -> {
              byte[] buffer = invocation.getArgument(0);
              int offset = invocation.getArgument(1);
              int length = invocation.getArgument(2);
              int toCopy = Math.min(length, dataToReturn.length);
              System.arraycopy(dataToReturn, 0, buffer, offset, toCopy);
              return toCopy;
            });
  }

  @Override
  public TargetDataLine acquireLine(AudioFormat format) {
    return mockLine;
  }

  /** Get the mock line for additional stubbing if needed. */
  TargetDataLine getMockLine() {
    return mockLine;
  }
}
