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
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.gson.Gson;
import com.z3r0byte.magistify.GlobalAccount;
import com.z3r0byte.magistify.Util.ConfigUtil;

import net.ilexiconn.magister.Magister;
import net.ilexiconn.magister.container.School;
import net.ilexiconn.magister.container.User;

import java.io.IOException;
import java.text.ParseException;
import java.util.Timer;
import java.util.TimerTask;

public class SessionService extends Service {
    private static final String TAG = "SessionService";

    Timer timer = new Timer();


    public SessionService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ConfigUtil configUtil = new ConfigUtil(getApplicationContext());
        User user = new Gson().fromJson(configUtil.getString("User"), User.class);
        School school = new Gson().fromJson(configUtil.getString("School"), School.class);
        sessionReloader(user, school);
        return START_STICKY;
    }

    private void sessionReloader(final User user, final School school) {
        TimerTask refreshSession = new TimerTask() {
            @Override
            public void run() {
                if (GlobalAccount.MAGISTER == null) {
                    try {
                        Log.d(TAG, "run: initiating session");
                        Magister magister = Magister.login(school, user.username, user.password);
                        GlobalAccount.MAGISTER = magister;
                        GlobalAccount.PROFILE = magister.profile;
                        GlobalAccount.USER = magister.user;
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        GlobalAccount.MAGISTER.login();
                        Log.d(TAG, "run: refreshing session");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        };
        timer.schedule(refreshSession, 5000, 120 * 1000);


    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
