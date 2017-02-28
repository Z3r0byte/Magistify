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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.google.gson.Gson;
import com.z3r0byte.magistify.AppointmentActivity;
import com.z3r0byte.magistify.DashboardActivity;
import com.z3r0byte.magistify.DatabaseHelpers.CalendarDB;
import com.z3r0byte.magistify.DatabaseHelpers.NewGradesDB;
import com.z3r0byte.magistify.GlobalAccount;
import com.z3r0byte.magistify.NewGradeActivity;
import com.z3r0byte.magistify.R;
import com.z3r0byte.magistify.Util.ConfigUtil;
import com.z3r0byte.magistify.Util.DateUtils;

import net.ilexiconn.magister.Magister;
import net.ilexiconn.magister.container.Appointment;
import net.ilexiconn.magister.container.Grade;
import net.ilexiconn.magister.container.Profile;
import net.ilexiconn.magister.container.School;
import net.ilexiconn.magister.container.User;
import net.ilexiconn.magister.container.type.AppointmentType;
import net.ilexiconn.magister.handler.AppointmentHandler;
import net.ilexiconn.magister.handler.GradeHandler;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class BackgroundService extends Service {
    private static final String TAG = "BackgroundService";

    Timer timer = new Timer();

    CalendarDB calendarDB;
    ConfigUtil configUtil;

    String previousAppointment;

    Boolean autoSilent;
    Appointment[] appointments;

    public BackgroundService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        configUtil = new ConfigUtil(getApplicationContext());
        User user = new Gson().fromJson(configUtil.getString("User"), User.class);
        School school = new Gson().fromJson(configUtil.getString("School"), School.class);
        Profile profile = new Gson().fromJson(configUtil.getString("Profile"), Profile.class);
        GlobalAccount.USER = user;
        GlobalAccount.PROFILE = profile;
        calendarDB = new CalendarDB(getApplicationContext());

        sessionTimer(user, school);
        loadAppointmentTimer();

        if (configUtil.getBoolean("appointment_enabled")) {
            notifyAppoinytmentTimer();
        }

        if (configUtil.getBoolean("silent_enabled")) {
            autoSilentTimer();
        }

        if (configUtil.getBoolean("new_grade_enabled")) {
            gradeTimer();
        }
        return START_STICKY;
    }

    /*
    Session management
     */

    private void sessionTimer(final User user, final School school) {
        TimerTask refreshSession = new TimerTask() {
            @Override
            public void run() {
                if (GlobalAccount.MAGISTER == null) {
                    if (configUtil.getInteger("failed_auth") >= 2) {
                        Log.w(TAG, "run: Warning! 2 Failed authentications, aborting for user's safety!");
                        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext());
                        mBuilder.setSmallIcon(R.drawable.ic_error);

                        Intent resultIntent = new Intent(getApplicationContext(), DashboardActivity.class);
                        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
                        stackBuilder.addParentStack(DashboardActivity.class);
                        stackBuilder.addNextIntent(resultIntent);
                        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
                                PendingIntent.FLAG_UPDATE_CURRENT);

                        mBuilder.setContentIntent(resultPendingIntent);
                        mBuilder.setContentTitle(getString(R.string.dialog_login_failed_title));
                        mBuilder.setContentText(getString(R.string.msg_fix_login));
                        mBuilder.setAutoCancel(true);
                        mBuilder.setSound(null);
                        mBuilder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE);

                        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        mNotificationManager.notify(666, mBuilder.build());
                        return;
                    }
                    try {
                        Log.d(TAG, "run: initiating session");
                        Magister magister = Magister.login(school, user.username, user.password);
                        GlobalAccount.MAGISTER = magister;
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


    private void loadAppointmentTimer() {
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

    /*
    Appointment notifications
     */

    private void notifyAppoinytmentTimer() {
        TimerTask notificationTask = new TimerTask() {
            @Override
            public void run() {
                Gson gson = new Gson();
                Appointment[] appointments = calendarDB.getNotificationAppointments();
                Log.d(TAG, "run: amount " + appointments.length);
                previousAppointment = configUtil.getString("previous_appointment");
                if (appointments.length >= 1) {
                    Appointment appointment = appointments[0];
                    if (!gson.toJson(appointment).equals(previousAppointment) && isCandidate(appointment)) {
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

                        Intent resultIntent = new Intent(getApplicationContext(), AppointmentActivity.class);
                        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
                        stackBuilder.addParentStack(AppointmentActivity.class);
                        stackBuilder.addNextIntent(resultIntent);
                        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                        mBuilder.setContentIntent(resultPendingIntent);

                        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        mNotificationManager.notify(9991, mBuilder.build());

                        previousAppointment = gson.toJson(appointment);
                        configUtil.setString("previous_appointment", previousAppointment);
                    }
                } else {
                    NotificationManager notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    notifManager.cancel(9991);
                }
            }
        };
        timer.schedule(notificationTask, 20000, 30 * 1000);
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


    /*
    Auto-silent
     */

    private void autoSilentTimer() {
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

    /*
    New Grade Notification
     */

    private void gradeTimer() {
        TimerTask gradeStack = new TimerTask() {
            @Override
            public void run() {
                Magister magister = GlobalAccount.MAGISTER;
                if (magister == null || magister.isExpired()) {
                    Log.e(TAG, "run: Invalid magister");
                    return;
                }

                NewGradesDB gradesdb = new NewGradesDB(getApplicationContext());

                GradeHandler gradeHandler = new GradeHandler(magister);
                Grade[] gradeArray;
                List<Grade> gradeList = new ArrayList<Grade>();
                try {
                    gradeArray = gradeHandler.getRecentGrades();
                    gradesdb.addGrades(gradeArray);
                    Collections.reverse(Arrays.asList(gradeArray));

                    //For testing purposes:
                    /*Grade sampleGrade = new Grade();
                    sampleGrade.isSufficient = false;
                    sampleGrade.grade = "2.3";
                    sampleGrade.subject = new SubSubject();
                    sampleGrade.subject.name = "Latijn";

                    Grade sampleGrade2 = new Grade();
                    sampleGrade2.isSufficient = true;
                    sampleGrade2.grade = "6.5";
                    sampleGrade2.subject = new SubSubject();
                    sampleGrade2.subject.name = "Nederlands";

                    gradeArray = new Grade[2];
                    gradeArray[0] = sampleGrade;
                    gradeArray[1] = sampleGrade2;*/

                    for (Grade grade : gradeArray) {
                        if (!gradesdb.hasBeenSeen(grade, false)
                                && (grade.isSufficient || !configUtil.getBoolean("pass_grades_only"))) {
                            gradeList.add(grade);
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                String GradesNotification = new Gson().toJson(gradeList);
                if (gradeList != null && gradeList.size() > 0
                        && !configUtil.getString("lastGradesNotification").equals(GradesNotification)) {

                    Log.d(TAG, "run: Some grades to show: " + gradeList.size());

                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext());
                    mBuilder.setSmallIcon(R.drawable.ic_grade_notification);

                    if (gradeList.size() == 1) {
                        Grade grade = gradeList.get(0);
                        mBuilder.setContentTitle("Nieuw cijfer voor " + grade.subject.name);
                        //mBuilder.setStyle(new NotificationCompat.BigTextStyle(mBuilder).bigText())
                        mBuilder.setContentText("Een " + grade.grade);
                    } else {
                        String content = "";
                        for (Grade grade : gradeList) {
                            String string = grade.subject.name + ", een " + grade.grade;
                            if (content.length() > 1) {
                                content = content + "\n" + string;
                            } else {
                                content = string;
                            }
                        }
                        mBuilder.setContentTitle("Nieuwe cijfers voor:");
                        mBuilder.setStyle(new NotificationCompat.BigTextStyle(mBuilder).bigText(content));
                        mBuilder.setContentText(content);
                    }
                    mBuilder.setAutoCancel(true);
                    mBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                    mBuilder.setDefaults(Notification.DEFAULT_ALL);

                    Intent resultIntent = new Intent(getApplicationContext(), NewGradeActivity.class);
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
                    stackBuilder.addParentStack(NewGradeActivity.class);
                    stackBuilder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                    mBuilder.setContentIntent(resultPendingIntent);


                    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(9992, mBuilder.build());

                    configUtil.setString("lastGradesNotification", GradesNotification);
                } else {
                    Log.w(TAG, "run: No grades!");
                }
            }
        };
        timer.schedule(gradeStack, 6000, 10 * 1000);
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
