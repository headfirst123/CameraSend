package com.eulerspace.hf.camerasend;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by eel on 2016-06-24.
 */
public class H264ToFile {

    File file = null;
    public final String TAG = "H264ToFile";
    FileOutputStream fos = null;


    public void writeToFile(byte[] data, int len) {
        // Log.e(TAG, "writeToFile" + data[0] + data[1] + data[2] + data[3] + " --" + len);
        if (file == null) {
            file = new File("/sdcard/h264.bin");
            if (!file.exists())
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            try {
                fos = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.e(TAG, "file open fail");
                return;
            }
        }
        try {
            fos.write(data, 0, len);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "file write fail");
        }

    }

    public void release() {
        if (fos != null) {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            fos = null;
        }

    }
}
