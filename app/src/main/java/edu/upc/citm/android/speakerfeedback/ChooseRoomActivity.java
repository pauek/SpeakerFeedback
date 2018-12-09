package edu.upc.citm.android.speakerfeedback;

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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ChooseRoomActivity extends AppCompatActivity {

    private RecyclerView room_list_view;

    private List<Room> rooms;
    private Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_room);

        rooms = new ArrayList<>();
        rooms.add(new Room("testroom"));
        rooms.add(null);

        room_list_view = findViewById(R.id.room_list_view);
        room_list_view.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new Adapter();
        room_list_view.setAdapter(adapter);
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
