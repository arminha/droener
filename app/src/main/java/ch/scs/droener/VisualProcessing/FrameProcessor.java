package ch.scs.droener.VisualProcessing;

import org.opencv.core.Mat;

/**
 * Created by moster on 30.11.2016.
 */

public interface FrameProcessor {
    void processFrame(Mat frame);
    double getXDeviation();
}
