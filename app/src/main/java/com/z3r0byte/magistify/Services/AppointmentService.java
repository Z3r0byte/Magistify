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

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.gson.Gson;
import com.z3r0byte.magistify.DatabaseHelpers.CalendarDB;
import com.z3r0byte.magistify.R;
import com.z3r0byte.magistify.Util.ConfigUtil;
import com.z3r0byte.magistify.Util.DateUtils;

import net.ilexiconn.magister.container.Appointment;
import net.ilexiconn.magister.container.type.AppointmentType;

import java.util.Timer;
import java.util.TimerTask;

public class AppointmentService extends Service {
    private static final String TAG = "AppointmentService";

    CalendarDB calendarDB;
    ConfigUtil configUtil;
    Timer timer = new Timer();

    String previousAppointment;

    public AppointmentService() {
        notificationTimer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        calendarDB = new CalendarDB(getApplicationContext());
        configUtil = new ConfigUtil(getApplicationContext());

        return START_STICKY;
    }

    private void notificationTimer() {
        TimerTask notificationTask = new TimerTask() {
            @Override
            public void run() {
                Gson gson = new Gson();
                Appointment[] appointments = calendarDB.getNotificationAppointments();
                Log.d(TAG, "run: amount " + appointments.length);
                if (appointments.length >= 1) {
                    Appointment appointment = appointments[0];
                    if (!gson.toJson(appointment).equals(previousAppointment) && isCandidate(appointment) || true) {
                        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext());
                        mBuilder.setSmallIcon(R.drawable.ic_appointment);

                        if (appointment.startDate != null) {
                            String time = DateUtils.formatDate(appointment.startDate, "HH:mm");
                            mBuilder.setContentTitle("Volgende afspraak (" + time + ")");
                        } else {
                            mBuilder.setContentTitle("Volgende afspraak:");
                        }
                        mBuilder.setContentText(appointment.description + " in " + appointment.location);
                        mBuilder.setAutoCancel(true);
                        mBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

                        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        mNotificationManager.notify(9991, mBuilder.build());
                    }
                } else {
                    NotificationManager notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    notifManager.cancel(9991);
                }
            }
        };
        timer.schedule(notificationTask, 20000, 60 * 1000);
    }

    private boolean isCandidate(Appointment appointment) {
        if (configUtil.getBoolean("show_own_appointments")) {
            return true;
        } else {
            if (appointment.type == AppointmentType.PERSONAL) {
                return false;
            } else {
                return true;
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
