package com.albaz.appuser.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.albaz.appuser.R;

public class message extends AppCompatActivity {

    private String message;

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        Intent iin = getIntent();
        Bundle b = iin.getExtras();
        message = b.get("message").toString(); // Set Message

        textView = findViewById(R.id.message);
        textView.setText(message); // Set Message In TextView
    }
}
