package ch.scs.droener.VisualProcessing;

import android.util.Log;

import org.opencv.core.Mat;

import ch.scs.droener.drone.BebopDrone;

/**
 * Created by armin on 11/30/16.
 */

public class BebopFrameController implements FrameController {

    private static final String TAG = "BebopFrameController";

    /**
     * Pixel to Yaw velocity (0-100).
     */
    private double scalingFactor = 0.1;

    private final FrameProcessor mProcessor;
    private final BebopDrone mDrone;

    private boolean mActive;

    public BebopFrameController(FrameProcessor processor, BebopDrone drone) {
        mProcessor = processor;
        mDrone = drone;
    }

    @Override
    public void processFrame(Mat frame) {
        if (mProcessor == null) {
            return;
        }

        mProcessor.processFrame(frame);
        int deviationX = mProcessor.getDeviationX();
        int deviationY = mProcessor.getDeviationY();

        byte newYaw = (byte) Math.round(deviationX * scalingFactor);
        Log.d(TAG, "X-deviation=" + deviationX + ", Y-deviation=" + deviationY + ", yaw=" + newYaw + ", active=" + mActive);
        if (mActive) {
            mDrone.setYaw(newYaw);
        }
    }

    public boolean isActive() {
        return mActive;
    }

    public void setActive(boolean active) {
        this.mActive = active;
    }
}
