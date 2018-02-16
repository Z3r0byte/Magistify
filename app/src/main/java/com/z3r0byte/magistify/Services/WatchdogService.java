/*
 * Copyright (c) 2016-2018 Bas van den Boom 'Z3r0byte'
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

package com.z3r0byte.magistify.Services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.z3r0byte.magistify.Util.ConfigUtil;
import com.z3r0byte.magistify.Util.DateUtils;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class WatchdogService extends Service {
    private static final String TAG = "Magistify Watchdog";

    Timer timer = new Timer();
    ConfigUtil configUtil;

    public WatchdogService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Woof!");
        setupWatchdog();
        return START_STICKY;
    }

    private void setupWatchdog() {
        TimerTask checkServices = new TimerTask() {
            @Override
            public void run() {
                configUtil = new ConfigUtil(getApplicationContext());
                Date lastrun = DateUtils.parseDate(configUtil.getString("last_service_run"), "dd-MM-YYYY HH:mm:ss");
                Log.d(TAG, "Date from last run: " + configUtil.getString("last_service_run"));
                Log.i(TAG, "Time since last run: " + Math.abs(new Date().getTime() - lastrun.getTime()) / 1000 + " seconds");
                if (Math.abs(new Date().getTime() - lastrun.getTime()) / 1000 > 120) {
                    Log.e(TAG, "Oh boi, something isn't right...");
                    Log.d(TAG, "Lemme fix this real quick");
                    BackgroundService backgroundService = new BackgroundService();
                    backgroundService.setAlarm(getApplicationContext());
                    Log.d(TAG, "This should do the trick");
                } else {
                    Log.d(TAG, "Everything is working like a charm :D");
                }
            }
        };
        timer.schedule(checkServices, 120 * 1000, 60 * 1000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        startService(new Intent(getApplicationContext(), WatchdogService.class));
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
