package ch.scs.droener.VisualProcessing;

import org.opencv.core.Mat;

import ch.scs.droener.drone.BebopDrone;

/**
 * Created by moster on 30.11.2016.
 */

public interface FrameProcessor {
    void processFrame(Mat frame);
    int getDeviationX();
    int getDeviationY();
}
