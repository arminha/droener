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
    private double scalingFactor = 0.8;

    private final FrameProcessor mProcessor;
    private final BebopDrone mDrone;

    private boolean mActive;
    private double lastYaw = 0;

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

        double newYaw = Math.round(deviationX * scalingFactor);
        lastYaw = 0.7*lastYaw + 0.3*newYaw;

        Log.d(TAG, "X-deviation=" + deviationX + ", Y-deviation=" + deviationY
                + ", newYaw=" + newYaw+ ", Yaw=" + lastYaw + ", active=" + mActive);
        if (mActive) {
            mDrone.setYaw((byte)lastYaw);
        } else {
            mDrone.setYaw((byte)0);
        }
    }

    public boolean isActive() {
        return mActive;
    }

    public void setActive(boolean active) {
        this.mActive = active;
    }
}
