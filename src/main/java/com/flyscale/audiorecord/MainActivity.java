package com.flyscale.audiorecord;

import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private AudioRecord mAudioRecord;
    private static final int SAMPLE_RATE = 44100;//采样率
    private static final int CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO;//通道配置
    private static final int CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO;//通道配置
    private static final int ENCODING_FORMAT = AudioFormat.ENCODING_PCM_16BIT;//编码配置
    private static final String PCM_FILE = "/mnt/sdcard/test.pcm";
    private static final String TAG = "MainActivity";
    private AudioTrack mAudioTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.start_record).setOnClickListener(this);
        findViewById(R.id.stop_record).setOnClickListener(this);
        findViewById(R.id.start_play).setOnClickListener(this);
        findViewById(R.id.stop_play).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_record:
                startRecord();
                break;
            case R.id.stop_record:
                stopRecord();
                break;
            case R.id.start_play:
                play();
                break;
            case R.id.stop_play:
                stopPlay();
                break;

        }
    }


    private void startRecord() {
        Log.d(TAG, "startRecord");
        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, ENCODING_FORMAT);
        mAudioRecord = new AudioRecord(
                MediaRecorder.AudioSource.DEFAULT,//默认音频源
                SAMPLE_RATE,    //采样率
                CHANNEL_CONFIG_IN, //通道
                ENCODING_FORMAT,    //表示格式
                minBufferSize); //最小缓存

        mAudioRecord.startRecording();
        startReadThread();
    }

    /**
     * 读取音频数据并写入文件
     */
    private void startReadThread() {
        Log.d(TAG, "startReadThread");
        new Thread() {
            @Override
            public void run() {
                super.run();
                short[] buf = new short[1024];//一次读取的音频数据大小
                BufferedOutputStream bos = null;
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(PCM_FILE);
                    bos = new BufferedOutputStream(fos);
                    while (mAudioRecord.read(buf, 0, buf.length) > 0) {
                        byte[] bytes = ArrayUtil.toByteArraySmallEnd(buf);
                        bos.write(bytes);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        bos.close();
                        fos.close();
                        if (mAudioRecord != null) {
                            mAudioRecord.stop();
                            mAudioRecord.release();
                            mAudioRecord = null;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private void stopRecord() {
        Log.d(TAG, "stopRecord");
        if (mAudioRecord == null) {
            Toast.makeText(this, "请先开始录音", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Recorder state=" + mAudioRecord.getState());
        mAudioRecord.stop();
        mAudioRecord.release();
        mAudioRecord = null;
        Toast.makeText(this, "文件已保存：" + PCM_FILE, Toast.LENGTH_LONG).show();
    }

    private void play() {
        File file = new File(PCM_FILE);
        if (!file.exists()) {
            Toast.makeText(this, "请先开始录音", Toast.LENGTH_SHORT).show();
            return;
        }
        int minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, ENCODING_FORMAT);
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG_OUT,
                ENCODING_FORMAT,
                minBufferSize,
                AudioTrack.MODE_STREAM);
        mAudioTrack.play();
        new Thread() {
            @Override
            public void run() {
                super.run();
                FileInputStream fis = null;
                BufferedInputStream bis = null;
                byte[] buf = new byte[1024];
                try {
                    fis = new FileInputStream(PCM_FILE);
                    bis = new BufferedInputStream(fis);
                    int read = 0;
                    while ((read = bis.read(buf)) > 0) {
                        mAudioTrack.write(buf, 0, read);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        bis.close();
                        fis.close();
                        if (mAudioTrack != null) {
                            mAudioTrack.stop();
                            mAudioTrack.release();
                            mAudioTrack = null;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private void stopPlay() {
        if (mAudioTrack == null) {
            Toast.makeText(this, "请先开始播放", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "stopPlay,state=" + mAudioTrack.getState());
        mAudioTrack.stop();
        mAudioTrack.release();
        mAudioTrack = null;
    }

}
