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

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.gson.Gson;
import com.z3r0byte.magistify.DatabaseHelpers.CalendarDB;
import com.z3r0byte.magistify.DatabaseHelpers.NewGradesDB;
import com.z3r0byte.magistify.GUI.NavigationDrawer;
import com.z3r0byte.magistify.GUI.NewGradeCard;
import com.z3r0byte.magistify.GUI.NextAppointmentCard;
import com.z3r0byte.magistify.Util.ConfigUtil;

import net.ilexiconn.magister.Magister;
import net.ilexiconn.magister.container.Appointment;
import net.ilexiconn.magister.container.Grade;
import net.ilexiconn.magister.container.School;
import net.ilexiconn.magister.container.User;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;

import it.gmariotti.cardslib.library.internal.CardHeader;
import it.gmariotti.cardslib.library.view.CardViewNative;

public class DashboardActivity extends AppCompatActivity {
    private static final String TAG = "DashboardActivity";

    Toolbar mToolbar;
    CardViewNative appointmentMain;
    CardViewNative gradeMain;
    SwipeRefreshLayout mSwipeRefreshLayout;
    ConfigUtil configUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);


        mToolbar = (Toolbar) findViewById(R.id.Toolbar);
        mToolbar.setTitle(R.string.title_dashboard);
        setSupportActionBar(mToolbar);

        NavigationDrawer navigationDrawer = new NavigationDrawer(this, mToolbar,
                GlobalAccount.PROFILE, GlobalAccount.USER, "Dashboard");
        navigationDrawer.SetupNavigationDrawer();

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.layout_refresh);
        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.setup_color_3,
                R.color.setup_color_5);
        mSwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        Log.d(TAG, "onRefresh: Refreshing!");
                        mSwipeRefreshLayout.setVisibility(View.GONE);
                        mSwipeRefreshLayout.setVisibility(View.VISIBLE);
                        setupAppointmentCard();
                        setupGradeCard();
                    }
                }
        );

        setupAppointmentCard();
        setupGradeCard();

        configUtil = new ConfigUtil(this);

        if (!configUtil.getBoolean("disable_ads")) {
            MobileAds.initialize(getApplicationContext(), getString(R.string.app_ad_id));
            AdView mAdView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder()
                    .addTestDevice("69A66DEE888B0E3042C80F31AA933CC7")
                    .build();
            mAdView.loadAd(adRequest);
        }

        if (configUtil.getInteger("failed_auth") >= 2) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage(R.string.dialog_login_failed_body);
            alertDialogBuilder.setPositiveButton(R.string.msg_relogin, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    relogin();
                }
            });
            alertDialogBuilder.setNegativeButton(R.string.msg_later, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
    }

    private void relogin() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.fragment_login);
        dialog.setTitle(R.string.msg_relogin);

        Button button = (Button) dialog.findViewById(R.id.button_login);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        EditText usertxt = (EditText) dialog.findViewById(R.id.edit_text_username);
                        EditText passwordtxt = (EditText) dialog.findViewById(R.id.edit_text_password);

                        String username = usertxt.getText().toString();
                        String password = passwordtxt.getText().toString();

                        School school = new Gson().fromJson(configUtil.getString("School"), School.class);
                        try {
                            Magister magister = Magister.login(school, username, password);
                        } catch (final IOException e) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(DashboardActivity.this, R.string.err_no_connection, Toast.LENGTH_SHORT).show();
                                }
                            });
                            return;
                        } catch (ParseException e) {
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(DashboardActivity.this, R.string.err_unknown, Toast.LENGTH_SHORT).show();
                                }
                            });
                            return;
                        } catch (final InvalidParameterException e) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(DashboardActivity.this, R.string.err_wrong_username_or_password, Toast.LENGTH_SHORT).show();
                                }
                            });
                            return;
                        }
                        Log.d(TAG, "onClick: login succeeded!");
                        User user = new User(username, password, false);
                        configUtil.setString("User", new Gson().toJson(user));
                        configUtil.setInteger("failed_auth", 0);
                        dialog.dismiss();
                    }
                }).start();

            }
        });


        dialog.show();
    }

    private void setupAppointmentCard() {
        CalendarDB db = new CalendarDB(this);

        Appointment[] appointments = db.getNextAppointments();
        Appointment appointment = null;

        Log.d(TAG, "setupAppointmentCard: Amount of appointments: " + appointments.length);

        if (appointments != null && appointments.length > 0) {
            appointment = appointments[0];
        }

        NextAppointmentCard mainCardContent = new NextAppointmentCard(this, appointment);
        CardHeader cardHeader = new CardHeader(this);
        cardHeader.setTitle(getString(R.string.msg_next_appointment));

        mainCardContent.addCardHeader(cardHeader);
        appointmentMain = (CardViewNative) findViewById(R.id.card_next_appointment);
        appointmentMain.setCard(mainCardContent);
    }

    private void setupGradeCard() {
        NewGradesDB gradesdb = new NewGradesDB(this);
        Grade[] grades = gradesdb.getNewGrades();
        Grade grade = null;

        if (grades != null && grades.length > 0) {
            Collections.reverse(Arrays.asList(grades));
            Collections.reverse(Arrays.asList(grades));
            grade = grades[0];
        }
        /*
        Grade sampleGrade = new Grade();
        sampleGrade.isSufficient = false;
        sampleGrade.grade = "2.3";
        sampleGrade.subject = new SubSubject();
        sampleGrade.subject.name = "Latijn";
        grade = sampleGrade;*/

        NewGradeCard mainCardContent = new NewGradeCard(this, grade);
        CardHeader cardHeader = new CardHeader(this);
        cardHeader.setTitle(getString(R.string.msg_newest_grade));

        mainCardContent.addCardHeader(cardHeader);
        gradeMain = (CardViewNative) findViewById(R.id.card_new_grade);
        gradeMain.setCard(mainCardContent);

        mSwipeRefreshLayout.setRefreshing(false);
    }
}
