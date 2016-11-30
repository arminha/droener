package ch.scs.droener.VisualProcessing;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

/**
 * Created by Remo on 11/30/2016.
 */
public class HoughCircleTransformation implements FrameProcessor {

    private static int iCannyUpperThreshold = 100;
    private static int iMinRadius = 20;
    private static int iMaxRadius = 400;
    private static int iAccumulator = 300;
    private static int kernelSize = 5;

    private int deviationX = 0;
    private int deviationY = 0;

    @Override
    public void processFrame(Mat image) {
        Mat circles = new Mat();
        Imgproc.medianBlur(image, image, kernelSize);
        Imgproc.cvtColor(image, circles, Imgproc.COLOR_GRAY2BGR);

        Imgproc.HoughCircles(image, circles, Imgproc.CV_HOUGH_GRADIENT,
                2.0, image.rows() / 8, iCannyUpperThreshold, iAccumulator,
                iMinRadius, iMaxRadius);

        int maxRadius = 0;
        Point ptLargestCircle = null;

        if (circles.cols() > 0) {
            for (int x = 0; x < circles.cols(); x++) {
                double vCircle[] = circles.get(0, x);

                if (vCircle == null)
                    break;

                Point pt = new Point(Math.round(vCircle[0]), Math.round(vCircle[1]));
                int radius = (int) Math.round(vCircle[2]);

                if (radius > maxRadius) {
                    ptLargestCircle = pt;
                }

                // draw the found circle
                // draw the circle outline
                Imgproc.circle(image, pt, radius, new Scalar(0, 255, 0), 2);
                // draw the circle center
                Imgproc.circle(image, pt, 3, new Scalar(0, 0, 255), 2);
            }
        }

        if (null != ptLargestCircle) {
            deviationX = (int) ptLargestCircle.x - image.width() / 2;
            deviationY = (int) ptLargestCircle.y - image.height() / 2;
        }
    }

    @Override
    public int getDeviationX() {
        return deviationX;
    }

    @Override
    public int getDeviationY() {
        return deviationY;
    }
}
