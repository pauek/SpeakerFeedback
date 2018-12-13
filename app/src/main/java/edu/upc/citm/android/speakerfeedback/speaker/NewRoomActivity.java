package edu.upc.citm.android.speakerfeedback.speaker;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import edu.upc.citm.android.speakerfeedback.speaker.R;

public class NewRoomActivity extends AppCompatActivity {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private App app;
    private EditText edit_room_id, edit_room_name, edit_password;
    private TextView err_msg_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_room);

        app = (App) getApplicationContext();

        edit_room_id = findViewById(R.id.edit_room_id);
        edit_room_name = findViewById(R.id.edit_room_name);
        edit_password = findViewById(R.id.edit_password);
        err_msg_view = findViewById(R.id.err_msg_view);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.new_room_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.option_create_room:
                checkRoomIsAvailable();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void checkRoomIsAvailable() {
        final String roomId   = edit_room_id.getText().toString();
        final String roomName = edit_room_name.getText().toString();
        final String password = edit_password.getText().toString();

        db.collection("rooms").document(roomId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    if (doc.exists()) {
                        String speakerId = doc.getString("speakerId");
                        if (app.getSpeakerId().equals(speakerId)) {
                            roomIsOk(roomId);
                        } else {
                            roomIsNotOk("Room already exists.");
                        }
                    } else {
                        createRoom(roomId, roomName, password);
                    }
                } else {
                    roomIsNotOk("Error getting room, try again.");
                }
            }
        });
    }

    private void createRoom(final String roomId, String roomName, String password) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("speakerId", app.getSpeakerId());
        fields.put("name", roomName);
        fields.put("password", password);
        db.collection("rooms").document(roomId).set(fields).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    roomIsOk(roomId);
                } else {
                    roomIsNotOk("Error creating room, try again.");
                }
            }
        });
    }

    private void roomIsOk(String roomId) {
        Intent data = new Intent();
        data.putExtra("roomId", roomId);
        setResult(RESULT_OK, data);
        finish();
    }

    private void roomIsNotOk(String message) {
        err_msg_view.setText(message);
        err_msg_view.setVisibility(View.VISIBLE);
    }

}
