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

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.Html;
import android.util.Log;

import com.google.gson.Gson;
import com.z3r0byte.magistify.AppointmentActivity;
import com.z3r0byte.magistify.DashboardActivity;
import com.z3r0byte.magistify.DatabaseHelpers.CalendarDB;
import com.z3r0byte.magistify.DatabaseHelpers.NewGradesDB;
import com.z3r0byte.magistify.DatabaseHelpers.ScheduleChangeDB;
import com.z3r0byte.magistify.GlobalAccount;
import com.z3r0byte.magistify.HomeworkActivity;
import com.z3r0byte.magistify.NewGradeActivity;
import com.z3r0byte.magistify.R;
import com.z3r0byte.magistify.ScheduleChangeActivity;
import com.z3r0byte.magistify.Util.ConfigUtil;
import com.z3r0byte.magistify.Util.DateUtils;

import net.ilexiconn.magister.Magister;
import net.ilexiconn.magister.container.Appointment;
import net.ilexiconn.magister.container.Grade;
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


public class BackgroundService extends BroadcastReceiver {
    private static final String TAG = "Magistify";

    ConfigUtil configUtil;
    Context context;
    CalendarDB calendarDB;
    PowerManager.WakeLock wakeLock;
    Gson mGson;
    ScheduleChangeDB scheduleChangeDB;
    NewGradesDB gradesdb;

    private static final int LOGIN_FAILED_ID = 9990;
    private static final int APPOINTMENT_NOTIFICATION_ID = 9991;
    private static final int NEW_GRADE_NOTIFICATION_ID = 9992;
    private static final int NEW_SCHEDULE_CHANGE_NOTIFICATION_ID = 9993;
    private static final int NEXT_APPOINTMENT_CHANGED_NOTIFICATION_ID = 9994;
    private static final int NEW_HOMEWORK_NOTIFICATION_ID = 9995;
    private static final int UNFINISHED_HOMEWORK_NOTIFICATION_ID = 9996;



    @Override
    public void onReceive(final Context context, Intent intent) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakeLock.acquire(15 * 1000);

        //Bundle extras = intent.getExtras();

        mGson = new Gson();
        calendarDB = new CalendarDB(context);
        scheduleChangeDB = new ScheduleChangeDB(context);
        gradesdb = new NewGradesDB(context);

        this.context = context;
        configUtil = new ConfigUtil(context);
        final User user = mGson.fromJson(configUtil.getString("User"), User.class);
        final School school = mGson.fromJson(configUtil.getString("School"), School.class);


        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    manageSession(user, school);
                    if (configUtil.getBoolean("silent_enabled") || configUtil.getBoolean("appointment_enabled") || configUtil.getBoolean("new_homework_notification")) {
                        getAppointments();
                    }

                    if (configUtil.getBoolean("appointment_enabled")) {
                        appointmentNotification();
                    }

                    if (configUtil.getBoolean("silent_enabled")) {
                        autoSilent();
                    }

                    if (configUtil.getBoolean("new_grade_enabled")) {
                        newGradeNotification();
                    }

                    if (configUtil.getBoolean("notificationOnNewChanges")) {
                        newScheduleChangeNotification();
                    }

                    if (configUtil.getBoolean("notificationOnChangedLesson")) {
                        nextAppointmentChangedNotification();
                    }

                    if (configUtil.getBoolean("unfinished_homework_notification")) {
                        unFinishedHomeworkNotification();
                    }

                    if (configUtil.getBoolean("new_homework_notification")) {
                        newHomeworkNotification();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    Log.d(TAG, "onReceive: Cleaning up and releasing wakelock!");
                    if (wakeLock.isHeld())
                        wakeLock.release();
                    calendarDB.close();
                    scheduleChangeDB.close();
                    gradesdb.close();
                }
            }
        }).start();
    }


    //Retrieving Data

    private void manageSession(final User user, final School school) {
        if (allowDataTransfer()) {
            if (GlobalAccount.MAGISTER == null) {
                if (configUtil.getInteger("failed_auth") >= 2) {
                    Log.w(TAG, "run: Warning! 2 Failed authentications, aborting for user's safety!");
                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
                    mBuilder.setSmallIcon(R.drawable.ic_error);

                    Intent resultIntent = new Intent(context, DashboardActivity.class);
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                    stackBuilder.addParentStack(DashboardActivity.class);
                    stackBuilder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
                            PendingIntent.FLAG_UPDATE_CURRENT);

                    mBuilder.setContentIntent(resultPendingIntent);
                    mBuilder.setContentTitle(context.getString(R.string.dialog_login_failed_title));
                    mBuilder.setContentText(context.getString(R.string.msg_fix_login));
                    mBuilder.setAutoCancel(true);
                    mBuilder.setSound(null);
                    mBuilder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE);

                    NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(LOGIN_FAILED_ID, mBuilder.build());
                } else {
                    try {
                        Log.d(TAG, "SessionManager: initiating session");
                        GlobalAccount.MAGISTER = Magister.login(school, user.username, user.password);
                        configUtil.setInteger("failed_auth", 0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    } catch (InvalidParameterException e) {
                        int fails = configUtil.getInteger("failed_auth");
                        fails++;
                        configUtil.setInteger("failed_auth", fails);
                        Log.w(TAG, "SessionManager: Amount of failed Authentications: " + fails);
                    }
                }
            } else if (GlobalAccount.MAGISTER.isExpired()) {
                try {
                    GlobalAccount.MAGISTER.login();
                    Log.d(TAG, "SessionManager: refreshing session");
                    configUtil.setInteger("failed_auth", 0);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InvalidParameterException e) {
                    e.printStackTrace();
                    int fails = configUtil.getInteger("failed_auth");
                    fails++;
                    configUtil.setInteger("failed_auth", fails);
                    Log.w(TAG, "SessionManager: Amount of failed Authentications: " + fails);
                }
            } else {
                Log.d(TAG, "manageSession: Session still valid");
            }
        }
    }

    private void getAppointments() {
        if (!allowDataTransfer()) {
            return;
        }
        Magister magister = GlobalAccount.MAGISTER;
        if (magister != null && !magister.isExpired()) {
            Date start = DateUtils.addDays(DateUtils.getToday(), -2);
            Date end = DateUtils.addDays(DateUtils.getToday(), 7);
            AppointmentHandler appointmentHandler = new AppointmentHandler(magister);
            try {
                Appointment[] appointments = appointmentHandler.getAppointments(start, end);
                calendarDB.removeAll();
                calendarDB.addItems(appointments);
                Log.d(TAG, "AppointmentData: New items added");
            } catch (IOException e) {
                Log.w(TAG, "AppointmentData: Failed to get appointments.");
                e.printStackTrace();
            } catch (AssertionError e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "run: Invalid Magister!");
        }
    }


    //Appointment Notification

    private void appointmentNotification() {
        Appointment[] appointments = calendarDB.getNotificationAppointments();
        Log.d(TAG, "AppointmentNotifications: amount of appointments that should be shown: " + appointments.length);
        String previousAppointment = configUtil.getString("previous_appointment");
        if (appointments.length >= 1) {
            Appointment appointment = appointments[0];
            if (!mGson.toJson(appointment).equals(previousAppointment) && isCandidate(appointment)) {
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
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

                Intent resultIntent = new Intent(context, AppointmentActivity.class);
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                stackBuilder.addParentStack(AppointmentActivity.class);
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(resultPendingIntent);

                NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(APPOINTMENT_NOTIFICATION_ID, mBuilder.build());

                previousAppointment = mGson.toJson(appointment);
                configUtil.setString("previous_appointment", previousAppointment);
            }
        } else {
            NotificationManager notifManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notifManager.cancel(APPOINTMENT_NOTIFICATION_ID);
        }
    }

    private boolean isCandidate(Appointment appointment) {
        return configUtil.getBoolean("show_own_appointments") || appointment.type != AppointmentType.PERSONAL;
    }


    //Auto-silent

    private void autoSilent() {
        Appointment[] appointments = calendarDB.getSilentAppointments(getMargin());
        if (doSilent(appointments)) {
            setSilenced(true);
            AudioManager audiomanager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audiomanager == null) return;
            if (!isSilencedByApp())
                configUtil.setInteger("previous_silent_state", audiomanager.getRingerMode());
            if (audiomanager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                audiomanager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            }
        } else {
            if (isSilencedByApp()) {
                AudioManager audiomanager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (audiomanager == null) return;
                if (configUtil.getBoolean("reverse_silent_state")) {
                    audiomanager.setRingerMode(configUtil.getInteger("previous_silent_state"));
                } else {
                    audiomanager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                }
                setSilenced(false);
            }
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

    private void setSilenced(Boolean silenced) {
        configUtil.setBoolean("silenced", silenced);
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


    // New Grade Notification

    private void newGradeNotification() {
        if (!allowDataTransfer()) {
            return;
        }
        Magister magister = GlobalAccount.MAGISTER;
        if (magister == null || magister.isExpired()) {
            Log.e(TAG, "New Grade Notification: Invalid magister");
        } else {


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

            } catch (IOException | AssertionError | NullPointerException e) {
                e.printStackTrace();
                return;
            }
            String GradesNotification = mGson.toJson(gradeList);
            if (gradeList.size() > 0
                    && !configUtil.getString("lastGradesNotification").equals(GradesNotification)) {

                Log.d(TAG, "New Grade Notification: Some grades to show: " + gradeList.size());

                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
                mBuilder.setSmallIcon(R.drawable.ic_grade_notification);

                if (gradeList.size() == 1) {
                    Grade grade = gradeList.get(0);
                    if (grade.description != null) {
                        mBuilder.setContentTitle("Nieuw cijfer voor " + grade.subject.name + " - " + grade.description);
                    } else {
                        mBuilder.setContentTitle("Nieuw cijfer voor " + grade.subject.name);
                    }
                    //mBuilder.setStyle(new NotificationCompat.BigTextStyle(mBuilder).bigText())
                    mBuilder.setContentText("Een " + grade.grade);
                } else {
                    CharSequence content = "";
                    for (Grade grade : gradeList) {
                        CharSequence string;
                        if (grade.description != null) {
                            string = grade.subject.name + " - " + grade.description + ": " + Html.fromHtml("<strong>" + grade.grade + "</strong>");
                        } else {
                            string = grade.subject.name + ", een " + grade.grade;
                        }
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
                mBuilder.setLights(Color.LTGRAY, 300, 200);

                Intent resultIntent = new Intent(context, NewGradeActivity.class);
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                stackBuilder.addParentStack(NewGradeActivity.class);
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(resultPendingIntent);


                NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(NEW_GRADE_NOTIFICATION_ID, mBuilder.build());

                configUtil.setString("lastGradesNotification", GradesNotification);
            } else {
                Log.w(TAG, "New Grade Notification: No grades!");
            }
        }
    }


    // Schedule Changes Notifications

    private void newScheduleChangeNotification() {
        if (allowDataTransfer()) {
            Magister magister = GlobalAccount.MAGISTER;
            if (magister == null || magister.isExpired()) {
                Log.d(TAG, "ScheduleChangeNotification: No valid Magister");
            } else {


                AppointmentHandler appointmentHandler = new AppointmentHandler(magister);
                Appointment[] appointments;
                try {
                    Log.d(TAG, "ScheduleChangeNotification: Requesting schedule changes....");
                    appointments = appointmentHandler.getScheduleChanges(
                            DateUtils.getToday(), DateUtils.addDays(DateUtils.getToday(), 3)
                    );
                } catch (IOException | AssertionError e) {
                    Log.d(TAG, "ScheduleChangeNotification: Error while requesting schedule changes");
                    e.printStackTrace();
                    return;
                }

                Boolean newChanges = false;
                if (appointments == null || appointments.length < 1) {
                    return;
                } else {
                    Log.d(TAG, "ScheduleChangeNotification: Checking for new changes....");
                    for (Appointment appointment :
                            appointments) {
                        if (!scheduleChangeDB.isInDatabase(appointment)) {
                            newChanges = true;
                        }
                    }
                    scheduleChangeDB.addItems(appointments);
                }

                if (newChanges) {
                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
                    mBuilder.setSmallIcon(R.drawable.ic_schedule_change);

                    mBuilder.setContentTitle("Nieuwe roosterijziging(en)!");
                    mBuilder.setContentText("Tik om te bekijken");
                    mBuilder.setAutoCancel(true);
                    mBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                    mBuilder.setDefaults(Notification.DEFAULT_ALL);

                    Intent resultIntent = new Intent(context, ScheduleChangeActivity.class);
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                    stackBuilder.addParentStack(ScheduleChangeActivity.class);
                    stackBuilder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                    mBuilder.setContentIntent(resultPendingIntent);


                    NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(NEW_SCHEDULE_CHANGE_NOTIFICATION_ID, mBuilder.build());
                }
            }


        }
    }

    private void nextAppointmentChangedNotification() {
        Appointment[] appointments = scheduleChangeDB.getNotificationAppointments();
        String previousChangedAppointment = configUtil.getString("previous_changed_appointment");
        if (appointments.length > 0) {
            Appointment appointment = appointments[0];
            if (!appointment.startDateString.equals(previousChangedAppointment)) {
                String content;
                if (appointment.description != null &&
                        !appointment.description.equalsIgnoreCase("null")) {
                    content = appointment.description;
                } else {
                    content = "De les is uitgevallen!";
                }

                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
                mBuilder.setSmallIcon(R.drawable.ic_schedule_change);

                mBuilder.setContentTitle("Let op! De volgende les is gewijzigd!");
                mBuilder.setContentText(content);
                mBuilder.setAutoCancel(true);
                mBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                mBuilder.setDefaults(Notification.DEFAULT_ALL);

                Intent resultIntent = new Intent(context, ScheduleChangeActivity.class);
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                stackBuilder.addParentStack(ScheduleChangeActivity.class);
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(resultPendingIntent);

                NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(NEXT_APPOINTMENT_CHANGED_NOTIFICATION_ID, mBuilder.build());

                configUtil.setString("previous_changed_appointment", appointment.startDateString);
            }
        } else {
            NotificationManager notifManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notifManager.cancel(NEXT_APPOINTMENT_CHANGED_NOTIFICATION_ID);
        }
    }

    //Homework Notifications

    private void newHomeworkNotification() {
        Appointment[] newhomework = calendarDB.getAppointmentsWithHomework();
        Integer currentHomework = configUtil.getInteger("current_amount_of_homework", 999);
        if (currentHomework != 999 && newhomework.length > currentHomework) {
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
            mBuilder.setSmallIcon(R.drawable.ic_new_homework);

            mBuilder.setContentTitle("Nieuw huiswerk");
            mBuilder.setContentText("Tik om je huiswerk te bekijken");
            mBuilder.setAutoCancel(true);
            mBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            mBuilder.setDefaults(Notification.DEFAULT_ALL);

            Intent resultIntent = new Intent(context, HomeworkActivity.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addParentStack(ScheduleChangeActivity.class);
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(resultPendingIntent);

            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(NEW_HOMEWORK_NOTIFICATION_ID, mBuilder.build());
        }
        if (newhomework != null) {
            configUtil.setInteger("current_amount_of_homework", newhomework.length);
        }
    }

    private void unFinishedHomeworkNotification() {
        String lastCheck = configUtil.getString("last_unfinished_homework_check");
        if (lastCheck.equals(""))
            lastCheck = DateUtils.formatDate(DateUtils.addDays(new Date(), -1), "yyyyMMdd");

        Date lastCheckDate = DateUtils.parseDate(lastCheck, "yyyyMMdd");
        Date now = DateUtils.parseDate(DateUtils.formatDate(new Date(), "yyyyMMdd"), "yyyyMMdd");
        Integer hours = configUtil.getInteger("unfinished_homework_hour");
        Integer minutes = configUtil.getInteger("unfinished_homework_minute");
        if (lastCheckDate.before(now)) {
            lastCheckDate = DateUtils.addHours(lastCheckDate, 24 + hours);
            lastCheckDate = DateUtils.addMinutes(lastCheckDate, minutes);
            if (new Date().after(lastCheckDate)) {
                Appointment[] appointments = calendarDB.getUnfinishedAppointments(DateUtils.addDays(now, 1));

                if (appointments.length > 0) {

                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
                    mBuilder.setSmallIcon(R.drawable.ic_unfinished_homework);
                    mBuilder.setContentTitle("Huiswerk waarschuwing");

                    if (appointments.length == 1) {
                        mBuilder.setContentText(appointments[0].description);
                    } else {
                        String content = "Je hebt je huiswerk voor de volgende lessen van morgen nog niet afgerond:";
                        for (Appointment appointment : appointments) {
                            String string = appointment.description;
                            if (content.length() > 1) {
                                content = content + "\n" + string;
                            } else {
                                content = string;
                            }
                        }
                        mBuilder.setStyle(new NotificationCompat.BigTextStyle(mBuilder).bigText(content));
                        mBuilder.setContentText(content);
                    }
                    mBuilder.setAutoCancel(true);
                    mBuilder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE);
                    mBuilder.setDefaults(Notification.DEFAULT_ALL);
                    mBuilder.setLights(Color.RED, 300, 200);

                    Intent resultIntent = new Intent(context, HomeworkActivity.class);
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                    stackBuilder.addParentStack(NewGradeActivity.class);
                    stackBuilder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                    mBuilder.setContentIntent(resultPendingIntent);


                    NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(UNFINISHED_HOMEWORK_NOTIFICATION_ID, mBuilder.build());
                }

                configUtil.setString("last_unfinished_homework_check", DateUtils.formatDate(new Date(), "yyyyMMdd"));
            }
        }
    }


    private Boolean usingWifi() {
        final ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr == null) return false;
        NetworkInfo activeNetwork = connMgr.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
    }

    private Boolean allowDataTransfer() {
        Boolean isWifi = usingWifi();
        Boolean dataAllowed = !configUtil.getBoolean("wifi_only");
        return isWifi || dataAllowed;
    }

    public void setAlarm(Context context) {
        cancelAlarm(context);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, BackgroundService.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60, pendingIntent);
    }

    public void cancelAlarm(Context context) {
        Intent intent = new Intent(context, BackgroundService.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }


}
