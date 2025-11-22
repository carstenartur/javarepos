package org.hammer.audio;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for AudioCaptureServiceImpl to verify model caching behavior.
 */
class AudioCaptureServiceImplTest {

    @Test
    void getLatestModel_returns_empty_when_no_capture() {
        // Create service but don't start it
        AudioCaptureServiceImpl service = new AudioCaptureServiceImpl(
            16000.0f, 16, 1, true, false, 8
        );
        
        // Should return EMPTY model when no data has been captured
        WaveformModel model = service.getLatestModel();
        assertNotNull(model);
        assertEquals(0, model.getNumberOfPoints());
        assertEquals(0, model.getChannelCount());
        assertSame(WaveformModel.EMPTY, model, "Should return EMPTY singleton when no data captured");
    }
    
    @Test
    void getLatestModel_returns_same_instance_when_called_multiple_times() {
        // Create service but don't start it (can't reliably test with actual audio capture in unit test)
        AudioCaptureServiceImpl service = new AudioCaptureServiceImpl(
            16000.0f, 16, 1, true, false, 8
        );
        
        // Get model twice - should be same instance since no update occurred
        WaveformModel model1 = service.getLatestModel();
        WaveformModel model2 = service.getLatestModel();
        
        assertSame(model1, model2, "Should return same cached instance when called multiple times without updates");
    }
}
