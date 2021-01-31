package fr.dzx.audiosource;

import android.media.AudioRecord;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Build;
import android.system.Os;
import android.util.Log;

import java.io.IOException;

public class RecordThread extends Thread {
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNELS = 1;

    private static final String SOCKET_NAME = "audiosource";

    private RecordService service;
    private AudioRecord recorder;

    private volatile LocalServerSocket serverSocket;

    RecordThread(RecordService service, AudioRecord recorder) {
        this.service = service;
        this.recorder = recorder;
    }

    @Override
    public void run() {
        try {
            serverSocket = new LocalServerSocket(SOCKET_NAME);
        } catch (IOException e) {
            Log.e(App.TAG, "LocalServerSocket (bind)", e);
        }

        while (!Thread.currentThread().isInterrupted()) {
            service.showNotificationListening();

            try (LocalSocket socket = serverSocket.accept()) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

                service.showNotificationEstablished();
                recorder.startRecording();

                int BUFFER_MS = 15; // Do not buffer more than BUFFER_MS milliseconds
                byte[] buf = new byte[SAMPLE_RATE * CHANNELS * BUFFER_MS / 1000];

                while (!Thread.currentThread().isInterrupted()) {
                    int r = recorder.read(buf, 0, buf.length);

                    if (r < 0) {
                        break;
                    }

                    socket.getOutputStream().write(buf, 0, r);
                }
            } catch (IOException e) {
                Log.e(App.TAG, "LocalSocket", e);
            } finally {
                recorder.stop();
            }
        }

        try {
            serverSocket.close();
        } catch (IOException e) {
            Log.e(App.TAG, "LocalServerSocket (close)", e);
        }
    }

    public void interrupt() {
        super.interrupt();

        if (Build.VERSION.SDK_INT >= 21) {
            // Manually shutdown the FD to interrupt accept
            try {
                Os.shutdown(serverSocket.getFileDescriptor(), 0);
            } catch (Exception e) {
                Log.e(App.TAG, "os.shutdown", e);
            }
        } else {
            // Connect back to interrupt accept
            try (LocalSocket socket = new LocalSocket()) {
                socket.connect(new LocalSocketAddress(SOCKET_NAME));
            } catch (IOException e) {
                Log.e(App.TAG, "LocalSocket (connect back)", e);
            }
        }
    }
}
