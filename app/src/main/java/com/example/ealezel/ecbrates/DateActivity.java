package com.example.ealezel.ecbrates;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class DateActivity extends AppCompatActivity {
    public static final String PREFS_NAME = "SharedPrefRatesDate";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_date);

    }
}