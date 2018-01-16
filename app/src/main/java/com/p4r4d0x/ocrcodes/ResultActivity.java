package com.p4r4d0x.ocrcodes;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class ResultActivity extends AppCompatActivity {
    String achievedElement;
    TextView tvAchievedElement;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        achievedElement = getIntent().getExtras().getString("resultCodeValue", "DefaultItem");

        tvAchievedElement = findViewById(R.id.tv_achieved_element);

        tvAchievedElement.setText(achievedElement);
    }

}
