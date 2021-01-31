package fr.dzx.audiosource;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class RecordService extends Service {
    private static final String ACTION_RECORD = "fr.dzx.audiosource.RECORD";
    private static final String ACTION_STOP = "fr.dzx.audiosource.STOP";

    private static final String CHANNEL_ID = "audiosource";
    private static final int NOTIFICATION_ID = 1;

    private static final int DEFAULT_SAMPLE_RATE = 44100;
    private static final int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int DEFAULT_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private Thread recorderThread;

    public static void start(Context context) {
        Intent intent = new Intent(context, RecordService.class).setAction(ACTION_RECORD);
        ContextCompat.startForegroundService(context, intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Notification notification = createNotificationStarting();

        if (Build.VERSION.SDK_INT >= 29) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    getString(R.string.app_name), NotificationManager.IMPORTANCE_NONE);
            getNotificationManager().createNotificationChannel(channel);

            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(ACTION_STOP)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (recorderThread != null && recorderThread.isAlive()) {
            return START_NOT_STICKY;
        }

        int minBufSize = AudioRecord.getMinBufferSize(
                DEFAULT_SAMPLE_RATE,
                DEFAULT_CHANNEL_CONFIG,
                DEFAULT_AUDIO_ENCODING);

        AudioRecord recorder = new AudioRecord(
                MediaRecorder.AudioSource.DEFAULT,
                DEFAULT_SAMPLE_RATE,
                DEFAULT_CHANNEL_CONFIG,
                DEFAULT_AUDIO_ENCODING,
                2 * minBufSize);

        if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
			Log.e(App.TAG, "Failed to initialize AudioRecord with default parameters");
            stopSelf();
            return START_NOT_STICKY;
        }

        recorderThread = new RecordThread(this, recorder);
        recorderThread.start();

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (recorderThread != null) {
            recorderThread.interrupt();

            try {
                recorderThread.join();
            } catch (InterruptedException e) {
                Log.e(App.TAG, "RecordThread.join", e);
            }

            recorderThread = null;
        }

        stopForeground(true);
    }

    protected void showNotificationListening() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .addAction(createStopAction())
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getText(R.string.notification_waiting))
                .setSmallIcon(R.drawable.ic_microphone)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
        getNotificationManager().notify(NOTIFICATION_ID, notification);
    }

    protected void showNotificationEstablished() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .addAction(createStopAction())
                .setColor(ContextCompat.getColor(this, R.color.ic_launcher_background))
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getText(R.string.notification_forwarding))
                .setSmallIcon(R.drawable.ic_microphone)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
        getNotificationManager().notify(NOTIFICATION_ID, notification);
    }

    private NotificationManagerCompat getNotificationManager() {
        return NotificationManagerCompat.from(this);
    }

    private Notification createNotificationStarting() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getText(R.string.notification_starting))
                .setSmallIcon(R.drawable.ic_microphone)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
    }

    private Intent createStopIntent() {
        return new Intent(this, RecordService.class)
                .setAction(ACTION_STOP);
    }

    private NotificationCompat.Action createStopAction() {
        Intent stopIntent = createStopIntent();
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent,
                PendingIntent.FLAG_ONE_SHOT);
        String stopString = getString(R.string.action_stop);
        NotificationCompat.Action.Builder actionBuilder = new NotificationCompat.Action.Builder(0
                , stopString, stopPendingIntent);
        return actionBuilder.build();
    }
}
