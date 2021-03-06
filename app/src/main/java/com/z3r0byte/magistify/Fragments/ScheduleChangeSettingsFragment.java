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

package com.z3r0byte.magistify.Fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.z3r0byte.magistify.R;
import com.z3r0byte.magistify.Util.ConfigUtil;

/**
 * A simple {@link Fragment} subclass.
 */
public class ScheduleChangeSettingsFragment extends Fragment {

    Switch notificationNewChanges;
    Switch notificationChangedLesson;

    ConfigUtil configUtil;

    public ScheduleChangeSettingsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_schedule_change_settings, container, false);

        configUtil = new ConfigUtil(getActivity());

        notificationNewChanges = (Switch) view.findViewById(R.id.notification_new);
        notificationChangedLesson = (Switch) view.findViewById(R.id.notification_before);

        notificationNewChanges.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                configUtil.setBoolean("notificationOnNewChanges", b);
                //getActivity().stopService(new Intent(getActivity(), OldBackgroundService.class));
                //getActivity().startService(new Intent(getActivity(), OldBackgroundService.class));
            }
        });

        notificationChangedLesson.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                configUtil.setBoolean("notificationOnChangedLesson", b);
                //getActivity().stopService(new Intent(getActivity(), OldBackgroundService.class));
                //getActivity().startService(new Intent(getActivity(), OldBackgroundService.class));
            }
        });

        setupSwitches();

        return view;
    }

    private void setupSwitches() {
        notificationNewChanges.setChecked(configUtil.getBoolean("notificationOnNewChanges"));
        notificationChangedLesson.setChecked(configUtil.getBoolean("notificationOnChangedLesson"));
    }

}
