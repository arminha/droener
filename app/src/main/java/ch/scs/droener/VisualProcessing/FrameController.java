package ch.scs.droener.VisualProcessing;

import org.opencv.core.Mat;

/**
 * Created by armin on 11/30/16.
 */

public interface FrameController {
    void processFrame(Mat frame);
}
