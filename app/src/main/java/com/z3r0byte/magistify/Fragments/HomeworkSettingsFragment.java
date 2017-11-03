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


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.z3r0byte.magistify.DonationActivity;
import com.z3r0byte.magistify.R;
import com.z3r0byte.magistify.Util.ConfigUtil;

/**
 * A simple {@link Fragment} subclass.
 */
public class HomeworkSettingsFragment extends Fragment {


    public HomeworkSettingsFragment() {
        // Required empty public constructor
    }

    Switch newHomework;
    Switch unfinishedHomework;
    RelativeLayout proFunctions;

    ConfigUtil configUtil;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_homework_settings, container, false);

        configUtil = new ConfigUtil(getActivity());

        newHomework = (Switch) view.findViewById(R.id.new_homework);
        unfinishedHomework = (Switch) view.findViewById(R.id.unfinished_homework);
        proFunctions = (RelativeLayout) view.findViewById(R.id.pro_functions_button);

        newHomework.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

            }
        });

        unfinishedHomework.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

            }
        });

        proFunctions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                proFunctionsInfo();
            }
        });

        checkPro();
        return view;
    }

    private void checkPro() {
        Boolean pro = configUtil.getBoolean("pro_unlocked");
        newHomework.setEnabled(pro);
        unfinishedHomework.setEnabled(pro);
    }

    private void proFunctionsInfo() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(getString(R.string.dialog_pro_title));
        alertDialogBuilder.setMessage(getString(R.string.dialog_pro_body));
        alertDialogBuilder.setPositiveButton("Doneren", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(getActivity(), "Let erop dat je minimaal €1,00 kiest. Bedankt!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getActivity(), DonationActivity.class);
                startActivity(intent);
            }
        });
        alertDialogBuilder.setNegativeButton("Nee, dankje", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

}
