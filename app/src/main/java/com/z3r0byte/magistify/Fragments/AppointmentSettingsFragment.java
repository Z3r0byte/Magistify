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


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.z3r0byte.magistify.R;
import com.z3r0byte.magistify.Services.BackgroundService;
import com.z3r0byte.magistify.Util.ConfigUtil;

/**
 * A simple {@link Fragment} subclass.
 */
public class AppointmentSettingsFragment extends Fragment {
    private static final String TAG = "AppointmentSettings";


    public AppointmentSettingsFragment() {
        // Required empty public constructor
    }

    View view;
    ConfigUtil configUtil;
    Switch enableSwitch;
    Switch ownAppointmentsSwitch;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_appointment_settings, container, false);

        configUtil = new ConfigUtil(getActivity().getApplicationContext());

        enableSwitch = (Switch) view.findViewById(R.id.enable);
        enableSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                configUtil.setBoolean("appointment_enabled", b);
                if (b) {
                    enableAll();
                    getActivity().stopService(new Intent(getActivity().getApplicationContext(), BackgroundService.class));
                    getActivity().startService(new Intent(getActivity().getApplicationContext(), BackgroundService.class));
                    /*
                    if (!ServiceUtil.isServiceRunning(AppointmentService.class, getActivity().getApplicationContext())) {
                        Log.d(TAG, "onCheckedChanged: Starting appointment service");
                        getActivity().startService(new Intent(getActivity().getApplicationContext(), AppointmentService.class));
                    }
                    */
                } else {
                    disableAll();
                    getActivity().stopService(new Intent(getActivity().getApplicationContext(), BackgroundService.class));
                    getActivity().startService(new Intent(getActivity().getApplicationContext(), BackgroundService.class));
                    /*
                    if (ServiceUtil.isServiceRunning(AppointmentService.class, getActivity().getApplicationContext())) {
                        Log.d(TAG, "onCheckedChanged: Stopping appointment service");
                        getActivity().stopService(new Intent(getActivity().getApplicationContext(), AppointmentService.class));
                    }
                    */
                }
            }
        });


        ownAppointmentsSwitch = (Switch) view.findViewById(R.id.own_appointments);
        ownAppointmentsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                configUtil.setBoolean("show_own_appointments", b);
            }
        });


        Boolean enabled = configUtil.getBoolean("appointment_enabled");
        enableSwitch.setChecked(enabled);
        if (enabled) {
            enableAll();
        } else {
            disableAll();
        }
        ownAppointmentsSwitch.setChecked(configUtil.getBoolean("show_own_appointments"));
        return view;
    }

    private void enableAll() {
        ownAppointmentsSwitch.setEnabled(true);
    }

    private void disableAll() {
        ownAppointmentsSwitch.setEnabled(false);
    }

}
