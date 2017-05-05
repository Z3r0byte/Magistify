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
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.z3r0byte.magistify.Adapters.ScheduleChangeAdapter;
import com.z3r0byte.magistify.GlobalAccount;
import com.z3r0byte.magistify.R;
import com.z3r0byte.magistify.Util.DateUtils;
import com.z3r0byte.magistify.Util.ErrorViewConfigs;

import net.ilexiconn.magister.container.Appointment;
import net.ilexiconn.magister.handler.AppointmentHandler;

import java.io.IOException;

import tr.xip.errorview.ErrorView;

/**
 * A simple {@link Fragment} subclass.
 */
public class SheduleChangeFragment extends Fragment {
    private static final String TAG = "SheduleChangeFragment";

    ScheduleChangeAdapter scheduleChangeAdapter;
    ListView listView;
    ErrorView errorView;
    SwipeRefreshLayout swipeRefreshLayout;


    public SheduleChangeFragment() {
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

        getScheduleChanges();


        return view;
    }

    private void getScheduleChanges() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (GlobalAccount.MAGISTER == null || GlobalAccount.MAGISTER.isExpired()) {
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
                AppointmentHandler appointmentHandler = new AppointmentHandler(GlobalAccount.MAGISTER);
                try {
                    final Appointment[] changes = appointmentHandler.getScheduleChanges(DateUtils.getToday(),
                            DateUtils.addDays(DateUtils.getToday(), 3));
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
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
