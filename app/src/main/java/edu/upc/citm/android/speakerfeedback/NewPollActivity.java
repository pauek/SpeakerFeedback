package edu.upc.citm.android.speakerfeedback;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class NewPollActivity extends AppCompatActivity {

    private ArrayList<String> options;

    private EditText edit_question, edit_option;
    private ListView options_view;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_poll);

        edit_question = findViewById(R.id.edit_question);
        edit_option = findViewById(R.id.edit_option);
        options_view = findViewById(R.id.options_view);

        options = new ArrayList<>();
        options.add("yes");
        options.add("no");

        adapter = new ArrayAdapter<String>(
            this, android.R.layout.simple_list_item_1, options
        );
        options_view.setAdapter(adapter);

        options_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                Toast.makeText(NewPollActivity.this, "Clicked " + options.get(pos), Toast.LENGTH_SHORT).show();
            }
        });

        options_view.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                options.remove(position);
                adapter.notifyDataSetChanged();
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.new_poll_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.option_save:
                savePoll();
                break;

            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public void savePoll() {
        if (options.size() < 2) {
            Toast.makeText(this, "Poll should have at least two options!", Toast.LENGTH_SHORT).show();
            return;
        }
        String question = edit_question.getText().toString();
        Intent data = new Intent();
        data.putExtra("question", question);
        data.putStringArrayListExtra("options", options);
        setResult(RESULT_OK, data);
        finish();
    }

    public void onAddOption(View view) {
        String option = edit_option.getText().toString().trim();
        if (!option.isEmpty()) {
            if (options.indexOf(option) == -1) {
                options.add(option);
                adapter.notifyDataSetChanged();
                edit_option.getText().clear();
            }
        }
    }
}
