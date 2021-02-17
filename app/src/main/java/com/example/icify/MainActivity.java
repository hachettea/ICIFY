package com.example.icify;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    //Variable
    Button btnRecord,btnStopRecord,btnPlay,btnStop;
    MediaRecorder mediaRecorder;
    private static String fileName = null;
    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.RECORD_AUDIO },PackageManager.PERMISSION_GRANTED);
        ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE },PackageManager.PERMISSION_GRANTED);
        ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE },PackageManager.PERMISSION_GRANTED);


    }

    public void onClick(View v){
        if( v.getId() == R.id.btnRecord)
        {
            record();
        }
        else if(v.getId() == R.id.btnStop)
        {
            stopAudio();
        }
        else if (v.getId() == R.id.btnPlay)
        {
            play();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_AUDIO_PERMISSION_CODE:
                if (grantResults.length > 0) {
                    boolean permissionToRecord = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean permissionToStore = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (permissionToRecord && permissionToStore) {
                        Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    public boolean CheckPermissions() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }

    private void RequestPermissions() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_AUDIO_PERMISSION_CODE);
    }


    private void record() {
        if (CheckPermissions()) {
            fileName = Environment.getExternalStorageDirectory().getAbsolutePath();
            fileName += "/AudioRecording.3gp";

            mediaRecorder = new MediaRecorder();

            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            mediaRecorder.setOutputFile(fileName);
            try {
                mediaRecorder.prepare();
            } catch (IOException e) {
                Log.e("TAG", "prepare() failed");
            }
            mediaRecorder.start();
            Toast.makeText(this, "Recording...", Toast.LENGTH_LONG).show();
        } else {
            RequestPermissions();
        }

    }

    private void stopAudio() {
        mediaRecorder.stop();
        mediaRecorder.release();
        Toast.makeText(this, "Recording Stopped", Toast.LENGTH_LONG).show();
    }

    private void play() {
        DataInputStream dis1 = null;
        byte[] datainBytes1 = null;
        try {
            dis1 = new DataInputStream( new FileInputStream(fileName));
            datainBytes1 = new byte[dis1.available()];
            dis1.readFully(datainBytes1);
            dis1.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        float[] dst = new float[0];
        if(datainBytes1 != null) {
            final FloatBuffer fb = ByteBuffer.wrap(datainBytes1).asFloatBuffer();
            dst = new float[fb.capacity()];
            fb.get(dst); // Copy the contents of the FloatBuffer into dst
        }
        if( dst != null)
        {
            System.out.println(dst[0]);
        }
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(fileName);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(this, "Playing recorded audio", Toast.LENGTH_LONG).show();
    }
}