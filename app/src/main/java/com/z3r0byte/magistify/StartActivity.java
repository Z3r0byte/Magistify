package com.z3r0byte.magistify;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import cat.ereza.customactivityoncrash.CustomActivityOnCrash;

public class StartActivity extends AppCompatActivity {

    private static final String TAG = "StartActivity";

    Boolean relogin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CustomActivityOnCrash.install(this);
        setContentView(R.layout.activity_start);

        if (getSharedPreferences("data", MODE_PRIVATE).getInt("DataVersion", 1) != 3 && getSharedPreferences("data", MODE_PRIVATE).getBoolean("LoggedIn", false)) {
            relogin = true;
            Toast.makeText(StartActivity.this, getString(R.string.msg_old_version), Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, SetupActivity.class));
            finish();
        }

        if (!getSharedPreferences("data", MODE_PRIVATE).getBoolean("LoggedIn", false) || relogin) {
            startActivity(new Intent(this, SetupActivity.class));
            finish();
        } else if (!relogin) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        }
    }
}
