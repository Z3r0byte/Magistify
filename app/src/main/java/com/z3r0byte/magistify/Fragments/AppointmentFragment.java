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
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.z3r0byte.magistify.Adapters.AppointmentsAdapter;
import com.z3r0byte.magistify.AppointmentDetailsActivity;
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

import java.io.IOException;
import java.io.InterruptedIOException;
import java.text.ParseException;
import java.util.Date;

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
    TextView currentDay;
    ImageView nextDay;
    ImageView previousDay;
    AppointmentsAdapter mAppointmentsAdapter;

    Appointment[] Appointments;
    Date selectedDate;

    Thread refreshThread;
    ConfigUtil configUtil;

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
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getActivity(), AppointmentDetailsActivity.class);
                intent.putExtra("Appointment", new Gson().toJson(Appointments[i]));
                startActivity(intent);
            }
        });

        currentDay = (TextView) view.findViewById(R.id.current_day);
        nextDay = (ImageView) view.findViewById(R.id.nextDay);
        previousDay = (ImageView) view.findViewById(R.id.previousDay);

        IconicsDrawable previousDayIcon = new IconicsDrawable(getActivity(), GoogleMaterial.Icon.gmd_navigate_before)
                .color(getResources().getColor(R.color.md_white_1000))
                .sizeDp(25);
        IconicsDrawable nextDayIcon = new IconicsDrawable(getActivity(), GoogleMaterial.Icon.gmd_navigate_next)
                .color(getResources().getColor(R.color.md_white_1000))
                .sizeDp(25);
        nextDay.setImageDrawable(nextDayIcon);
        previousDay.setImageDrawable(previousDayIcon);

        nextDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedDate = DateUtils.addDays(selectedDate, 1);
                refresh();
            }
        });
        previousDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedDate = DateUtils.addDays(selectedDate, -1);
                refresh();
            }
        });

        selectedDate = DateUtils.getToday();
        configUtil = new ConfigUtil(getActivity());

        refresh();

        SharedListener.finishInitiator = new FinishInitiator();
        FinishResponder responder = new FinishResponder(this);
        SharedListener.finishInitiator.addListener(responder);

        return view;
    }

    public void refresh() {
        if (refreshThread != null) {
            refreshThread.interrupt();
        }
        mSwipeRefreshLayout.setRefreshing(true);
        currentDay.setText(DateUtils.formatDate(selectedDate, "EEE dd MMM"));
        refreshThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Magister magister = GlobalAccount.MAGISTER;
                if (magister != null && magister.isExpired()) {
                    try {
                        magister.login();
                        GlobalAccount.MAGISTER = magister;
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
                    User user = new Gson().fromJson(configUtil.getString("User"), User.class);
                    School school = new Gson().fromJson(configUtil.getString("School"), School.class);
                    try {
                        GlobalAccount.MAGISTER = Magister.login(school, user.username, user.password);
                        magister = GlobalAccount.MAGISTER;
                    } catch (IOException | ParseException e) {
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
                }

                if (Thread.interrupted()) {
                    return;
                }

                AppointmentHandler appointmentHandler = new AppointmentHandler(magister);
                try {
                    Appointments = appointmentHandler.getAppointments(selectedDate, selectedDate);
                } catch (InterruptedIOException e) {
                    e.printStackTrace();
                    return;
                } catch (IOException e) {
                    Appointments = null;
                    Log.e(TAG, "run: No connection...", e);
                }

                if (Thread.interrupted()) {
                    return;
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
        });
        refreshThread.start();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_appointments, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_today) {
            selectedDate = new Date();
            refresh();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
