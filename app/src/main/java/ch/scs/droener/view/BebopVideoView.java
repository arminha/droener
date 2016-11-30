package ch.scs.droener.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.parrot.arsdk.arcontroller.ARCONTROLLER_STREAM_CODEC_TYPE_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARFrame;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import ch.scs.droener.ImageUtils;

public class BebopVideoView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "BebopVideoView";
    private static final String VIDEO_MIME_TYPE = "video/avc";
    private static final int VIDEO_DEQUEUE_TIMEOUT = 33000;

    private MediaCodec mMediaCodec;
    private Lock mReadyLock;

    private boolean mIsCodecConfigured = false;

    private ByteBuffer mSpsBuffer;
    private ByteBuffer mPpsBuffer;

    private ByteBuffer[] mBuffers;

    private static final int VIDEO_WIDTH = 640;
    private static final int VIDEO_HEIGHT = 368;

    public BebopVideoView(Context context) {
        super(context);
        customInit();
    }

    public BebopVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        customInit();
    }

    public BebopVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        customInit();
    }

    private void customInit() {
        mReadyLock = new ReentrantLock();
        getHolder().addCallback(this);
    }

    public void displayFrame(ARFrame frame) {
        mReadyLock.lock();

        if ((mMediaCodec != null)) {
            if (mIsCodecConfigured) {
                // Here we have either a good PFrame, or an IFrame
                int index = -1;

                try {
                    index = mMediaCodec.dequeueInputBuffer(VIDEO_DEQUEUE_TIMEOUT);
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Error while dequeue input buffer");
                }
                if (index >= 0) {
                    ByteBuffer b;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        b = mMediaCodec.getInputBuffer(index);
                    } else {
                        b = mBuffers[index];
                        b.clear();
                    }

                    if (b != null) {
                        b.put(frame.getByteData(), 0, frame.getDataSize());
                    }

                    try {
                        mMediaCodec.queueInputBuffer(index, 0, frame.getDataSize(), 0, 0);
                    } catch (IllegalStateException e) {
                        Log.e(TAG, "Error while queue input buffer");
                    }
                }
            }

            // Try to display previous frame
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            int outIndex;
            try {
                outIndex = mMediaCodec.dequeueOutputBuffer(info, 0);

                while (outIndex >= 0) {
                    try (Image image = mMediaCodec.getOutputImage(outIndex)) {
                        Mat mat = ImageUtils.imageToMat(image);
                        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(mat, bitmap);
                        drawBitmap(bitmap);
                    }
                    mMediaCodec.releaseOutputBuffer(outIndex, true);
                    outIndex = mMediaCodec.dequeueOutputBuffer(info, 0);
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error while dequeue input buffer (outIndex)");
            }
        }


        mReadyLock.unlock();
    }

    private void drawBitmap(Bitmap bitmap) {
        Canvas canvas = getHolder().lockCanvas();
        if (canvas != null) {
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);

            float scaleHeight = (float) canvas.getHeight() / bitmap.getHeight();
            float scaleWidth = (float) canvas.getWidth() / bitmap.getWidth();
            float scale = Math.min(scaleHeight, scaleWidth);
            canvas.drawBitmap(bitmap, new Rect(0,0,bitmap.getWidth(), bitmap.getHeight()),
                    new Rect((int)((canvas.getWidth() - scale*bitmap.getWidth()) / 2),
                            (int)((canvas.getHeight() - scale*bitmap.getHeight()) / 2),
                            (int)((canvas.getWidth() - scale*bitmap.getWidth()) / 2 + scale*bitmap.getWidth()),
                            (int)((canvas.getHeight() - scale*bitmap.getHeight()) / 2 + scale*bitmap.getHeight())), null);

            getHolder().unlockCanvasAndPost(canvas);
        }
    }

    public void configureDecoder(ARControllerCodec codec) {
        mReadyLock.lock();

        if (codec.getType() == ARCONTROLLER_STREAM_CODEC_TYPE_ENUM.ARCONTROLLER_STREAM_CODEC_TYPE_H264) {
            ARControllerCodec.H264 codecH264 = codec.getAsH264();

            mSpsBuffer = ByteBuffer.wrap(codecH264.getSps().getByteData());
            mPpsBuffer = ByteBuffer.wrap(codecH264.getPps().getByteData());
        }

        if ((mMediaCodec != null) && (mSpsBuffer != null)) {
            configureMediaCodec();
        }

        mReadyLock.unlock();
    }

    private void configureMediaCodec() {
        mMediaCodec.stop();
        MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, VIDEO_WIDTH, VIDEO_HEIGHT);
        format.setByteBuffer("csd-0", mSpsBuffer);
        format.setByteBuffer("csd-1", mPpsBuffer);

        mMediaCodec.configure(format, null, null, 0);
        mMediaCodec.start();

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            mBuffers = mMediaCodec.getInputBuffers();
        }

        mIsCodecConfigured = true;
    }

    private void initMediaCodec(String type) {
        try {
            mMediaCodec = MediaCodec.createDecoderByType(type);
        } catch (IOException e) {
            Log.e(TAG, "Exception", e);
        }

        if ((mMediaCodec != null) && (mSpsBuffer != null)) {
            configureMediaCodec();
        }
    }

    private void releaseMediaCodec() {
        if (mMediaCodec != null) {
            if (mIsCodecConfigured) {
                mMediaCodec.stop();
                mMediaCodec.release();
            }
            mIsCodecConfigured = false;
            mMediaCodec = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mReadyLock.lock();
        initMediaCodec(VIDEO_MIME_TYPE);
        mReadyLock.unlock();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mReadyLock.lock();
        releaseMediaCodec();
        mReadyLock.unlock();
    }
}
