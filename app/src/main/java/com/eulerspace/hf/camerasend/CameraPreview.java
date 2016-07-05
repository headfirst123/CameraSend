package com.eulerspace.hf.camerasend;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;


public class CameraPreview extends AppCompatActivity
        implements SurfaceHolder.Callback, PreviewCallback {

    private final String Tag = "CameraPreview";
    private final boolean isMultiBroadcast = false;
    DatagramSocket socket;
    InetAddress address;
    WifiManager.MulticastLock lock;
    AVCEncode avcCodec;
    public Camera m_camera;
    SurfaceView m_prevewview;
    SurfaceHolder m_surfaceHolder;
    int width = 1920;
    int height = 1080;
    int framerate = 60;
    int bitrate = 16000000;
    /*
    480P	720X480	1800Kbps
    720P	1280X720	3500Kbps *2
    1080P	1920X1080	8500Kbps *2

    */
    long t1 = 0;
    long t2 = 0;
    long t_send = 0;
    int send_len = 0;
    int frameSize = width * height * 3 / 2;

    byte[] h264 = new byte[frameSize];

    int packetUnitCount = 0;
    int packetUnitIndex = 0;
    int packetUnitLen = 16 * 1024; //16KB
    byte[] packetUnitData = new byte[packetUnitLen];
    int packetOffset = 0;
    DatagramPacket packetUnit = null;

    H264ToMpeg h264ToMpeg = null;
    H264ToFile h264ToFile = null;

    private void setStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectAll()
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build());

    }

    private void init() {
        avcCodec = new AVCEncode(width, height, framerate, bitrate);
        m_prevewview = (SurfaceView) findViewById(R.id.surfaceViewPlay);
        m_surfaceHolder = m_prevewview.getHolder(); // 绑定SurfaceView，取得SurfaceHolder对象
        m_surfaceHolder.setFixedSize(width, height); // 预览大小設置
        m_surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        m_surfaceHolder.addCallback((Callback) this);

        //h264ToMpeg = new H264ToMpeg(avcCodec.mediaCodec.getOutputFormat());
        //h264ToMpeg.start();
        //Collections.binarySearch()
        h264ToFile = new H264ToFile();

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
                    Log.i(Tag, "create MulticastSocket  T " + Thread.currentThread().getId());

                } catch (Exception e) {
                    //// TODO: 2016-06-21
                    e.printStackTrace();
                    Log.e(Tag, "error exception create MulticastSocket" + e.getMessage());
                }
            } else {//UDP
                socket = new DatagramSocket();
                //address = InetAddress.getByName("192.168.43.1");//hotspot
                address = InetAddress.getByName("10.0.0.2");//connect to AP
            }

            t1 = System.currentTimeMillis();
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setStrictMode();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);
        init();
    }

    @Override
    protected void onPause() {
        Log.i(Tag, "onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.i(Tag, "onStop");
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        Log.i(Tag, "onDestroy");
        super.onDestroy();
        this.finish();

    }

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder arg0) {
        initCamera();
    }

    private void initCamera() {
        try {
            m_camera = Camera.open();
            m_camera.setPreviewDisplay(m_surfaceHolder);
            Camera.Parameters parameters = m_camera.getParameters();
            parameters.setPreviewSize(width, height);
            parameters.setPictureSize(width, height);
            parameters.setPreviewFormat(ImageFormat.YV12);
//            List<String> sp = parameters.getSupportedFocusModes();
//            Log.i(Tag, "support focus mode:");
//            for (String a : sp) {
//                Log.i(Tag, a);
//            }
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
//            if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
//                parameters.set("orientation", "portrait");
//                parameters.set("rotation", 90);
//                ;
//                m_camera.setDisplayOrientation(90);
//            } else {
//                parameters.set("orientation", "landscape");
//                m_camera.setDisplayOrientation(0);
//            }
            m_camera.setParameters(parameters);

            //m_camera.setPreviewCallback((PreviewCallback) this);
            byte[] callbackBuffer = new byte[frameSize];
            m_camera.addCallbackBuffer(callbackBuffer);
            m_camera.setPreviewCallbackWithBuffer((PreviewCallback) this);
            m_camera.startPreview();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0) {
        m_camera.setPreviewCallback(null);
        m_camera.stopPreview();
        m_camera.release();
        m_camera = null;
        avcCodec.close();
        Log.i(Tag, "surfaceDestroyeda");
        if (h264ToMpeg != null) {
            h264ToMpeg.release();
            h264ToMpeg = null;
        }
        Log.i(Tag, "surfaceDestroyed0");
        if (h264ToFile != null) {
            h264ToFile.release();
            h264ToFile = null;
        }
        Log.i(Tag, "surfaceDestroyed1");
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        Log.i(Tag, "surfaceDestroyed");
    }


    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

        //Log.i(Tag, "h264 send");
        t_send = System.currentTimeMillis();
        int encode_len = avcCodec.offerEncoder(data, h264, h264ToMpeg);
        if (encode_len > 0) {
//            if (encode_len > 64 * 1024 &&encode_len<65486*2) { // split to two packet
//                int packetLen = 65486;//64*1024;
//                byte[] packetData1 = new byte[packetLen];
//                Log.i(Tag," e len="+encode_len);
//                System.arraycopy(h264, 0, packetData1, 0, packetLen);
//                DatagramPacket packet1 = new DatagramPacket(packetData1, packetLen, address, 5000);
//                sendPacket(packet1);
//
//                System.arraycopy(h264, packetLen, packetData1, 0, encode_len-packetLen);
//                DatagramPacket packet2 = new DatagramPacket(packetData1, encode_len-packetLen, address, 5000);
//                sendPacket(packet2);
//            }
//            else
//            {
//                //maybe should not new object every time,  use setData to update
//                DatagramPacket packet = new DatagramPacket(h264, encode_len, address, 5000);
//                sendPacket(packet);
//            }
            splitPacket(encode_len);
            send_len += encode_len;
            h264ToFile.writeToFile(h264, encode_len);
        }
        camera.addCallbackBuffer(data);
        t2 = System.currentTimeMillis();
        //HashMap
        long speed = send_len / (t2 - t1) * 1000 / 1024;

        Log.i(Tag, " h264 send " + encode_len + "  " + speed + "KB/S  (" + (t2 - t_send) + "ms) " + (isMultiBroadcast ? "MC" : "UDP"));
    }

    // split the h264 frame to many packet unit (default 16KB)
    private void splitPacket(int encode_len) {

        packetUnitCount = encode_len / packetUnitLen;
        packetUnitIndex = 0;
        if (packetUnitData == null) {
            packetUnitData = new byte[packetUnitLen];
        }
        if (packetUnit == null) {
            packetUnit = new DatagramPacket(packetUnitData, packetUnitLen, address, 5000);
        }
        for (packetUnitIndex = 0; packetUnitIndex < packetUnitCount - 1; packetUnitIndex++) {
            packetOffset = packetUnitLen * packetUnitIndex;
            //System.arraycopy(h264, packetOffset, packetUnitData, 0, packetUnitLen);
            packetUnit.setData(h264, packetOffset, packetUnitLen);
            sendPacket(packetUnit);
        }
        //the last packet unit
        packetOffset = packetUnitLen * packetUnitIndex;
        //Log.i(Tag,packetOffset+"  "+(encode_len-packetOffset));
        packetUnit.setData(h264, packetOffset, encode_len - packetOffset);
        sendPacket(packetUnit);
    }

    private boolean sendPacket(DatagramPacket packet) {
        try {
            if (isMultiBroadcast) {
                lock.acquire();
                socket.send(packet);
                lock.release();
            } else
                socket.send(packet);
            return true;
        } catch (IOException e) {
            Log.e(Tag, "send data fail================ " + packet.getLength());
            e.printStackTrace();
        }
        return false;

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i(Tag, "onConfigurationChanged");


    }
}
