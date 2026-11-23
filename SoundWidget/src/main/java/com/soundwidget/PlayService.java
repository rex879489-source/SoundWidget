package com.soundwidget;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import java.io.File;

public class PlayService extends Service {

    private static MediaPlayer player;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(
                new NotificationChannel("play", "Playing sound", NotificationManager.IMPORTANCE_LOW));
        Notification n = new Notification.Builder(this, "play")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("Playing sound")
                .build();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(1, n);
        }

        stopPlayer(); // tapping again restarts the sound

        File f = new File(getFilesDir(), "sound");
        if (!f.exists()) {
            Toast.makeText(this, "Open the app and choose a sound first", Toast.LENGTH_LONG).show();
            stopSelf();
            return START_NOT_STICKY;
        }

        try {
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            player.setDataSource(f.getAbsolutePath());
            player.setOnCompletionListener(mp -> { stopPlayer(); stopSelf(); });
            player.setOnErrorListener((mp, what, extra) -> { stopPlayer(); stopSelf(); return true; });
            player.prepare();
            player.start();
        } catch (Exception e) {
            stopPlayer();
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private static void stopPlayer() {
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) { }
            player.release();
            player = null;
        }
    }

    @Override
    public void onDestroy() {
        stopPlayer();
        super.onDestroy();
    }
}
