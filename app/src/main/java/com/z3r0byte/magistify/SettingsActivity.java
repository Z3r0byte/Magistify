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

package com.z3r0byte.magistify;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.z3r0byte.magistify.Util.ConfigUtil;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";

    Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mToolbar = (Toolbar) findViewById(R.id.Toolbar);
        mToolbar.setTitle(R.string.title_dashboard);
        mToolbar.setNavigationIcon(R.drawable.back);
        setSupportActionBar(mToolbar);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        final ConfigUtil configUtil = new ConfigUtil(this);

        Switch ads = (Switch) findViewById(R.id.ad_switch);
        ads.setChecked(configUtil.getBoolean("disable_ads"));
        ads.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    configUtil.setBoolean("disable_ads", b);
                } else {
                    configUtil.setBoolean("disable_ads", b);
                }
                Toast.makeText(SettingsActivity.this, R.string.msg_restart_app_to_apply, Toast.LENGTH_LONG).show();
            }
        });
    }
}
