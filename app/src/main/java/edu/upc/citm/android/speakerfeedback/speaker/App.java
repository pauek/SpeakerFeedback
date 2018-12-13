package edu.upc.citm.android.speakerfeedback.speaker;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.os.Build;

public class App extends Application {

    public static final String CHANNEL_ID = "SpeakerFeedback";

    private String speakerId;

    public String getSpeakerId() {
        return speakerId;
    }

    public void setSpeakerId(String speakerId) {
        this.speakerId = speakerId;
        SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
        prefs.edit().putString("speakerId", speakerId).apply();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
        readSpeakerId();
    }

    private void readSpeakerId() {
        SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
        speakerId = prefs.getString("speakerId", null);
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SpeakerFeedback Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
}
