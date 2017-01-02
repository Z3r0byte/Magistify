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

package com.z3r0byte.magistify.Fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.z3r0byte.magistify.Adapters.AppointmentsAdapter;
import com.z3r0byte.magistify.GlobalAccount;
import com.z3r0byte.magistify.R;
import com.z3r0byte.magistify.Util.ErrorViewConfigs;

import net.ilexiconn.magister.Magister;
import net.ilexiconn.magister.container.Appointment;
import net.ilexiconn.magister.handler.AppointmentHandler;

import java.io.IOException;

import tr.xip.errorview.ErrorView;

/**
 * A simple {@link Fragment} subclass.
 */
public class AppointmentFragment extends Fragment {
    private static final String TAG = "AppointmentFragment";


    public AppointmentFragment() {
        // Required empty public constructor
    }

    View view;
    SwipeRefreshLayout mSwipeRefreshLayout;
    ErrorView errorView;
    ListView listView;
    AppointmentsAdapter mAppointmentsAdapter;

    Appointment[] Appointments;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_appointment, container, false);

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.layout_refresh);
        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.setup_color_3,
                R.color.setup_color_5);
        mSwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        Log.d(TAG, "onRefresh: Refreshing!");
                        refresh();
                    }
                }
        );
        errorView = (ErrorView) view.findViewById(R.id.error_view_appointments);

        Appointments = new Appointment[0];

        listView = (ListView) view.findViewById(R.id.list_appointments);
        mAppointmentsAdapter = new AppointmentsAdapter(getActivity(), Appointments);
        listView.setAdapter(mAppointmentsAdapter);

        refresh();

        return view;
    }

    private void refresh() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Magister magister = GlobalAccount.MAGISTER;
                if (magister != null && magister.isExpired()) {
                    try {
                        magister.login();
                    } catch (IOException e) {
                        Log.e(TAG, "run: No connection during login", e);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                listView.setVisibility(View.GONE);
                                errorView.setVisibility(View.VISIBLE);
                                errorView.setConfig(ErrorViewConfigs.NoConnectionConfig);
                                mSwipeRefreshLayout.setRefreshing(false);
                            }
                        });
                        return;
                    }
                } else if (magister == null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listView.setVisibility(View.GONE);
                            errorView.setVisibility(View.VISIBLE);
                            errorView.setConfig(ErrorViewConfigs.NoConnectionConfig);
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    });
                    return;
                }
                AppointmentHandler appointmentHandler = new AppointmentHandler(magister);
                try {
                    Appointments = appointmentHandler.getAppointmentsOfToday();
                } catch (IOException e) {
                    Appointments = null;
                    Log.e(TAG, "run: No connection...", e);
                }
                if (Appointments != null && Appointments.length != 0) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAppointmentsAdapter = new AppointmentsAdapter(getActivity(), Appointments);
                            listView.setAdapter(mAppointmentsAdapter);
                            listView.setVisibility(View.VISIBLE);
                            errorView.setVisibility(View.GONE);
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    });
                } else if (Appointments != null && Appointments.length < 1) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listView.setVisibility(View.GONE);
                            errorView.setVisibility(View.VISIBLE);
                            errorView.setConfig(ErrorViewConfigs.NoLessonConfig);
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    });
                } else {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listView.setVisibility(View.GONE);
                            errorView.setVisibility(View.VISIBLE);
                            errorView.setConfig(ErrorViewConfigs.NoConnectionConfig);
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    });
                }
            }
        }).start();
    }

}
