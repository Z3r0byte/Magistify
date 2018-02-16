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


public class NewGradesSettingsFragment extends Fragment {

    private static final String TAG = "NewGradesSettingsFragme";


    public NewGradesSettingsFragment() {
        // Required empty public constructor
    }


    View view;
    Switch enableSwitch;
    Switch passGradeSwitch;

    ConfigUtil configUtil;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_new_grades_settings, container, false);

        configUtil = new ConfigUtil(getActivity().getApplicationContext());

        enableSwitch = (Switch) view.findViewById(R.id.enable);
        enableSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                configUtil.setBoolean("new_grade_enabled", b);
                if (b) {
                    enableAll();
                    //getActivity().stopService(new Intent(getActivity().getApplicationContext(), OldBackgroundService.class));
                    //getActivity().startService(new Intent(getActivity().getApplicationContext(), OldBackgroundService.class));
                    /*
                    if (!ServiceUtil.isServiceRunning(NewGradeService.class, getActivity().getApplicationContext())) {
                        Log.d(TAG, "onCheckedChanged: Starting new-grade service");
                        getActivity().startService(new Intent(getActivity().getApplicationContext(), NewGradeService.class));
                    }
                    */
                } else {
                    disableAll();
                    //getActivity().stopService(new Intent(getActivity().getApplicationContext(), OldBackgroundService.class));
                    //getActivity().startService(new Intent(getActivity().getApplicationContext(), OldBackgroundService.class));
                    /*
                    if (ServiceUtil.isServiceRunning(NewGradeService.class, getActivity().getApplicationContext())) {
                        Log.d(TAG, "onCheckedChanged: Stopping new-grade service");
                        getActivity().stopService(new Intent(getActivity().getApplicationContext(), NewGradeService.class));
                    }
                    */
                }
            }
        });

        passGradeSwitch = (Switch) view.findViewById(R.id.pass_grades);
        passGradeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                configUtil.setBoolean("pass_grades_only", b);
            }
        });

        Boolean enabled = configUtil.getBoolean("new_grade_enabled");
        enableSwitch.setChecked(enabled);
        if (enabled) {
            enableAll();
        } else {
            disableAll();
        }
        passGradeSwitch.setChecked(configUtil.getBoolean("pass_grades_only"));
        return view;
    }


    private void enableAll() {
        passGradeSwitch.setEnabled(true);
    }

    private void disableAll() {
        passGradeSwitch.setEnabled(false);
    }

}
