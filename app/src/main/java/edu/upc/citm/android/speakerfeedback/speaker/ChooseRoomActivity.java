package edu.upc.citm.android.speakerfeedback.speaker;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChooseRoomActivity extends AppCompatActivity {

    private App app;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private static final int NEW_ROOM = 1;
    private RecyclerView room_list_view;

    private List<Room> rooms;
    private Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_room);

        app = (App) getApplicationContext();
        rooms = new ArrayList<>();

        room_list_view = findViewById(R.id.room_list_view);
        room_list_view.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new Adapter();
        room_list_view.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        db.collection("rooms").whereEqualTo("speakerId", app.getSpeakerId()).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                rooms.clear();
                for (DocumentSnapshot doc : documentSnapshots) {
                    rooms.add(new Room(doc.getId()));
                }
                rooms.add(null); // This one for the 'Add Room' button!
                adapter.notifyDataSetChanged();
            }
        });
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        CardView card_view;
        LinearLayout add_room_view;
        TextView room_id_view;

        public ViewHolder(View itemView) {
            super(itemView);
            card_view = itemView.findViewById(R.id.card_view);
            add_room_view = itemView.findViewById(R.id.add_room_view);
            room_id_view = itemView.findViewById(R.id.room_id_view);

            card_view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onRoomClick(getAdapterPosition());
                }
            });
        }
    }

    private void onRoomClick(int pos) {
        Room room = rooms.get(pos);
        if (room == null) {
            Intent intent = new Intent(this, NewRoomActivity.class);
            startActivityForResult(intent, NEW_ROOM);
        } else {
            Intent data = new Intent();
            data.putExtra("roomId", rooms.get(pos).getId());
            setResult(RESULT_OK, data);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case NEW_ROOM:
                if (resultCode == RESULT_OK) {
                    setResult(RESULT_OK, data);
                    finish();
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }

    }

    class Adapter extends RecyclerView.Adapter<ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = getLayoutInflater().inflate(R.layout.room_view, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Room room = rooms.get(position);
            if (room == null) {
                // Un room buit significa el botó d'afegir room!
                holder.add_room_view.setVisibility(View.VISIBLE);
                holder.room_id_view.setVisibility(View.GONE);
                holder.card_view.setCardElevation(0.0f);
                holder.card_view.setCardBackgroundColor(getResources().getColor(R.color.add_room));
                return;
            }

            holder.card_view.setCardElevation(5.0f);
            holder.card_view.setCardBackgroundColor(0xffffffff);
            holder.add_room_view.setVisibility(View.GONE);
            holder.room_id_view.setText(rooms.get(position).getId());
        }

        @Override
        public int getItemCount() {
            return rooms.size();
        }
    }

}
