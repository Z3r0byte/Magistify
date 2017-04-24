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

package com.z3r0byte.magistify.Fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.z3r0byte.magistify.GlobalAccount;
import com.z3r0byte.magistify.R;
import com.z3r0byte.magistify.Util.DateUtils;

import net.ilexiconn.magister.container.Appointment;
import net.ilexiconn.magister.handler.AppointmentHandler;

import java.io.IOException;

/**
 * A simple {@link Fragment} subclass.
 */
public class SheduleChangeFragment extends Fragment {
    private static final String TAG = "SheduleChangeFragment";


    public SheduleChangeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule_change, container, false);

        getScheduleChanges();


        return view;
    }

    private void getScheduleChanges() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (GlobalAccount.MAGISTER == null || GlobalAccount.MAGISTER.isExpired()) {
                    return;
                }
                AppointmentHandler appointmentHandler = new AppointmentHandler(GlobalAccount.MAGISTER);
                try {
                    Appointment[] changes = appointmentHandler.getScheduleChanges(DateUtils.getToday(),
                            DateUtils.addDays(DateUtils.getToday(), 3));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
