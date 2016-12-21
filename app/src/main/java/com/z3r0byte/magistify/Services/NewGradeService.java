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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.z3r0byte.magistify.DatabaseHelpers.NewGradesDB;
import com.z3r0byte.magistify.GlobalAccount;
import com.z3r0byte.magistify.R;
import com.z3r0byte.magistify.Util.ConfigUtil;

import net.ilexiconn.magister.Magister;
import net.ilexiconn.magister.container.Grade;
import net.ilexiconn.magister.handler.GradeHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class NewGradeService extends Service {
    private static final String TAG = "NewGradeService";

    Timer timer = new Timer();

    public NewGradeService() {
    }

    ConfigUtil configUtil;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: Starting Service...");
        configUtil = new ConfigUtil(this.getApplicationContext());
        GradeChecker();
        return START_STICKY;
    }

    private void GradeChecker() {
        TimerTask refreshSession = new TimerTask() {
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

                    for (Grade grade : gradeArray) {
                        if (!gradesdb.hasBeenNotified(grade)) {
                            gradeList.add(grade);
                            Log.d(TAG, "run: This grade has not been notified before: " + grade.subject);
                        } else {
                            Log.d(TAG, "run: This grade has been notified before: " + grade.subject);
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                if (gradeList != null && gradeList.size() > 0) {

                    Log.d(TAG, "run: Some grades to show: " + gradeList.size());

                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext());
                    mBuilder.setSmallIcon(R.drawable.ic_grade_notification);

                    if (gradeList.size() == 1) {
                        Grade grade = gradeList.get(0);
                        mBuilder.setContentTitle("Nieuw cijfer voor " + grade.subject.name);
                        //mBuilder.setStyle(new NotificationCompat.BigTextStyle(mBuilder).bigText())
                        mBuilder.setContentText(grade.grade);
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


                    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(9992, mBuilder.build());

                } else {
                    Log.w(TAG, "run: No grades!");
                }
            }
        };
        timer.schedule(refreshSession, 6000, 10 * 1000);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
