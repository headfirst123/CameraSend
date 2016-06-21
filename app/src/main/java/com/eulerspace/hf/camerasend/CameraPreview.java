package com.eulerspace.hf.camerasend;

import android.content.Context;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;


public class CameraPreview extends AppCompatActivity
        implements SurfaceHolder.Callback, PreviewCallback {

    private final String Tag = "CameraPreview";
    boolean isMultiBroadcast = true;
    DatagramSocket socket;
    InetAddress address;
    WifiManager.MulticastLock lock;
    AVCEncode avcCodec;
    public Camera m_camera;
    SurfaceView m_prevewview;
    SurfaceHolder m_surfaceHolder;
    int width = 1920;
    int height = 1080;
    int framerate = 30;
    int bitrate = 2500000;

    byte[] h264 = new byte[width * height * 3 / 2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectAll()   // or .detectAll() for all detectable problems
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);

        avcCodec = new AVCEncode(width, height, framerate, bitrate);

        m_prevewview = (SurfaceView) findViewById(R.id.surfaceViewPlay);
        m_surfaceHolder = m_prevewview.getHolder(); // 绑定SurfaceView，取得SurfaceHolder对象
        m_surfaceHolder.setFixedSize(width, height); // 预览大小設置
        m_surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        m_surfaceHolder.addCallback((Callback) this);

        try {
            if (isMultiBroadcast)//multicast
            {
                lock = ((WifiManager) this.getSystemService(Context.WIFI_SERVICE)).createMulticastLock("broadcast_lock");
                try {
                    socket = new MulticastSocket();
                    String BROADCAST_IP = "239.10.0.0";
                    //IP协议多点广播地址范围:224.0.0.0---239.255.255.255,其中224.0.0.0为系统自用
                     address = InetAddress.getByName(BROADCAST_IP);
                    ((MulticastSocket) socket).joinGroup(address);
                    Log.i(Tag,"create MulticastSocket");

                } catch (Exception e) {
                    //// TODO: 2016-06-21
                    e.printStackTrace();
                    Log.e(Tag,"error exception create MulticastSocket"+e.getMessage());
                }
            } else {
                socket = new DatagramSocket();
                address = InetAddress.getByName("10.0.0.61");
            }
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }


    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder arg0) {
        try {
            m_camera = Camera.open();
            m_camera.setPreviewDisplay(m_surfaceHolder);
            Camera.Parameters parameters = m_camera.getParameters();
            parameters.setPreviewSize(width, height);
            parameters.setPictureSize(width, height);
            parameters.setPreviewFormat(ImageFormat.YV12);
            List<String> sp = parameters.getSupportedFocusModes();
            Log.i(Tag, "support focus mode:");
            for (String a : sp) {
                Log.i(Tag, a);
            }
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                parameters.set("orientation", "portrait");
                parameters.set("rotation", 90);
                ;
                m_camera.setDisplayOrientation(90);
            } else {
                parameters.set("orientation", "landscape");
                m_camera.setDisplayOrientation(0);
            }
            m_camera.setParameters(parameters);
            m_camera.setPreviewCallback((PreviewCallback) this);
            m_camera.startPreview();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0) {
        m_camera.setPreviewCallback(null); //！！这个必须在前，不然退出出错
        m_camera.stopPreview();
        m_camera.release();
        m_camera = null;
        avcCodec.close();
    }


    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

        Log.i(Tag, "h264 start");
        int ret = avcCodec.offerEncoder(data, h264);

        if (ret > 0) {
            try {
                DatagramPacket packet = new DatagramPacket(h264, ret, address, 5000);
                if (isMultiBroadcast) {
                    lock.acquire();
                    socket.send(packet);
                    lock.release();
                } else
                    socket.send(packet);
            } catch (IOException e) {
                Log.e(Tag,"multicast send error");
                e.printStackTrace();
            }
        }
        Log.i(Tag, "h264 end");

    }

}
