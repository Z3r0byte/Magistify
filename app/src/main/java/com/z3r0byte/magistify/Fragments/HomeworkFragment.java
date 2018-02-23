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


import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.z3r0byte.magistify.Adapters.HomeworkAdapter;
import com.z3r0byte.magistify.AppointmentDetailsActivity;
import com.z3r0byte.magistify.DatabaseHelpers.CalendarDB;
import com.z3r0byte.magistify.GlobalAccount;
import com.z3r0byte.magistify.Listeners.FinishInitiator;
import com.z3r0byte.magistify.Listeners.FinishResponder;
import com.z3r0byte.magistify.Listeners.SharedListener;
import com.z3r0byte.magistify.R;
import com.z3r0byte.magistify.Util.ConfigUtil;
import com.z3r0byte.magistify.Util.DateUtils;
import com.z3r0byte.magistify.Util.ErrorViewConfigs;

import net.ilexiconn.magister.Magister;
import net.ilexiconn.magister.container.Appointment;
import net.ilexiconn.magister.container.School;
import net.ilexiconn.magister.container.User;
import net.ilexiconn.magister.handler.AppointmentHandler;

import org.json.JSONException;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.text.ParseException;
import java.util.Date;

import tr.xip.errorview.ErrorView;

/**
 * A simple {@link Fragment} subclass.
 */
public class HomeworkFragment extends Fragment {
    private static final String TAG = "HomeworkFragment";

    HomeworkAdapter homeworkAdapter;
    ListView listView;
    SwipeRefreshLayout swipeRefreshLayout;
    Appointment[] homework;
    ErrorView errorView;
    ConfigUtil configUtil;

    public HomeworkFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_homework, container, false);
        listView = view.findViewById(R.id.list_homework);
        swipeRefreshLayout = view.findViewById(R.id.layout_refresh);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.setup_color_3,
                R.color.setup_color_5);
        swipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        Log.d(TAG, "onRefresh: Refreshing!");
                        refresh();
                    }
                }
        );
        errorView = view.findViewById(R.id.error_view_homework);

        homework = new Appointment[0];
        homeworkAdapter = new HomeworkAdapter(getActivity(), homework);
        listView.setAdapter(homeworkAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getActivity(), AppointmentDetailsActivity.class);
                intent.putExtra("Appointment", new Gson().toJson(homework[i]));
                startActivity(intent);
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int i, long l) {
                swipeRefreshLayout.setRefreshing(true);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        Appointment appointment = homework[i];
                        AppointmentHandler appointmentHandler = new AppointmentHandler(GlobalAccount.MAGISTER);
                        try {
                            appointment.finished = !appointment.finished;
                            Boolean finished = appointmentHandler.finishAppointment(appointment);
                            Log.d(TAG, "run: Gelukt: " + finished);
                            if (finished) {
                                SharedListener.finishInitiator.finished();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    swipeRefreshLayout.setRefreshing(false);
                                    Toast.makeText(getActivity(), R.string.err_no_connection, Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getActivity(), R.string.err_unknown, Toast.LENGTH_SHORT).show();
                                    swipeRefreshLayout.setRefreshing(false);
                                }
                            });
                        }
                    }
                }).start();

                return true;
            }
        });


        configUtil = new ConfigUtil(getActivity());

        SharedListener.finishInitiator = new FinishInitiator();
        FinishResponder responder = new FinishResponder(this);
        SharedListener.finishInitiator.addListener(responder);

        swipeRefreshLayout.setRefreshing(true);
        refresh();

        return view;
    }

    public void refresh() {
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
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        });
                        return;
                    }
                } else if (magister == null) {
                    User user = new Gson().fromJson(configUtil.getString("User"), User.class);
                    School school = new Gson().fromJson(configUtil.getString("School"), School.class);
                    try {
                        GlobalAccount.MAGISTER = Magister.login(school, user.username, user.password);
                        magister = GlobalAccount.MAGISTER;
                    } catch (IOException | ParseException | InvalidParameterException | NullPointerException e) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                listView.setVisibility(View.GONE);
                                errorView.setVisibility(View.VISIBLE);
                                errorView.setConfig(ErrorViewConfigs.NoConnectionConfig);
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        });
                        return;
                    }
                }
                AppointmentHandler appointmentHandler = new AppointmentHandler(GlobalAccount.MAGISTER);
                Appointment[] appointments;
                try {
                    Date start = DateUtils.getToday();
                    Date end = DateUtils.addDays(start, 14);
                    appointments = appointmentHandler.getAppointments(start, end);
                } catch (IOException e) {
                    appointments = null;
                    Log.e(TAG, "run: No connection...", e);
                }
                if (appointments != null && appointments.length != 0) {
                    CalendarDB calendarDB = new CalendarDB(getActivity());
                    calendarDB.addItems(appointments);
                    homework = calendarDB.getAppointmentsWithHomework();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            homeworkAdapter = new HomeworkAdapter(getActivity(), homework);
                            listView.setAdapter(homeworkAdapter);
                            listView.setVisibility(View.VISIBLE);
                            errorView.setVisibility(View.GONE);
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                } else if (homework != null && homework.length < 1) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listView.setVisibility(View.GONE);
                            errorView.setVisibility(View.VISIBLE);
                            errorView.setConfig(ErrorViewConfigs.NoHomeworkConfig);
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                } else {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listView.setVisibility(View.GONE);
                            errorView.setVisibility(View.VISIBLE);
                            errorView.setConfig(ErrorViewConfigs.NoConnectionConfig);
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                }
            }
        }).start();
    }

}
