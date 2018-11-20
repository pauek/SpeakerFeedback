package edu.upc.citm.android.speakerfeedback;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

public class NewPollActivity extends AppCompatActivity {

    private EditText edit_question;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_poll);

        edit_question = findViewById(R.id.edit_question);
    }

    public void onClickAdd(View view) {
        String question = edit_question.getText().toString();
        Intent data = new Intent();
        data.putExtra("question", question);
        setResult(RESULT_OK, data);
        finish();
    }
}
