package com.example.facedetectionapp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.FaceDetector;

import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


public class FacePreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
	private SurfaceHolder mHolder;
    private Camera mCamera;
    private Bitmap mWorkBitmap;
    
    private static final int NUM_FACES = 5; // max is 64
    
    private FaceDetector mFaceDetector;
    private FaceDetector.Face[] mFaces = new FaceDetector.Face[NUM_FACES];
    private FaceDetector.Face face = null;
    
    private PointF eyesMidPts[] = new PointF[NUM_FACES];
    private float  eyesDistance[] = new float[NUM_FACES];

    private Paint tmpPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int picWidth, picHeight;
    private float ratio, xRatio, yRatio;
    
    private static final String TAG = "CameraPreview";
    
    public FacePreview(Context context) {
        super(context);
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setFormat(ImageFormat.NV21);

        tmpPaint.setStyle(Paint.Style.STROKE);
        tmpPaint.setColor(Color.RED);

        picWidth = 640;
        picHeight = 360;
    }
    
    public void setCamera(Camera camera){
    	mCamera = camera;
    }
    
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {      
        	if (mCamera != null){
        		setWillNotDraw(false);
            	mCamera.setPreviewDisplay(holder);
        	}
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            cameraReleased();
        }
       
    }
    
    /** A safe way to get an instance of the Camera object. */
    public void getCameraInstance(){
        try {
            mCamera = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        	Log.e(TAG, "Erreur " + e.getMessage());
        }
    }
    
    public void cameraReleased(){
   	 	if(mCamera != null) {
   	 		mCamera.stopPreview();
   	 		mCamera.setPreviewCallbackWithBuffer(null);
            mCamera.release();	
            mCamera = null;
        }
   	 	setWillNotDraw(true);
    }
    
    public void surfaceDestroyed(SurfaceHolder holder) {
    	cameraReleased();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Make sure to stop the preview before resizing or reformatting it.
    	Log.d(TAG, String.format("surfaceChanged: format=%d, w=%d, h=%d", format, w, h));
        if (mHolder.getSurface() == null){
          // preview surface does not exist
          return;
        }
        
        try {
            // stop preview before making changes
            mCamera.stopPreview();
        } catch (Exception e){
          // ignore: tried to stop a non-existent preview
        	 cameraReleased();
        }

        Log.d(TAG, "Width: " + w + " Height: " + h);
        
        try {
        	// set preview size and make any resize, rotate or
            // reformatting changes here
        	Camera.Parameters parameters = mCamera.getParameters();
            List<Size> sizes = parameters.getSupportedPreviewSizes();
            Size optimalSize = getOptimalPreviewSize(sizes, w, h);
            parameters.setPreviewFpsRange(30000, 30000);
            parameters.setPreviewSize(optimalSize.width, optimalSize.height);
            // Now that the size is known, set up the camera parameters and begin
            // the preview.
            mCamera.setParameters(parameters);
            
            mCamera.setPreviewDisplay(mHolder);
            // start preview with new settings
            mCamera.startPreview();
            
            //mCamera.startFaceDetection();
            // Setup the objects for the face detection
            mWorkBitmap = Bitmap.createBitmap(optimalSize.width,  optimalSize.height, Bitmap.Config.RGB_565);
            mFaceDetector = new FaceDetector(optimalSize.width,  optimalSize.height, NUM_FACES);

            int bufSize = optimalSize.width * optimalSize.height *
                 ImageFormat.getBitsPerPixel(parameters.getPreviewFormat()) / 8;
            byte[] cbBuffer = new byte[bufSize];
            mCamera.setPreviewCallbackWithBuffer(this);
            //mCamera.setPreviewCallback(this);
            mCamera.addCallbackBuffer(cbBuffer);

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
            cameraReleased();
        }
    }
    
   
    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1	;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
        	Log.d(TAG, "Cool size : " + size.width + " " + size.height);
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    @Override
	public void onPreviewFrame(byte[] data, Camera camera) {
        Log.d(TAG, "onPreviewFrame");

        // face detection: first convert the image from NV21 to RGB_565
        YuvImage yuv = new YuvImage(data, ImageFormat.NV21,
                mWorkBitmap.getWidth(), mWorkBitmap.getHeight(), null);
        // TODO: make rect a member and use it for width and height values above
        Rect rect = new Rect(0, 0, mWorkBitmap.getWidth(), mWorkBitmap.getHeight()); 
        
        // TODO: use a threaded option or a circular buffer for converting streams?  
        //see http://ostermiller.org/convert_java_outputstream_inputstream.html
        ByteArrayOutputStream baout = new ByteArrayOutputStream();
        if (!yuv.compressToJpeg(rect, 100, baout)) {
            Log.e(TAG, "compressToJpeg failed");
        }
        
        BitmapFactory.Options bfo = new BitmapFactory.Options();
        bfo.inPreferredConfig = Bitmap.Config.RGB_565;
        mWorkBitmap = BitmapFactory.decodeStream(
            new ByteArrayInputStream(baout.toByteArray()), null, bfo);

        Arrays.fill(mFaces, null);        // use arraycopy instead?
        Arrays.fill(eyesMidPts, null);        // use arraycopy instead?
        mFaceDetector.findFaces(mWorkBitmap, mFaces);
        Log.d(TAG, ""+ mFaces.length);
        for (int i = 0; i < mFaces.length; i++)
        {
            face = mFaces[i];
            try {
                PointF eyesMP = new PointF();
                face.getMidPoint(eyesMP);
                eyesDistance[i] = face.eyesDistance();
                eyesMidPts[i] = eyesMP;
                
                Log.i("Face",
                       i +  " " + face.confidence() + " " + face.eyesDistance() + " "
                       + "Pose: ("+ face.pose(FaceDetector.Face.EULER_X) + ","
                       + face.pose(FaceDetector.Face.EULER_Y) + ","
                       + face.pose(FaceDetector.Face.EULER_Z) + ")"
                       + "Eyes Midpoint: ("+eyesMidPts[i].x + "," + eyesMidPts[i].y +")"
                );
            }
            catch (Exception e)
            {
                if (true) Log.e("Face", i + " is null");
            }
        }
        
        invalidate(); // use a dirty Rect?

        // Requeue the buffer so we get called again
        mCamera.addCallbackBuffer(data);
	}
    
    @Override
    protected void onDraw(Canvas canvas)
    {
        Log.d(TAG,"onDraw");
        super.onDraw(canvas);
        
        if(mWorkBitmap != null){
        	xRatio = getWidth() * 1.0f / mWorkBitmap.getWidth();
		    yRatio = getHeight() * 1.0f / mWorkBitmap.getHeight();
		
		    for (int i = 0; i < eyesMidPts.length; i++)
		    {
		    	if (eyesMidPts[i] != null)
		        {
		        	ratio = eyesDistance[i] * 4.0f / picWidth;
		        	canvas.drawRect((eyesMidPts[i].x - picWidth * ratio / 2.0f) * xRatio,
                            		(eyesMidPts[i].y - picHeight * ratio / 2.0f) * yRatio,
                            		(eyesMidPts[i].x + picWidth * ratio / 2.0f) * xRatio,
                            		(eyesMidPts[i].y + picHeight * ratio / 2.0f) * yRatio, tmpPaint);
		        }
		    }
        }
    }
   	
}
