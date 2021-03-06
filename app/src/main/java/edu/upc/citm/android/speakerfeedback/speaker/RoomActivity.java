package edu.upc.citm.android.speakerfeedback.speaker;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomActivity extends AppCompatActivity {
    public static final String TAG = "SpeakerFeedback";
    private static final int REGISTER_USER = 0;
    private static final int NEW_POLL = 1;
    private static final int CHOOSE_ROOM = 2;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private DocumentReference roomRef;
    private ListenerRegistration votesRegistration;

    private App app;

    private String roomId;

    private Map<String, String> users = new HashMap<>();
    private Map<Object, String> ids = new HashMap<>();
    private List<Poll> polls = new ArrayList<>();
    private Map<String, Poll> polls_map = new HashMap<>();
    private boolean thereIsAnActivePoll = true;

    private FloatingActionButton btn_add_poll;
    private RecyclerView polls_view;
    private TextView num_users_view, room_id_view;
    private Adapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        app = (App) getApplicationContext();

        polls_view = findViewById(R.id.polls_view);
        num_users_view = findViewById(R.id.num_users_view);
        room_id_view = findViewById(R.id.room_id_view);
        btn_add_poll = findViewById(R.id.btn_add_poll);

        adapter = new Adapter();

        polls_view.setLayoutManager(new LinearLayoutManager(this));
        polls_view.setAdapter(adapter);

        getOrRegisterUser();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (roomId != null) {
            setUpSnapshotListeners();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.room_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.option_exit_room:
                askExitRoom();
                break;

            default:
                return super.onOptionsItemSelected(item);
        }
        return true;

    }

    private EventListener<DocumentSnapshot> roomListener = new EventListener<DocumentSnapshot>() {
        @Override
        public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
            if (e != null) {
                Log.e(TAG, String.format("Error al rebre rooms/%s", roomId), e);
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
                Log.e(TAG, "Error al rebre usuaris dins d'un room", e);
                return;
            }
            users.clear();
            for (DocumentSnapshot doc : documentSnapshots) {
                users.put(doc.getId(), doc.getString("name"));
            }
            num_users_view.setText(Integer.toString(documentSnapshots.size()));
        }
    };

    private EventListener<QuerySnapshot> pollsListener = new EventListener<QuerySnapshot>() {
        @Override
        public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
            Log.i(TAG, "Received poll list");
            if (e != null) {
                Log.e(TAG, "Error al rebre la llista de 'polls'");
                return;
            }
            polls.clear();
            int activePolls = 0;
            for (DocumentSnapshot doc : documentSnapshots) {
                try {
                    Poll poll = doc.toObject(Poll.class);
                    ids.put(poll, doc.getId());
                    polls.add(poll);
                    polls_map.put(doc.getId(), poll);
                    if (poll.isOpen()) {
                        activePolls++;
                    }
                } catch (RuntimeException err) {
                    String msg = String.format("Converison failed for id '%s'", doc.getId());
                    polls.add(Poll.errorPoll(msg));
                    Log.e(TAG, msg);
                }
            }
            thereIsAnActivePoll = (activePolls > 0);
            if (thereIsAnActivePoll) {
                addVotesListener();
            } else {
                removeVotesListener();
            }
            Log.i(TAG, String.format("Loaded %d polls (%d active)", polls.size(), activePolls));
            adapter.notifyDataSetChanged();
            btn_add_poll.setVisibility(thereIsAnActivePoll ? View.GONE : View.VISIBLE);
        }
    };

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
                final String voteId = doc.getId();
                String username = users.get(voteId);
                if (username == null) {
                    username = "<unknown>";
                }
                if (!doc.contains("pollid")) {
                    Log.e(TAG, String.format("Vote by '%s' (%s) is missing 'pollid'", username, doc.getId()));
                    continue;
                }
                String pollId = doc.getString("pollid");
                Long vote = doc.getLong("option");
                if (vote == null) {
                    Log.e(TAG, String.format("Vote by '%s' (%s) is missing 'option' (poll %s)", username, voteId, pollId));
                    continue;
                }
                Poll poll = polls_map.get(pollId);
                if (poll == null) {
                    Log.e(TAG, String.format("Vote by '%s' (%s) is for a non-existing poll (%s)", username, doc.getId(), pollId));
                } else if (!poll.isOpen()) {
                    Log.e(TAG, String.format("Vote by '%s' (%s) is for an already closed poll (%s)", username, doc.getId(), pollId));
                } else {
                    poll.addVote((int)(long)vote);
                }
            }
            adapter.notifyDataSetChanged();
        }
    };


    private void setUpSnapshotListeners() {
        roomRef = db.collection("rooms").document(roomId);
        roomRef.addSnapshotListener(this, roomListener);
        roomRef.collection("polls").orderBy("start", Query.Direction.DESCENDING)
                .addSnapshotListener(this, pollsListener);

        db.collection("users").whereEqualTo("room", roomId)
                .addSnapshotListener(this, usersListener);
    }

    private void removeVotesListener() {
        if (votesRegistration != null) {
            Log.i(TAG, "Removed votes listener.");
            votesRegistration.remove();
        }
    }

    private void addVotesListener() {
        Log.i(TAG, "Added votes listener.");
        votesRegistration = roomRef.collection("votes")
                .addSnapshotListener(this, votesListener);
    }

    private void startFirestoreListenerService() {
        Intent intent = new Intent(this, FirestoreListenerService.class);
        intent.putExtra("roomId", roomId);
        startService(intent);
    }

    private void stopFirestoreListenerService() {
        Intent intent = new Intent(this, FirestoreListenerService.class);
        stopService(intent);
    }

    private void getOrRegisterUser() {
        // Busquem a les preferències de l'app l'ID de l'usuari per saber si ja s'havia registrat
        if (app.getSpeakerId() == null) {
            // Hem de registrar l'usuari, demanem el nom
            Intent intent = new Intent(this, RegisterUserActivity.class);
            startActivityForResult(intent, REGISTER_USER);
            Toast.makeText(this, "Encara t'has de registrar", Toast.LENGTH_SHORT).show();
        } else {
            // Ja està registrat, mostrem el id al Log
            Log.i(TAG, "speakerId = " + app.getSpeakerId());
            getRoom();
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
                    List<String> options = data.getStringArrayListExtra("options");
                    createNewPoll(question, options);
                }
                break;

            case CHOOSE_ROOM:
                if (resultCode == RESULT_OK) {
                    roomId = data.getStringExtra("roomId");
                    enterRoom();
                } else {
                    finish();
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void createNewPoll(String question, List<String> options) {
        Poll poll = new Poll(question, options);
        roomRef.collection("polls").add(poll).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Error adding poll: " + e.toString());
                Toast.makeText(RoomActivity.this, "Couldn't add poll", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerUser(String name) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("name", name);
        db.collection("speakers").add(fields).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                // Toast.makeText(RoomActivity.this, "Success!", Toast.LENGTH_SHORT).show();
                // textView.setText(documentReference.getId());
                app.setSpeakerId(documentReference.getId());
                getRoom();
                Log.i(TAG, "New Speaker: speakerId = " + app.getSpeakerId());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Error creant objecte", e);
                Toast.makeText(RoomActivity.this,
                        "No s'ha pogut registrar l'usuari, intenta-ho més tard", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void getRoom() {
        SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
        roomId = prefs.getString("roomId", null);
        if (roomId == null) {
            Intent intent = new Intent(this, ChooseRoomActivity.class);
            startActivityForResult(intent, CHOOSE_ROOM);
        } else {
            Log.i(TAG, "roomId = " + roomId);
            enterRoom();
        }
    }

    private void enterRoom() {
        room_id_view.setText(roomId);
        setUpSnapshotListeners();
        startFirestoreListenerService();
        db.collection("rooms").document(roomId).update("open", true).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(RoomActivity.this, "Error writing to the room!", Toast.LENGTH_SHORT).show();
            }
        });
        // Save room from config
        SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
        prefs.edit().putString("roomId", roomId).commit();
    }

    private void exitRoom() {
        stopFirestoreListenerService();
        db.collection("rooms").document(roomId).update("open", FieldValue.delete()).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(RoomActivity.this, "Error writing to the room!", Toast.LENGTH_SHORT).show();
            }
        });
        roomId = null;
        // Erase room from config
        SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
        prefs.edit().remove("roomId").commit();
    }

    private void askExitRoom() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.confirmation)
            .setMessage(getString(R.string.sure_close_room, roomId))
            .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    exitRoom();
                    getRoom();
                }
            }).setNegativeButton(android.R.string.cancel, null)
            .create().show();
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
                }).create().show();
        }
        else {
            final String[] options = getClickedPollOptions();
            new AlertDialog.Builder(this)
                .setTitle(poll.getQuestion())
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (options[which].equals(getString(R.string.reopen_poll))) {
                            reopenPoll(pos);
                        } else if (options[which].equals(getString(R.string.delete_poll))) {
                            maybeDeletePoll(pos);
                        }
                    }
                }).create().show();
        }
    }

    @NonNull
    private String[] getClickedPollOptions() {
        // Hide reopen option if a poll is open
        final String[] options;
        if (isSomePollOpen()) {
            options = new String[]{
                getString(R.string.delete_poll)
            };
        } else {
            options = new String[]{
                getString(R.string.reopen_poll),
                getString(R.string.delete_poll)
            };
        }
        return options;
    }

    private boolean isSomePollOpen() {
        boolean somePollIsOpen = false;
        for (Poll p : polls) {
            if (p.isOpen()) {
                somePollIsOpen = true;
            }
        }
        return somePollIsOpen;
    }

    private void reopenPoll(int pos) {
        final Poll poll = polls.get(pos);
        final String pollId = ids.get(poll);
        roomRef.collection("polls").document(pollId).update("open", true)
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "Error reopening poll: " + e.toString());
                }
            });
    }

    public void maybeDeletePoll(final int pos) {
        new AlertDialog.Builder(this)
            .setMessage(getString(R.string.delete_poll_question, polls.get(pos).getQuestion()))
            .setTitle(R.string.confirmation)
            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    deletePoll(pos);
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .create().show();
    }

    private void deletePoll(final int pos) {
        final Poll poll = polls.get(pos);
        final String pollId = ids.get(poll);
        final String question = poll.getQuestion();
        roomRef.collection("polls").document(pollId).delete()
            .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Error deleting poll: " + e.toString());
                }
            })
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                Log.i(TAG, String.format("Delete poll '%s' ('%s')", question, pollId));
                }
            });
    }

    public void closePoll(int pos) {
        final Poll poll = polls.get(pos);
        final String pollId = ids.get(poll);
        poll.setOpen(false);
        poll.setOpen(false);
        roomRef.collection("polls").document(pollId)
                .set(poll).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.i(TAG, "Poll saved");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Poll NOT saved", e);
            }
        });
        removeVotesListener();
    }

    public void onClickUserList(View view) {
        Intent intent = new Intent(this, UserListActivity.class);
        intent.putExtra("roomId", roomId);
        startActivity(intent);
    }

    private static final int MAX_OPTIONS = 6;
    private static final int option_view_ids[] = { R.id.option1_view, R.id.option2_view, R.id.option3_view, R.id.option4_view, R.id.option5_view, R.id.option6_view };
    private static final int bar_view_ids[]    = { R.id.bar1_view, R.id.bar2_view, R.id.bar3_view, R.id.bar4_view, R.id.bar5_view, R.id.bar6_view };
    private static final int count_view_ids[]  = { R.id.count1_view, R.id.count2_view, R.id.count3_view, R.id.count4_view, R.id.count5_view, R.id.count6_view };

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
                    onPollClicked(getAdapterPosition());
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
            final Poll poll = polls.get(position);
            final String pollId = ids.get(poll);
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
                if (options != null && i < options.size()) {
                    holder.option_views[i].setText(options.get(i));
                    holder.option_views[i].setTextColor(poll.isOpen() ? activeColor : passiveColor);
                    holder.setOptionVisibility(i, View.VISIBLE);
                } else {
                    holder.setOptionVisibility(i, View.GONE);
                }
                holder.bar_views[i].setAlpha(poll.isOpen() ? 1.0f : 0.25f);
            }
            List<Integer> results = poll.getResults();
            int size = (options == null ? 0 : options.size());
            if (size > MAX_OPTIONS) {
                Log.e(TAG, String.format("Poll (%s) has more options than the maximum!", pollId));
                size = MAX_OPTIONS;
            }
            for (int i = 0; i < size; i++) {
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
