/*
 * Copyright (c) 2016-2017 Bas van den Boom 'Z3r0byte'
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.z3r0byte.magistify;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.z3r0byte.magistify.Services.BackgroundService;
import com.z3r0byte.magistify.Services.NewBackgroundService;
import com.z3r0byte.magistify.Util.ServiceUtil;


public class StartActivity extends AppCompatActivity {

    private static final String TAG = "StartActivity";

    Boolean relogin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        if (getSharedPreferences("data", MODE_PRIVATE).getInt("DataVersion", 1) != 3 && getSharedPreferences("data", MODE_PRIVATE).getBoolean("LoggedIn", false)) {
            relogin = true;
            Toast.makeText(StartActivity.this, getString(R.string.msg_old_version), Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, SetupActivity.class));
            finish();
        }

        if (!getSharedPreferences("data", MODE_PRIVATE).getBoolean("LoggedIn", false) || relogin) {
            getSharedPreferences("data", MODE_PRIVATE).edit().putInt("login_version", BuildConfig.VERSION_CODE).apply();
            startActivity(new Intent(this, SetupActivity.class));
            finish();
        } else if (!relogin) {
            if (!ServiceUtil.isServiceRunning(BackgroundService.class, this)) {
                //startService(new Intent(this, BackgroundService.class));
            }

            /*
            if (!ServiceUtil.isServiceRunning(WatchdogService.class, this)) {
                startService(new Intent(this, WatchdogService.class));
            }
            */

            boolean serviceRunning = (PendingIntent.getBroadcast(this, 0,
                    new Intent("com.z3r0byte.magistify.Services.NewBackgroundService"),
                    PendingIntent.FLAG_NO_CREATE) != null);
            if (!serviceRunning) {
                Log.d(TAG, "onCreate: Starting background service");
                NewBackgroundService backgroundService = new NewBackgroundService();
                backgroundService.setAlarm(getApplicationContext());
            } else {
                Log.d(TAG, "onCreate: Not starting background service");
            }

            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        }
    }
}
