package com.eulerspace.hf.camerasend;

import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by eel on 2016-06-21.
 */
public class CameraSurface implements SurfaceHolder.Callback,Camera.PreviewCallback{

    AVCEncode avcCodec;
    public Camera m_camera;
    SurfaceView m_prevewview;
    SurfaceHolder m_surfaceHolder;
    int width = 1920;
    int height = 1080;
    int framerate = 20;
    int bitrate = 2500000;

    byte[] h264 = new byte[width*height*3/2];
    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

    }
}
