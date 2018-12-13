package edu.upc.citm.android.speakerfeedback.speaker;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import edu.upc.citm.android.speakerfeedback.speaker.R;

public class UserListActivity extends AppCompatActivity {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private List<String> users;
    private RecyclerView users_view;
    private Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        users = new ArrayList<>();
        adapter = new Adapter();

        users_view = findViewById(R.id.users_view);
        users_view.setLayoutManager(new GridLayoutManager(this, 3));
        users_view.setAdapter(adapter);

        String roomId = getIntent().getStringExtra("roomId");

        db.collection("users").whereEqualTo("room", roomId)
                .addSnapshotListener(this, usersListener);
    }

    private EventListener<QuerySnapshot> usersListener = new EventListener<QuerySnapshot>() {
        @Override
        public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
            users.clear();
            for (DocumentSnapshot doc : documentSnapshots) {
                String name = "<unknown name>";
                if (doc.contains("name")) {
                    name = doc.getString("name");
                }
                users.add(name);
            }
            adapter.notifyDataSetChanged();
        }
    };

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView username_view;

        public ViewHolder(View itemView) {
            super(itemView);
            username_view = itemView.findViewById(R.id.username_view);
        }
    }

    class Adapter extends RecyclerView.Adapter<ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = getLayoutInflater().inflate(R.layout.user_item, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.username_view.setText(users.get(position));
        }

        @Override
        public int getItemCount() {
            return users.size();
        }
    }
}
