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
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.google.gson.Gson;
import com.z3r0byte.magistify.Adapters.ScheduleChangeAdapter;
import com.z3r0byte.magistify.DatabaseHelpers.ScheduleChangeDB;
import com.z3r0byte.magistify.GlobalAccount;
import com.z3r0byte.magistify.R;
import com.z3r0byte.magistify.Util.ConfigUtil;
import com.z3r0byte.magistify.Util.DateUtils;
import com.z3r0byte.magistify.Util.ErrorViewConfigs;

import net.ilexiconn.magister.Magister;
import net.ilexiconn.magister.container.Appointment;
import net.ilexiconn.magister.container.School;
import net.ilexiconn.magister.container.User;
import net.ilexiconn.magister.handler.AppointmentHandler;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.text.ParseException;

import tr.xip.errorview.ErrorView;

/**
 * A simple {@link Fragment} subclass.
 */
public class ScheduleChangeFragment extends Fragment {
    private static final String TAG = "ScheduleChangeFragment";

    ScheduleChangeAdapter scheduleChangeAdapter;
    ListView listView;
    ErrorView errorView;
    SwipeRefreshLayout swipeRefreshLayout;
    ConfigUtil configUtil;


    public ScheduleChangeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule_change, container, false);

        listView = (ListView) view.findViewById(R.id.list_scheduleChanges);
        errorView = (ErrorView) view.findViewById(R.id.error_view_schedulechanges);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.layout_refresh);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.setup_color_3,
                R.color.setup_color_5);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getScheduleChanges();
            }
        });

        configUtil = new ConfigUtil(getActivity());
        getScheduleChanges();


        return view;
    }

    private void getScheduleChanges() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (GlobalAccount.MAGISTER != null && GlobalAccount.MAGISTER.isExpired()) {
                    try {
                        GlobalAccount.MAGISTER.login();
                    } catch (IOException e) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                errorView.setVisibility(View.VISIBLE);
                                errorView.setConfig(ErrorViewConfigs.NoConnectionConfig);
                                listView.setVisibility(View.GONE);
                            }
                        });
                        return;
                    }
                } else if (GlobalAccount.MAGISTER == null) {
                    User user = new Gson().fromJson(configUtil.getString("User"), User.class);
                    School school = new Gson().fromJson(configUtil.getString("School"), School.class);
                    try {
                        GlobalAccount.MAGISTER = Magister.login(school, user.username, user.password);
                    } catch (IOException | ParseException | InvalidParameterException | NullPointerException e) {
                        e.printStackTrace();
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                errorView.setVisibility(View.VISIBLE);
                                errorView.setConfig(ErrorViewConfigs.NoConnectionConfig);
                                listView.setVisibility(View.GONE);
                            }
                        });
                        return;
                    }
                }
                AppointmentHandler appointmentHandler = new AppointmentHandler(GlobalAccount.MAGISTER);
                try {
                    Appointment[] changes = appointmentHandler.getScheduleChanges(DateUtils.getToday(),
                            DateUtils.addDays(DateUtils.getToday(), 7));
                    final ScheduleChangeDB scheduleChangeDB = new ScheduleChangeDB(getActivity());
                    scheduleChangeDB.addItems(changes);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Appointment[] changes = scheduleChangeDB.getChanges();
                            if (changes != null && changes.length > 0) {
                                scheduleChangeAdapter = new ScheduleChangeAdapter(getActivity(), changes);
                                listView.setAdapter(scheduleChangeAdapter);
                                listView.setVisibility(View.VISIBLE);
                                errorView.setVisibility(View.GONE);
                            } else {
                                listView.setVisibility(View.GONE);
                                errorView.setVisibility(View.VISIBLE);
                                errorView.setConfig(ErrorViewConfigs.NoScheduleChangesConfig);
                            }
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                } catch (IOException e) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            errorView.setVisibility(View.VISIBLE);
                            errorView.setConfig(ErrorViewConfigs.NoConnectionConfig);
                            listView.setVisibility(View.GONE);
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });

                    e.printStackTrace();
                }
            }
        }).start();
    }

}
