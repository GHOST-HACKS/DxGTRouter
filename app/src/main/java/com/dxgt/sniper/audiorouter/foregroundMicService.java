package com.dxgt.sniper.audiorouter;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;

public class ForegroundMicService extends Service {
    private MediaProjection mediaProjection;
    private AudioRecord audioRecord;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("resultCode") && intent.hasExtra("data")) {
            int resultCode = intent.getIntExtra("resultCode", -1);
            Intent data = intent.getParcelableExtra("data");
            
            MediaProjectionManager pm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            mediaProjection = pm.getMediaProjection(resultCode, data);
            
            createNotificationChannel();
            startForeground(1, getNotification());
            startAudioCapture();
        }
        return START_STICKY;
    }

    private void startAudioCapture() {
        int bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            audioRecord.startRecording();
        }
    }

    private Notification getNotification() {
        Notification.Builder builder = new Notification.Builder(this)
            .setContentTitle("DxGT Glacier Router")
            .setContentText("🎙️ Active - Sharing mic between PUBG and TikTok")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("audio_channel");
        }
        
        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                "audio_channel", 
                "Audio Routing", 
                NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception e) {}
        }
        if (mediaProjection != null) {
            try {
                mediaProjection.stop();
            } catch (Exception e) {}
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
