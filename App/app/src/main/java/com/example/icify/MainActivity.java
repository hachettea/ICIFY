package com.example.icify;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    //Variable
    ImageButton btnRecord;
    ImageButton btnPlay;
    ImageButton btnStop;
    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;

    private static String fileName = null;
    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    MediaPlayer mediaPlayer;
    ListView listView;
    File[] musiques;
    TextView title;
    TextView artist;
    TextView chrono;
    ImageView cover;
    long timerMusique;
    private CountDownTimer countDown;
    boolean timerRunning = false;
    boolean paused = false;
    int length = 0;

    MediaMetadataRetriever metaRetriever ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        metaRetriever = new MediaMetadataRetriever();
        ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.RECORD_AUDIO },PackageManager.PERMISSION_GRANTED);
        ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE },PackageManager.PERMISSION_GRANTED);
        ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE },PackageManager.PERMISSION_GRANTED);

        btnRecord = findViewById(R.id.btnRecord);
        btnPlay = findViewById(R.id.btnPlay);
        btnPlay.setEnabled(false);

        btnStop = findViewById(R.id.btnStop);
        btnStop.setEnabled(false);
        btnStop.setVisibility(View.GONE);

        bufferSize = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        listView = (ListView)findViewById(R.id.listMusique);
        title = (TextView)findViewById(R.id.title);
        artist = (TextView)findViewById(R.id.artist);
        chrono = (TextView)findViewById(R.id.chrono);
        cover = (ImageView)findViewById(R.id.cover);

        initializeListMusiques();
    }

    public void onClick(View v){
        if( v.getId() == R.id.btnRecord && isRecording == false)
        {
            record();
            btnRecord.setImageResource(R.drawable.stop_foreground);
        }
        else if( v.getId() == R.id.btnRecord && isRecording == true)
        {
            stopRecording();
            btnRecord.setImageResource(R.drawable.microphone_foreground);
        }
        else if (v.getId() == R.id.btnPlay && isPlaying == false)
        {
            play(fileName);
            btnPlay.setImageResource(R.drawable.pause_foreground);
            btnStop.setEnabled(true);
            btnStop.setVisibility(View.VISIBLE);
        }
        else if (v.getId() == R.id.btnPlay && isPlaying == true && paused == true)
        {
            musicResume();
            btnPlay.setImageResource(R.drawable.pause_foreground);
            btnStop.setEnabled(true);
            btnStop.setVisibility(View.VISIBLE);
        }
        else if (v.getId() == R.id.btnPlay && isPlaying == true )
        {
            pausesMusique();
            btnPlay.setImageResource(R.drawable.play_foreground);
            btnStop.setEnabled(false);
            btnStop.setVisibility(View.GONE);
        }
        else if (v.getId() == R.id.btnStop && isPlaying == true )
        {
            stopPlaying();
            btnPlay.setImageResource(R.drawable.play_foreground);
            btnStop.setEnabled(false);
            btnStop.setVisibility(View.GONE);
        }
    }

    private void stopPlaying() {
        mediaPlayer.stop();
        mediaPlayer.release();
        stopTimer();
        Toast.makeText(this, "Music stopped", Toast.LENGTH_SHORT).show();
        isPlaying = false;
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

    /**
     * On recuperer le nom du fichier set au bon path.
     * @return le ficheir de la commande audio au .wav
     */
    private String getFilename() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, "AudioRecorder");

        if (!file.exists()) {
            file.mkdirs();
        }

        return (file.getAbsolutePath() + "/" + "commande.wav");
    }

    /**
     * On crée un fichier temporaire pour eviter une perte de data lorsque le buffer est depassé
     * @return
     */
    private String getTempFilename() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, "AudioRecorder");

        if (!file.exists()) {
            file.mkdirs();
        }

        File tempFile = new File(filepath, "record_temp.raw");

        if (tempFile.exists())
            tempFile.delete();

        return (file.getAbsolutePath() + "/" + "record_temp.raw");
    }

    public void pausesMusique(){
        paused = true;
        mediaPlayer.pause();
        stopTimer();
        length = mediaPlayer.getCurrentPosition();
    }

    public void musicResume(){
        paused = false;
        mediaPlayer.seekTo(length);
        mediaPlayer.start();
        startTimer();
    }

    private void record() {
        if (CheckPermissions()) {
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize);

            int i = recorder.getState();
            if (i == 1)
                recorder.startRecording();

            isRecording = true;

            recordingThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    writeAudioDataToFile();
                }
            }, "AudioRecorder Thread");

            recordingThread.start();


        } else {
            RequestPermissions();
        }

    }

    private void writeAudioDataToFile() {
        byte data[] = new byte[bufferSize];
        String filename = getTempFilename();
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int read = 0;

        if (null != os) {
            while (isRecording) {
                read = recorder.read(data, 0, bufferSize);

                if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                    try {
                        os.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopRecording() {
        if (null != recorder) {
            isRecording = false;

            int i = recorder.getState();
            if (i == 1)
                recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;
        }

        copyWaveFile(getTempFilename(), getFilename());
        deleteTempFile();
        /*
        mediaRecorder.stop();
        mediaRecorder.release();
        Toast.makeText(this, "Recording Stopped", Toast.LENGTH_LONG).show();
        */
    }

    private void deleteTempFile() {
        File file = new File(getTempFilename());

        file.delete();
    }

    private void copyWaveFile(String inFilename, String outFilename) {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = 44100;
        int channels = 2;
        long byteRate = 16 * 44100 * channels / 8;

        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;



            WriteWaveFileHeader(out, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);

            while (in.read(data) != -1) {
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void play(String fileName) {
        mediaPlayer = new MediaPlayer();
        DataInputStream dis1 = null;
        byte[] datainBytes1 = null;
        try {
            dis1 = new DataInputStream(new FileInputStream("/storage/emulated/0/Music/"+fileName));
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


        try {

            mediaPlayer.setDataSource("/storage/emulated/0/Music/"+fileName);
            metaRetriever = new MediaMetadataRetriever();

            metaRetriever.setDataSource("/storage/emulated/0/Music/"+fileName);
            String title = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String artist = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String duration = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            timerMusique = Long.parseLong(duration);
            startTimer();
            long timerMusiqueAffichage = timerMusique /1000;
            String chrono = String.format("%d:%02d:%02d",timerMusiqueAffichage / 3600,(timerMusiqueAffichage % 3600) / 60, timerMusiqueAffichage % 60);
            this.title.setText(title);
            this.artist.setText(artist);
            this.chrono.setText(chrono);



            byte [] data = metaRetriever.getEmbeddedPicture();

            if(data != null)
            {
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                cover.setImageBitmap(bitmap); //associated cover art in bitmap
            }
            else
            {
                cover.setImageResource(R.drawable.cover); //any default cover resourse folder
            }

            cover.setAdjustViewBounds(true);
            cover.setLayoutParams(new LinearLayout.LayoutParams(320, 320));

            mediaPlayer.prepare();
            mediaPlayer.start();

            isPlaying = true;
            btnPlay.setImageResource(R.drawable.pause_foreground);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(this, "Playing file", Toast.LENGTH_SHORT).show();
    }

    public void startTimer()
    {
        stopTimer();
        countDown = new CountDownTimer(timerMusique,1000){

            @Override
            public void onTick(long l) {
                timerMusique -= 1000;
                updateTimer();
            }

            @Override
            public void onFinish() {

            }
        }.start();

        timerRunning = true;
    }

    public void stopTimer()
    {
        if(countDown == null)
        {

        }else
        {

            countDown.cancel();

            timerRunning = false;
        }
    }

    public void updateTimer()
    {
        int minutes = (int) timerMusique / 60000;
        int seconds = (int) timerMusique % 60000 / 1000;

        String timeLeftTxt;

        timeLeftTxt = "" +minutes;
        timeLeftTxt +=":";
        if(seconds < 10) timeLeftTxt += "0";
        timeLeftTxt += seconds;
        chrono.setText(timeLeftTxt);
    }


    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen, long totalDataLen, long longSampleRate,
                                     int channels, long byteRate) throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    private void initializeListMusiques()
    {
        List<String> list = new ArrayList<String>();
        String path = Environment.getExternalStorageDirectory().getPath();
        path += "/Music/";
        File file = new File(path);
        musiques = file.listFiles();


        for(int i=0;i<musiques.length;i++)
        {

            metaRetriever.setDataSource(path+"/"+musiques[i].getName());
            String title = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String duration = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            list.add(title);



            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> listView, View itemView, int itemPosition, long itemId)
                {
                    fileName = musiques[itemPosition].getName();
                    stopTimer();
                    btnPlay.setEnabled(true);
                    btnStop.setEnabled(true);
                    paused = false;
                    btnStop.setVisibility(View.VISIBLE);
                    play(fileName);
                }
            });
        }

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,list);
        listView.setAdapter(arrayAdapter);
    }

}