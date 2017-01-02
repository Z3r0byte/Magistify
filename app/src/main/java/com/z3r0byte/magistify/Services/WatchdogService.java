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

package com.z3r0byte.magistify.Services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.z3r0byte.magistify.Util.ConfigUtil;
import com.z3r0byte.magistify.Util.ServiceUtil;

import java.util.Timer;
import java.util.TimerTask;

public class WatchdogService extends Service {
    private static final String TAG = "WatchdogService";

    Timer timer = new Timer();

    public WatchdogService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setupWatchdog();
        return START_STICKY;
    }

    private void setupWatchdog() {
        TimerTask checkServices = new TimerTask() {
            @Override
            public void run() {
                ConfigUtil configUtil = new ConfigUtil(getApplicationContext());

                if (!ServiceUtil.isServiceRunning(SessionService.class, getApplicationContext())) {
                    Log.w(TAG, "run: Session service is not running, trying to (re)start it...");
                    startService(new Intent(getApplicationContext(), SessionService.class));
                }

                if (configUtil.getBoolean("new_grade_enabled") &&
                        !ServiceUtil.isServiceRunning(NewGradeService.class, getApplicationContext())) {
                    Log.w(TAG, "run: New grades service is not running, trying to (re)start it...");
                    startService(new Intent(getApplicationContext(), NewGradeService.class));
                }

                if (configUtil.getBoolean("silent_enabled") &&
                        !ServiceUtil.isServiceRunning(AutoSilentService.class, getApplicationContext())) {
                    Log.w(TAG, "run: Auto-silent service is not running, trying to (re)start it...");
                    startService(new Intent(getApplicationContext(), AutoSilentService.class));
                }

                if (configUtil.getBoolean("appointment_enabled") &&
                        !ServiceUtil.isServiceRunning(AppointmentService.class, getApplicationContext())) {
                    Log.w(TAG, "run: Appointment service is not running, trying to (re)start it...");
                    startService(new Intent(getApplicationContext(), AppointmentService.class));
                }
            }
        };
        timer.schedule(checkServices, 0, 60 * 1000);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
