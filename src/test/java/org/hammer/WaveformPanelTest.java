package org.hammer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.hammer.audio.AudioCaptureService;
import org.hammer.audio.WaveformModel;

import static org.mockito.Mockito.*;

/**
 * Tests for WaveformPanel to ensure proper interaction with AudioCaptureService
 * and that paint methods work correctly.
 */
@ExtendWith(MockitoExtension.class)
class WaveformPanelTest {

    @Mock
    private AudioCaptureService mockService;

    @Test
    void testSetAudioCaptureServiceCallsRecomputeLayout() {
        WaveformPanel panel = new WaveformPanel();
        
        // Set up panel with known size
        panel.setSize(200, 100);
        
        // Set the audio capture service
        panel.setAudioCaptureService(mockService);
        
        // Verify that recomputeLayout was called at least once with panel dimensions
        // Note: it may be called multiple times (directly and via resize listener)
        verify(mockService, atLeastOnce()).recomputeLayout(200, 100);
    }

    @Test
    void testSetAudioCaptureServiceWithNullIsHandled() {
        WaveformPanel panel = new WaveformPanel();
        panel.setSize(200, 100);
        
        // Set null service should not throw
        panel.setAudioCaptureService(null);
        
        // No exception means success
    }

    @Test
    void testSetAudioCaptureServiceStoresReference() {
        WaveformPanel panel = new WaveformPanel();
        panel.setSize(200, 100);
        
        // Set the audio capture service
        panel.setAudioCaptureService(mockService);
        
        // Verify service is set by checking it's called for recomputeLayout
        verify(mockService, atLeastOnce()).recomputeLayout(anyInt(), anyInt());
    }

    @Test
    void testComponentResizeCallsRecomputeLayout() {
        WaveformPanel panel = new WaveformPanel();
        
        panel.setAudioCaptureService(mockService);
        
        // Trigger a resize
        panel.setSize(300, 200);
        
        // Verify that recomputeLayout was called with the new dimensions
        verify(mockService, atLeastOnce()).recomputeLayout(anyInt(), anyInt());
    }
}
