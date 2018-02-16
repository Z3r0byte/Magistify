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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by bas on 16-11-16.
 */

public class BootListener extends BroadcastReceiver {
    Boolean relogin = false;


    @Override
    public void onReceive(Context context, Intent intent) {
        //starting the watchdog which will automatically start the other services.
        //context.startService(new Intent(context.getApplicationContext(), WatchdogService.class));
        //context.startService(new Intent(context.getApplicationContext(), OldBackgroundService.class));
        BackgroundService backgroundService = new BackgroundService();
        backgroundService.setAlarm(context.getApplicationContext());
    }
}
