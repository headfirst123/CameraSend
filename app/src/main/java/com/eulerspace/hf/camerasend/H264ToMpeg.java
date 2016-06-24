package com.eulerspace.hf.camerasend;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by eel on 2016-06-23.
 */
public class H264ToMpeg {

    private final String Tag = "H264ToMpeg";
    MediaMuxer muxer = null;
    private int mTrackIndex = 0;
    private MediaCodec.BufferInfo mBufferInfo;

    public H264ToMpeg(MediaFormat videoFormat) {
        try {
            muxer = new MediaMuxer("/sdcard/source.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            Log.e(Tag, "create MediaMuxer fail");
            e.printStackTrace();
        }
        // More often, the MediaFormat will be retrieved from MediaCodec.getOutputFormat()
        // or MediaExtractor.getTrackFormat().
        mTrackIndex = muxer.addTrack(videoFormat);

    }

    public void start() {
        muxer.start();
    }

    public void onFrame(ByteBuffer inputBuffer, MediaCodec.BufferInfo mBufferInfo) {
        muxer.writeSampleData(mTrackIndex, inputBuffer, mBufferInfo);
        Log.i(Tag, "write to file ");
    }

    public void release() {
        if (muxer != null) {
            muxer.stop();
            muxer.release();
            muxer = null;
        }
    }

}
