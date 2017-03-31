package com.cansecs.workholes;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

class Android_Gesture_Detector implements GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener {

    @Override
    public boolean onDown(MotionEvent e) {
        Log.d("Gesture ", " onDown");
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        Log.d("Gesture ", " onSingleTapConfirmed");
        return false;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        Log.d("Gesture ", " onSingleTapUp");
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        Log.d("Gesture ", " onShowPress");
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        Log.d("Gesture ", " onDoubleTap");
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        Log.d("Gesture ", " onDoubleTapEvent");
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        Log.d("Gesture ", " onLongPress");
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

        Log.d("Gesture ", " onScroll");
        if (e1.getY() < e2.getY()){
            Log.d("Gesture ", " Scroll Down");
        }
        if(e1.getY() > e2.getY()){
            Log.d("Gesture ", " Scroll Up");
        }
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e1.getX() < e2.getX()) {
            Log.d("Gesture ", "Left to Right swipe: "+ e1.getX() + " - " + e2.getX());
            Log.d("Speed ", String.valueOf(velocityX) + " pixels/second");
        }
        if (e1.getX() > e2.getX()) {
            Log.d("Gesture ", "Right to Left swipe: "+ e1.getX() + " - " + e2.getX());
            Log.d("Speed ", String.valueOf(velocityX) + " pixels/second");
        }
        if (e1.getY() < e2.getY()) {
            Log.d("Gesture ", "Up to Down swipe: " + e1.getX() + " - " + e2.getX());
            Log.d("Speed ", String.valueOf(velocityY) + " pixels/second");
        }
        if (e1.getY() > e2.getY()) {
            Log.d("Gesture ", "Down to Up swipe: " + e1.getX() + " - " + e2.getX());
            Log.d("Speed ", String.valueOf(velocityY) + " pixels/second");
        }
        return false;

    }
}
