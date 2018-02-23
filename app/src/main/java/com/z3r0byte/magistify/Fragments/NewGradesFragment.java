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
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.gson.Gson;
import com.z3r0byte.magistify.Adapters.NewGradesAdapter;
import com.z3r0byte.magistify.DatabaseHelpers.NewGradesDB;
import com.z3r0byte.magistify.GlobalAccount;
import com.z3r0byte.magistify.GradeDetailsActivity;
import com.z3r0byte.magistify.R;
import com.z3r0byte.magistify.Util.ConfigUtil;
import com.z3r0byte.magistify.Util.ErrorViewConfigs;

import net.ilexiconn.magister.Magister;
import net.ilexiconn.magister.container.Grade;
import net.ilexiconn.magister.container.School;
import net.ilexiconn.magister.container.User;
import net.ilexiconn.magister.handler.GradeHandler;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import tr.xip.errorview.ErrorView;

public class NewGradesFragment extends Fragment {
    private static final String TAG = "NewGradesFragment";

    View view;
    SwipeRefreshLayout mSwipeRefreshLayout;
    ErrorView errorView;
    ListView listView;
    NewGradesAdapter mNewGradesAdapter;
    ConfigUtil configUtil;

    Grade[] Grades;


    public NewGradesFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_new_grades, container, false);
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
        errorView = (ErrorView) view.findViewById(R.id.error_view_new_grades);

        Grades = new Grade[0];

        listView = (ListView) view.findViewById(R.id.list_new_grades);
        mNewGradesAdapter = new NewGradesAdapter(getActivity(), Grades);
        listView.setAdapter(mNewGradesAdapter);

        configUtil = new ConfigUtil(getActivity());

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
                                mSwipeRefreshLayout.setRefreshing(false);
                            }
                        });
                        return;
                    }
                }
                GradeHandler gradeHandler = new GradeHandler(magister);
                try {
                    Grades = gradeHandler.getRecentGrades();
                    Collections.reverse(Arrays.asList(Grades));

                    if (configUtil.getBoolean("pass_grades_only")) {
                        Grades = filterGrades(Grades);
                    }
                } catch (IOException e) {
                    Grades = null;
                    Log.e(TAG, "run: No connection...", e);
                }
                if (Grades != null && Grades.length != 0) {
                    NewGradesDB db = new NewGradesDB(getActivity());
                    db.addGrades(Grades);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mNewGradesAdapter = new NewGradesAdapter(getActivity(), Grades);
                            listView.setAdapter(mNewGradesAdapter);
                            listView.setVisibility(View.VISIBLE);
                            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                    Intent intent = new Intent(getActivity(), GradeDetailsActivity.class);
                                    intent.putExtra("Grade", new Gson().toJson(Grades[i]));
                                    startActivity(intent);
                                }
                            });
                            errorView.setVisibility(View.GONE);
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    });
                } else if (Grades != null && Grades.length < 1) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listView.setVisibility(View.GONE);
                            errorView.setVisibility(View.VISIBLE);
                            errorView.setConfig(ErrorViewConfigs.NoNewGradesConfig);
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


    private Grade[] filterGrades(Grade[] grades) {
        ArrayList<Grade> filtered = new ArrayList<>();
        for (Grade grade : grades) {
            if (grade.isSufficient) {
                filtered.add(grade);
            }
        }

        Grade[] filteredArray = new Grade[filtered.size()];
        filteredArray = filtered.toArray(filteredArray);

        return filteredArray;
    }

}
