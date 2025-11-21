package org.hammer;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

/**
 * Eclipse application entry point for Audio Analyzer.
 * This class launches the AudioAnalyseFrame Swing application.
 * 
 * @author chammer
 */
public class AudioApp implements IApplication {

    @Override
    public Object start(IApplicationContext context) throws Exception {
        // Launch the main AudioAnalyseFrame application
        AudioAnalyseFrame.main(new String[0]);
        
        // Keep the application running
        // In a real RCP application, you would wait for the UI to close
        // For now, we return immediately as the Swing app manages its own lifecycle
        return IApplication.EXIT_OK;
    }

    @Override
    public void stop() {
        // Cleanup if needed
    }
}
