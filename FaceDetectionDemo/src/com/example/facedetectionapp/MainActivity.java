package com.example.facedetectionapp;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import com.example.cameraapp.R;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;



public class MainActivity extends Activity {
    // For tap event
    private GestureDetector mGestureDetector;
    private static final String TAG = "MainActivity"; 

    private FacePreview mPreview;

    @Override
    protected void onDestroy()
    {
    	mPreview.cameraReleased();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate() called.");

        setContentView(R.layout.activity_main);
        // For gesture handling.
        mGestureDetector = createGestureDetector(this);
        
        // Create our Preview view and set it as the content of our activity.
       	mPreview = new FacePreview(this);
       	setContentView(mPreview);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_CAMERA) {
            // Stop the preview and release the camera.
            // Execute your logic as quickly as possible
            // so the capture happens quickly.
        	mPreview.cameraReleased();
            return false;
        }
        else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            openOptionsMenu();
             return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event)
    {
        if (mGestureDetector != null) {
            return mGestureDetector.onMotionEvent(event);
        }
        return false;
    }
    
    @Override
    protected void onPause() {
        mPreview.cameraReleased();
        super.onPause();
    }
    
    @Override
    public void onResume() {
        super.onResume();
       	mPreview.getCameraInstance();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection.
        switch (item.getItemId()) {
            case R.id.menu_stop:
            	mPreview.cameraReleased();
            	return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public void onOptionsMenuClosed(Menu menu) {
        // Nothing else to do, closing the Activity.
        finish();
    }
    
    private GestureDetector createGestureDetector(Context context)
    {
        GestureDetector gestureDetector = new GestureDetector(context);
        //Create a base listener for generic gestures
        gestureDetector.setBaseListener( new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
            	 Log.d(TAG,"gesture = " + gesture);
                if (gesture == Gesture.TAP) {
                    //handleGestureTap();
                	 openOptionsMenu();
                    return true;
                } else if (gesture == Gesture.TWO_TAP) {
                    handleGestureTwoTap();
                    return true;
                } else if (gesture == Gesture.SWIPE_RIGHT) {
                    handleGestureSwipeRight();
                    return true;
                } else if (gesture == Gesture.SWIPE_LEFT) {
                    handleGestureSwipeLeft();
                    return true;
                }
                return false;
            }
        });
        return gestureDetector;
    }

    private void handleGestureTwoTap()
    {
    	 Log.d(TAG,"handleGestureTwoTap() called.");
    }

    private void handleGestureSwipeRight()
    {
    	 Log.d(TAG,"handleGestureSwipeRight() called.");
    	 // Quit
         this.finish();
    }

    private void handleGestureSwipeLeft()
    {
    	 Log.d(TAG,"handleGestureSwipeLeft() called.");
    }
    
}
