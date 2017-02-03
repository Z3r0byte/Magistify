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
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;
import android.util.Log;

import com.z3r0byte.magistify.DatabaseHelpers.CalendarDB;
import com.z3r0byte.magistify.Util.ConfigUtil;

import net.ilexiconn.magister.container.Appointment;
import net.ilexiconn.magister.container.type.AppointmentType;

import java.util.Timer;
import java.util.TimerTask;

public class AutoSilentService extends Service {
    private static final String TAG = "AutoSilentService";

    ConfigUtil configUtil;
    CalendarDB calendarDB;
    Boolean autoSilent;
    Appointment[] appointments;
    Timer timer = new Timer();

    public AutoSilentService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        calendarDB = new CalendarDB(getApplicationContext());
        configUtil = new ConfigUtil(getApplicationContext());
        autoSilent = configUtil.getBoolean("silent_enabled");
        setup();

        Log.i(TAG, "onStartCommand: Starting service...");
        return START_STICKY;
    }

    private void setup() {
        if (autoSilent) {
            TimerTask silentTask = new TimerTask() {
                @Override
                public void run() {
                    appointments = calendarDB.getSilentAppointments(getMargin());
                    if (doSilent(appointments)) {
                        silenced(true);
                        AudioManager audiomanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        configUtil.setInteger("previous_silent_state", audiomanager.getRingerMode());
                        if (audiomanager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                            audiomanager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                        }
                    } else {
                        if (isSilencedByApp()) {
                            AudioManager audiomanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                            if (configUtil.getBoolean("reverse_silent_state")) {
                                audiomanager.setRingerMode(configUtil.getInteger("previous_silent_state"));
                            } else {
                                audiomanager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                            }
                            silenced(false);
                        }
                    }
                }
            };
            Log.d(TAG, "setup: Starting task...");
            timer.schedule(silentTask, 6000, 10 * 1000);
        } else {
            Log.d(TAG, "setup: Service staat uit");
        }
    }

    private Boolean doSilent(Appointment[] appointments) {
        if (appointments.length < 1) {
            Log.d(TAG, "doSilent: No appointments!");
            return false;
        }
        for (Appointment appointment :
                appointments) {
            try {
                if (appointment.type.getID() != AppointmentType.PERSONAL.getID() || configUtil.getBoolean("silent_own_appointments")) {
                    Log.d(TAG, "doSilent: valid appointment");
                    return true;
                } else {
                    Log.d(TAG, "doSilent: No valid appointment");
                }
            } catch (NullPointerException e) {
                Log.d(TAG, "doSilent: No valid appointments found");
            }
        }
        return false;
    }

    private void silenced(Boolean silenced) {
        configUtil.setBoolean("silent", silenced);
    }

    private Boolean isSilencedByApp() {
        return configUtil.getBoolean("silenced");
    }

    private Integer getMargin() {
        Integer margin = configUtil.getInteger("silent_margin");
        if (margin == 1) {
            return 1;
        } else if (margin == 2) {
            return 2;
        } else if (margin == 3) {
            return 3;
        } else if (margin == 4) {
            return 4;
        } else if (margin == 5) {
            return 5;
        } else {
            return 1;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
