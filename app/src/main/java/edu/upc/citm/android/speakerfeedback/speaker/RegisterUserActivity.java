package edu.upc.citm.android.speakerfeedback.speaker;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import edu.upc.citm.android.speakerfeedback.speaker.R;

public class RegisterUserActivity extends AppCompatActivity {

    private EditText edit_name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_user);

        edit_name = findViewById(R.id.edit_name);
    }

    public void onSaveName(View view) {
        String name = edit_name.getText().toString();
        Intent data = new Intent();
        data.putExtra("name", name);
        setResult(RESULT_OK, data);
        finish();
    }
}
