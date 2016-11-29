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

package com.z3r0byte.magistify;

import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.z3r0byte.magistify.GUI.NavigationDrawer;
import com.z3r0byte.magistify.Util.ConfigUtil;

import java.util.ArrayList;
import java.util.List;

public class AutoSilentActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private static final String TAG = "AutoSilentActivity";

    Toolbar mToolbar;
    Spinner marginSpinner;
    Switch enableSwitch;
    Switch ownAppointmentSwitch;
    TextView margin_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_silent);

        ConfigUtil configutil = new ConfigUtil(this);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.title_auto_silent);
        setSupportActionBar(mToolbar);

        NavigationDrawer navigationDrawer = new NavigationDrawer(this, mToolbar,
                GlobalAccount.PROFILE, GlobalAccount.USER, "Auto-stil");
        navigationDrawer.SetupNavigationDrawer();

        CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        collapsingToolbarLayout.setTitle("Auto-silent");
        collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(android.R.color.transparent));

        enableSwitch = (Switch) findViewById(R.id.enable);
        enableSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                new ConfigUtil(getApplicationContext()).setBoolean("silent_enabled", b);
                if (b) {
                    enableAll();
                } else {
                    disableAll();
                }
            }
        });
        ownAppointmentSwitch = (Switch) findViewById(R.id.own_appointments);
        ownAppointmentSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                new ConfigUtil(getApplicationContext()).setBoolean("silent_own_appointments", b);
            }
        });
        margin_text = (TextView) findViewById(R.id.text1);

        marginSpinner = (Spinner) findViewById(R.id.margin);
        marginSpinner.setOnItemSelectedListener(this);

        List<String> margins = new ArrayList<>();
        margins.add("1 minuut");
        margins.add("2 minuten");
        margins.add("3 minuten");
        margins.add("4 minuten");
        margins.add("5 minuten");

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, margins);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        marginSpinner.setAdapter(dataAdapter);
        marginSpinner.setEnabled(false);
        Integer index = Math.abs(configutil.getInteger("silent_margin") - 1);
        marginSpinner.setSelection(index);

        Boolean enabled = configutil.getBoolean("silent_enabled");
        enableSwitch.setChecked(enabled);
        if (enabled) {
            enableAll();
        } else {
            disableAll();
        }
        ownAppointmentSwitch.setChecked(configutil.getBoolean("silent_own_appointments"));
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        ConfigUtil configUtil = new ConfigUtil(this);
        configUtil.setInteger("silent_margin", position + 1);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    private void enableAll() {
        marginSpinner.setEnabled(true);
        ownAppointmentSwitch.setEnabled(true);
        margin_text.setEnabled(true);
    }

    private void disableAll() {
        marginSpinner.setEnabled(false);
        ownAppointmentSwitch.setEnabled(false);
        margin_text.setEnabled(false);
    }
}
