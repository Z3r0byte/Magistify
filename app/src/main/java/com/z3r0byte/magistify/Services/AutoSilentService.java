/*
 * Copyright (c) 2016-2016 Bas van den Boom 'Z3r0byte'
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
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.IBinder;
import android.util.Log;

import com.z3r0byte.magistify.DatabaseHelpers.CalendarDB;
import com.z3r0byte.magistify.GlobalAccount;
import com.z3r0byte.magistify.Util.ConfigUtil;
import com.z3r0byte.magistify.Util.DateUtils;

import net.ilexiconn.magister.Magister;
import net.ilexiconn.magister.container.Appointment;
import net.ilexiconn.magister.container.type.AppointmentType;
import net.ilexiconn.magister.handler.AppointmentHandler;

import java.io.IOException;
import java.util.Date;
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
        //autoSilent = configUtil.getBoolean("auto-silent");
        autoSilent = true;
        setup();
        getAppointments();
        return START_STICKY;
    }

    private void setup() {
        if (autoSilent) {
            TimerTask notificationTask = new TimerTask() {
                @Override
                public void run() {
                    appointments = calendarDB.getSilentAppointments(getMargin());
                    if (doSilent(appointments)) {
                        /*NotificationManager notificationManager =
                                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                                && !notificationManager.isNotificationPolicyAccessGranted()) {
                            Log.w(TAG, "run: Not allowed to change state of do not disturb!");
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && notificationManager.isNotificationPolicyAccessGranted()) {
                            Log.d(TAG, "run: Allowed to change state of Do Not Disturb");
                            silenced(true);
                            AudioManager audiomanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                            if (audiomanager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                                audiomanager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                            }
                        } else {
                            Log.d(TAG, "run: Not Android N of above, so automatically allowed to change state.");
                            silenced(true);
                            AudioManager audiomanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                            if (audiomanager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                                audiomanager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                            }
                        }*/
                        silenced(true);
                        AudioManager audiomanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        if (audiomanager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                            audiomanager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                        }
                    } else {
                        if (isSilencedByApp()) {
                            AudioManager audiomanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                            audiomanager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                            silenced(false);
                        }
                    }
                }
            };
            timer.schedule(notificationTask, 6000, 10 * 1000);
        }
    }

    private void getAppointments() {
        if (autoSilent) {
            TimerTask notificationTask = new TimerTask() {
                @Override
                public void run() {
                    Magister magister = GlobalAccount.MAGISTER;
                    if (magister != null && !magister.isExpired()) {
                        Date start = DateUtils.addDays(DateUtils.getToday(), -2);
                        Date end = DateUtils.addDays(DateUtils.getToday(), 7);
                        AppointmentHandler appointmentHandler = new AppointmentHandler(magister);
                        try {
                            Appointment[] appointments = appointmentHandler.getAppointments(start, end);
                            calendarDB.removeAll();
                            calendarDB.addItems(appointments);
                            Log.d(TAG, "run: New items added");
                        } catch (IOException e) {
                            Log.w(TAG, "run: Failed to get appointments.");
                        } catch (AssertionError e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.e(TAG, "run: Invalid Magister!");
                    }
                }
            };
            timer.schedule(notificationTask, 6000, 120 * 1000); //short refresh time, because of errors that happen sometimes and crash the refresh function.
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
        SharedPreferences prefs = getSharedPreferences("data", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("silent", silenced);
        editor.apply();
    }

    private Boolean isSilencedByApp() {
        SharedPreferences prefs = getSharedPreferences("data", MODE_PRIVATE);
        return prefs.getBoolean("silent", false);
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
