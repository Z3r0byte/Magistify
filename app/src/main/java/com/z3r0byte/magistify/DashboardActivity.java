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
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.z3r0byte.magistify.DatabaseHelpers.CalendarDB;
import com.z3r0byte.magistify.DatabaseHelpers.NewGradesDB;
import com.z3r0byte.magistify.GUI.NavigationDrawer;
import com.z3r0byte.magistify.GUI.NewGradeCard;
import com.z3r0byte.magistify.GUI.NextAppointmentCard;
import com.z3r0byte.magistify.Util.ConfigUtil;

import net.ilexiconn.magister.container.Appointment;
import net.ilexiconn.magister.container.Grade;

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

        ConfigUtil configUtil = new ConfigUtil(this);

        if (!configUtil.getBoolean("disable_ads")) {
            MobileAds.initialize(getApplicationContext(), getString(R.string.app_ad_id));
            AdView mAdView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder()
                    .addTestDevice("69A66DEE888B0E3042C80F31AA933CC7")
                    .build();
            mAdView.loadAd(adRequest);
        }
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
