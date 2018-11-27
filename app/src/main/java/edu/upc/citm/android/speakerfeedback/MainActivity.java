package edu.upc.citm.android.speakerfeedback;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int REGISTER_USER = 0;
    private static final int NEW_POLL = 1;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private DocumentReference roomRef;

    private String userId;
    private List<String> ids = new ArrayList<>();
    private List<Poll> polls = new ArrayList<>();
    private Map<String, Poll> polls_map = new HashMap<>();

    private RecyclerView polls_view;
    private TextView num_users_view;
    private Adapter adapter;
    private ListenerRegistration votesRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        polls_view = findViewById(R.id.polls_view);
        num_users_view = findViewById(R.id.num_users_view);
        adapter = new Adapter();

        polls_view.setLayoutManager(new LinearLayoutManager(this));
        polls_view.setAdapter(adapter);

        getOrRegisterUser();
    }

    private EventListener<DocumentSnapshot> roomListener = new EventListener<DocumentSnapshot>() {
        @Override
        public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
            if (e != null) {
                Log.e("SpeakerFeedback", "Error al rebre rooms/testroom", e);
                return;
            }
            String name = documentSnapshot.getString("name");
            setTitle(name);
        }
    };

    private EventListener<QuerySnapshot> usersListener = new EventListener<QuerySnapshot>() {
        @Override
        public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
            if (e != null) {
                Log.e("SpeakerFeedback", "Error al rebre usuaris dins d'un room", e);
                return;
            }
            num_users_view.setText(Integer.toString(documentSnapshots.size()));
        }
    };

    private EventListener<QuerySnapshot> pollsListener = new EventListener<QuerySnapshot>() {
        @Override
        public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
            if (e != null) {
                Log.e("SpeakerFeedback", "Error al rebre la llista de 'polls'");
                return;
            }
            polls.clear();
            boolean oneIsOpen = false;
            for (DocumentSnapshot doc : documentSnapshots) {
                Poll poll = doc.toObject(Poll.class);
                ids.add(doc.getId());
                polls.add(poll);
                polls_map.put(doc.getId(), poll);
                if (poll.isOpen()) {
                    oneIsOpen = true;
                }
            }
            Log.i("SpeakerFeedback", String.format("He carregat %d polls.", polls.size()));
            adapter.notifyDataSetChanged();
            if (oneIsOpen) {
                addVotesListener();
            } else {
                removeVotesListener();
            }
        }
    };

    private void removeVotesListener() {
        if (votesRegistration != null) {
            votesRegistration.remove();
        }
    }

    private void addVotesListener() {
        votesRegistration = roomRef.collection("votes")
                .addSnapshotListener(this, votesListener);
    }

    private EventListener<QuerySnapshot> votesListener = new EventListener<QuerySnapshot>() {
        @Override
        public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
            // Reset votes for the open Poll
            for (Poll poll : polls) {
                if (poll.isOpen()) {
                    poll.resetVotes();
                }
            }
            // Accumulate votes
            for (DocumentSnapshot doc : documentSnapshots) {
                if (!doc.contains("pollid")) {
                    Log.e("SpeakerFeedback", "Vote is missing 'pollId'");
                    return;
                }
                String pollId = doc.getString("pollid");
                Long vote = doc.getLong("option");
                if (vote == null) {
                    Log.e("SpeakerFeedback", "Vote is missing 'option'");
                    return;
                }
                Poll poll = polls_map.get(pollId);
                if (poll == null) {
                    Log.e("SpeakerFeedback", "Vote for non-existing poll");
                } else if (!poll.isOpen()) {
                    Log.e("SpeakerFeedback", "Vote for an already closed poll");
                } else {
                    poll.addVote((int)(long)vote);
                }
            }
            adapter.notifyDataSetChanged();
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        setUpSnapshotListeners();
    }

    private void setUpSnapshotListeners() {
        roomRef = db.collection("rooms").document("testroom");
        roomRef.addSnapshotListener(this, roomListener);
        roomRef.collection("polls").orderBy("start", Query.Direction.DESCENDING)
                .addSnapshotListener(this, pollsListener);

        db.collection("users").whereEqualTo("room", "testroom")
                .addSnapshotListener(this, usersListener);
    }

    private void getOrRegisterUser() {
        // Busquem a les preferències de l'app l'ID de l'usuari per saber si ja s'havia registrat
        SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
        userId = prefs.getString("userId", null);
        if (userId == null) {
            // Hem de registrar l'usuari, demanem el nom
            Intent intent = new Intent(this, RegisterUserActivity.class);
            startActivityForResult(intent, REGISTER_USER);
            Toast.makeText(this, "Encara t'has de registrar", Toast.LENGTH_SHORT).show();
        } else {
            // Ja està registrat, mostrem el id al Log
            Log.i("SpeakerFeedback", "userId = " + userId);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REGISTER_USER:
                if (resultCode == RESULT_OK) {
                    String name = data.getStringExtra("name");
                    registerUser(name);
                } else {
                    Toast.makeText(this, "Has de registrar un nom", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;

            case NEW_POLL:
                if (resultCode == RESULT_OK) {
                    String question = data.getStringExtra("question");
                    createNewPoll(question);
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void createNewPoll(String question) {
        Poll poll = new Poll(question);
        roomRef.collection("polls").add(poll).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Couldn't add poll", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerUser(String name) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("name", name);
        db.collection("users").add(fields).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                // Toast.makeText(MainActivity.this, "Success!", Toast.LENGTH_SHORT).show();
                // textView.setText(documentReference.getId());
                userId = documentReference.getId();
                SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
                prefs.edit().putString("userId", userId).apply();
                Log.i("SpeakerFeedback", "New user: userId = " + userId);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("SpeakerFeedback", "Error creant objecte", e);
                Toast.makeText(MainActivity.this,
                        "No s'ha pogut registrar l'usuari, intenta-ho més tard", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    public void onAddPoll(View view) {
        Intent intent = new Intent(this, NewPollActivity.class);
        startActivityForResult(intent, NEW_POLL);
    }

    public void onPollClicked(final int pos) {
        final Poll poll = polls.get(pos);
        if (poll.isOpen()) {
            new AlertDialog.Builder(this)
                .setTitle(poll.getQuestion())
                .setItems(new String[]{"Close Poll"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            closePoll(pos);
                        }
                    }
                })
                .create().show();
        }
    }

    public void closePoll(int pos) {
        String id = ids.get(pos);
        Poll poll = polls.get(pos);
        poll.setOpen(false);
        db.collection("rooms").document("testroom").collection("polls").document(id)
                .set(poll).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.i("SpeakerFeedback", "Poll saved");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("SpeakerFeedback", "Poll NOT saved", e);
            }
        });
        removeVotesListener();
    }

    public void onClickUserList(View view) {
        Intent intent = new Intent(this, UserListActivity.class);
        intent.putExtra("roomId", "testroom");
        startActivity(intent);
    }

    private static final int MAX_OPTIONS = 10;
    private static final int option_view_ids[] = { R.id.option1_view, R.id.option2_view, R.id.option3_view, R.id.option4_view, R.id.option5_view };
    private static final int bar_view_ids[]    = { R.id.bar1_view, R.id.bar2_view, R.id.bar3_view, R.id.bar4_view, R.id.bar5_view };
    private static final int count_view_ids[]  = { R.id.count1_view, R.id.count2_view, R.id.count3_view, R.id.count4_view, R.id.count5_view };

    class ViewHolder extends RecyclerView.ViewHolder {
        private CardView card_view;
        private TextView label_view;
        private TextView question_view;
        private TextView[] option_views;
        private View[] bar_views;
        private TextView[] count_views;

        ViewHolder(View itemView) {
            super(itemView);
            card_view     = itemView.findViewById(R.id.card_view);
            label_view    = itemView.findViewById(R.id.label_view);
            question_view = itemView.findViewById(R.id.question_view);

            option_views = new TextView[MAX_OPTIONS];
            for (int i = 0; i < option_view_ids.length; i++) {
                option_views[i] = itemView.findViewById(option_view_ids[i]);
            }
            bar_views = new View[MAX_OPTIONS];
            for (int i = 0; i < bar_view_ids.length; i++) {
                bar_views[i] = itemView.findViewById(bar_view_ids[i]);
            }
            count_views = new TextView[MAX_OPTIONS];
            for (int i = 0; i < count_view_ids.length; i++) {
                count_views[i] = itemView.findViewById(count_view_ids[i]);
            }

            card_view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    onPollClicked(pos);
                }
            });
        }

        void setOptionVisibility(int i, int visibility) {
            option_views[i].setVisibility(visibility);
            bar_views[i].setVisibility(visibility);
            count_views[i].setVisibility(visibility);
        }
    }

    class Adapter extends RecyclerView.Adapter<ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = getLayoutInflater().inflate(R.layout.poll_view, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Poll poll = polls.get(position);
            if (position == 0) {
                holder.label_view.setVisibility(View.VISIBLE);
                if (poll.isOpen()) {
                    holder.label_view.setText(R.string.active);
                } else {
                    holder.label_view.setText(R.string.previous);
                }
            } else {
                if (!poll.isOpen() && polls.get(position-1).isOpen()) {
                    holder.label_view.setVisibility(View.VISIBLE);
                    holder.label_view.setText(R.string.previous);
                } else {
                    holder.label_view.setVisibility(View.GONE);
                }
            }
            float elevation = poll.isOpen() ? 10.0f : 0.0f;
            int bg_color = getResources().getColor(poll.isOpen() ? android.R.color.white : R.color.card_bg);
            holder.card_view.setCardElevation(elevation);
            holder.card_view.setCardBackgroundColor(bg_color);

            int activeColor = Color.rgb(0, 0, 0);
            int passiveColor = Color.rgb(100, 100, 100);

            holder.question_view.setText(poll.getQuestion());
            holder.question_view.setTextColor(poll.isOpen() ? activeColor : passiveColor);

            List<String> options = poll.getOptions();
            for (int i = 0; i < option_view_ids.length; i++) {
                if (i < options.size()) {
                    holder.option_views[i].setText(options.get(i));
                    holder.option_views[i].setTextColor(poll.isOpen() ? activeColor : passiveColor);
                    holder.setOptionVisibility(i, View.VISIBLE);
                } else {
                    holder.setOptionVisibility(i, View.GONE);
                }
                holder.bar_views[i].setAlpha(poll.isOpen() ? 1.0f : 0.25f);
            }
            List<Integer> results = poll.getResults();
            for (int i = 0; i < options.size(); i++) {
                Integer res = null;
                if (results != null && i < results.size()) {
                    res = results.get(i);
                }
                ViewGroup.LayoutParams params = holder.bar_views[i].getLayoutParams();
                params.width = 4;
                int visibility = View.GONE;
                if (res != null) {
                    visibility = View.VISIBLE;
                    params.width += 16 * (int) res;
                    holder.count_views[i].setText(String.format("%d", results.get(i)));
                }
                holder.bar_views[i].setVisibility(visibility);
                holder.count_views[i].setVisibility(visibility);
                holder.bar_views[i].setLayoutParams(params);
            }
        }

        @Override
        public int getItemCount() {
            return polls.size();
        }
    }
}
