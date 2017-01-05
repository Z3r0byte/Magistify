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

import com.google.gson.Gson;
import com.z3r0byte.magistify.DatabaseHelpers.CalendarDB;
import com.z3r0byte.magistify.GlobalAccount;
import com.z3r0byte.magistify.Util.ConfigUtil;
import com.z3r0byte.magistify.Util.DateUtils;

import net.ilexiconn.magister.Magister;
import net.ilexiconn.magister.container.Appointment;
import net.ilexiconn.magister.container.Profile;
import net.ilexiconn.magister.container.School;
import net.ilexiconn.magister.container.User;
import net.ilexiconn.magister.handler.AppointmentHandler;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.text.ParseException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class SessionService extends Service {
    private static final String TAG = "SessionService";

    Timer timer = new Timer();

    CalendarDB calendarDB;
    ConfigUtil configUtil;

    public SessionService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        configUtil = new ConfigUtil(getApplicationContext());
        User user = new Gson().fromJson(configUtil.getString("User"), User.class);
        School school = new Gson().fromJson(configUtil.getString("School"), School.class);
        Profile profile = new Gson().fromJson(configUtil.getString("Profile"), Profile.class);
        GlobalAccount.USER = user;
        GlobalAccount.PROFILE = profile;
        sessionReloader(user, school);

        calendarDB = new CalendarDB(getApplicationContext());
        getAppointments();
        return START_STICKY;
    }

    private void sessionReloader(final User user, final School school) {
        TimerTask refreshSession = new TimerTask() {
            @Override
            public void run() {
                if (GlobalAccount.MAGISTER == null) {
                    if (configUtil.getInteger("failed_auth") >= 2) {
                        Log.w(TAG, "run: Warning! 2 Failed authentications, aborting for user's safety!");
                        return;
                    }
                    try {
                        Log.d(TAG, "run: initiating session");
                        Magister magister = Magister.login(school, user.username, user.password);
                        GlobalAccount.MAGISTER = magister;
                        GlobalAccount.PROFILE = magister.profile;
                        GlobalAccount.USER = magister.user;
                        configUtil.setInteger("failed_auth", 0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    } catch (InvalidParameterException e) {
                        int fails = configUtil.getInteger("failed_auth");
                        fails++;
                        configUtil.setInteger("failed_auth", fails);
                        Log.w(TAG, "run: Amount of failed Authentications: " + fails);
                    }
                } else {
                    try {
                        GlobalAccount.MAGISTER.login();
                        Log.d(TAG, "run: refreshing session");
                        configUtil.setInteger("failed_auth", 0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InvalidParameterException e) {
                        int fails = configUtil.getInteger("failed_auth");
                        fails++;
                        configUtil.setInteger("failed_auth", fails);
                        Log.w(TAG, "run: Amount of failed Authentications: " + fails);
                    }
                }

            }
        };
        timer.schedule(refreshSession, 0, 120 * 1000);


    }


    private void getAppointments() {
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
        timer.schedule(notificationTask, 6000, 60 * 1000); //short refresh time, because of errors that happen sometimes and crash the refresh function.
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
