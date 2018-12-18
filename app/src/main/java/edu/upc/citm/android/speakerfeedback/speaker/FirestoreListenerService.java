package edu.upc.citm.android.speakerfeedback.speaker;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class FirestoreListenerService extends Service {

    private boolean serviceStarted = false;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String roomId;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("SpeakerFeedback", "FirestoreListenerService.onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("SpeakerFeedback", "FirestoreListenerService.onStartCommand");

        if (!serviceStarted) {
            roomId = intent.getStringExtra("roomId");
            assert(roomId != null);
            createForegroundNotification();
            removeOldUsers();
            serviceStarted = true;
        }

        return START_NOT_STICKY;
    }

    private void createForegroundNotification() {
        Intent intent = new Intent(this, RoomActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        // Crear una notificació i cridar startForeground (perquè el servei segueixi funcionant)
        Notification notification = new NotificationCompat.Builder(this, App.CHANNEL_ID)
                .setContentTitle(getString(R.string.open_room, roomId))
                .setSmallIcon(R.drawable.ic_message)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);
    }

    private Calendar cal = new GregorianCalendar();

    private void removeOldUsers() {
        db.collection("users").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot documentSnapshots) {
                // Compute last weeks date from today's
                cal.setTime(new Date());
                cal.add(Calendar.MONTH, -1);
                Date lastMonth = cal.getTime();

                // Get a list of users without last_active or that were last_active one month ago
                final List<String> toDelete = new ArrayList<>();
                for (DocumentSnapshot doc : documentSnapshots) {
                    Date last_active = doc.getDate("last_active");
                    if (last_active == null || last_active.before(lastMonth)) {
                        toDelete.add(doc.getId());
                    }
                }

                if (toDelete.isEmpty()) {
                    Log.i("SpeakerFeedback", "No users to delete");
                    return;
                }

                Log.i("SpeakerFeedback", String.format("Will remove %d users.", toDelete.size()));

                // Delete them
                db.runTransaction(new Transaction.Function<Void>() {
                    @Nullable
                    @Override
                    public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                        for (String userId : toDelete) {
                            transaction.delete(db.collection("users").document(userId));
                        }
                        return null;
                    }
                }).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i("SpeakerFeedback", "Deleted old users successfully.");
                    }
                });
            }
        });
    }

    @Override
    public void onDestroy() {
        Log.i("SpeakerFeedback", "FirestoreListenerService.onDestroy");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
