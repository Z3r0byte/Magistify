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

package com.z3r0byte.magistify;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.gson.Gson;
import com.z3r0byte.magistify.Adapters.ScheduleChangeAdapter;
import com.z3r0byte.magistify.DatabaseHelpers.CalendarDB;
import com.z3r0byte.magistify.DatabaseHelpers.NewGradesDB;
import com.z3r0byte.magistify.DatabaseHelpers.ScheduleChangeDB;
import com.z3r0byte.magistify.GUI.NavigationDrawer;
import com.z3r0byte.magistify.GUI.ScrollRefreshLayout;
import com.z3r0byte.magistify.Util.ConfigUtil;
import com.z3r0byte.magistify.Util.DateUtils;

import net.ilexiconn.magister.Magister;
import net.ilexiconn.magister.container.Appointment;
import net.ilexiconn.magister.container.Grade;
import net.ilexiconn.magister.container.Profile;
import net.ilexiconn.magister.container.School;
import net.ilexiconn.magister.container.User;
import net.ilexiconn.magister.handler.GradeHandler;
import net.ilexiconn.magister.util.HttpUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;


public class DashboardActivity extends AppCompatActivity {
    private static final String TAG = "DashboardActivity";

    Toolbar mToolbar;
    CardView appointmentMain;
    CardView gradeMain;
    CardView scheduleChangeMain;
    ScrollRefreshLayout mSwipeRefreshLayout;
    ConfigUtil configUtil;


    final static String SKU_FIFTY_CENTS = "fifty_cents";
    final static String SKU_ONE_EURO = "one_euro";
    final static String SKU_TWO_EURO = "two_euro";
    final static String SKU_FIVE_EURO = "five_euro";

    IInAppBillingService mService;
    Bundle ownedItems;
    ArrayList<String> boughtSKU = new ArrayList<>();
    ArrayList<String> boughtToken = new ArrayList<>();
    Grade[] Grades;

    ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            mService = IInAppBillingService.Stub.asInterface(service);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mToolbar = (Toolbar) findViewById(R.id.Toolbar);
        mToolbar.setTitle(R.string.title_dashboard);
        setSupportActionBar(mToolbar);

        configUtil = new ConfigUtil(this);

        User user = new Gson().fromJson(configUtil.getString("User"), User.class);
        Profile profile = new Gson().fromJson(configUtil.getString("Profile"), Profile.class);

        NavigationDrawer navigationDrawer = new NavigationDrawer(this, mToolbar,
                profile, user, "Dashboard");
        navigationDrawer.SetupNavigationDrawer();


        mSwipeRefreshLayout = (ScrollRefreshLayout) findViewById(R.id.layout_refresh);
        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.setup_color_3,
                R.color.setup_color_5);
        mSwipeRefreshLayout.setSwipeableChildren(R.id.card_layout);
        mSwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        Log.d(TAG, "onRefresh: Refreshing!");
                        mSwipeRefreshLayout.setVisibility(View.GONE);
                        mSwipeRefreshLayout.setVisibility(View.VISIBLE);
                        setupAppointmentCard();
                        setupChangeCard();
                        retrieveGrades();
                    }
                }
        );

        if (!configUtil.getBoolean("disable_ads")) {
            MobileAds.initialize(getApplicationContext(), getString(R.string.app_ad_id));
            AdView mAdView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder()
                    .addTestDevice("BEF9819F219452AE8661484A2AA03C59")
                    .build();
            mAdView.loadAd(adRequest);
        } else {
            findViewById(R.id.adView).setVisibility(View.GONE);
        }

        if (configUtil.getInteger("failed_auth") >= 2) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage(R.string.dialog_login_failed_desc);
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

        Intent serviceIntent =
                new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);

        appointmentMain = (CardView) findViewById(R.id.card_next_appointment);
        scheduleChangeMain = (CardView) findViewById(R.id.card_schedulechanges);

        gradeMain = (CardView) findViewById(R.id.card_new_grade);
        gradeMain.setVisibility(View.GONE);

        setupAppointmentCard();
        setupChangeCard();
        retrieveGrades();

        getPurchases();
        showUpdateMessage();
    }

    private void showUpdateMessage() {
        final int version = BuildConfig.VERSION_CODE;
        if (configUtil.getInteger("last_update_message") != version && configUtil.getInteger("login_version") != version) {
            android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(this);
            alertDialogBuilder.setTitle(R.string.title_magistify_changelog);
            alertDialogBuilder.setMessage(R.string.desc_magistify_changelog);
            alertDialogBuilder.setPositiveButton("Oké", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    configUtil.setInteger("last_update_message", version);
                }
            });
            android.app.AlertDialog alertDialog = alertDialogBuilder.create();
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
                        } catch (InvalidParameterException e) {
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
                        GlobalAccount.USER = user;
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
        } else {
        }


        appointmentMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), AppointmentActivity.class));
            }
        });

        if (appointment != null) {
            findViewById(R.id.layout_no_appointment).setVisibility(View.GONE);
            TextView period = (TextView) findViewById(R.id.text_list_period);
            TextView lesson = (TextView) findViewById(R.id.text_lesson);
            TextView classroom = (TextView) findViewById(R.id.text_classroom);
            TextView time = (TextView) findViewById(R.id.text_time);


            if (appointment.periodFrom == 0) {
                Log.d(TAG, "nextAppointmentCard: No valid period");
                period.setText("");
                findViewById(R.id.layout_list_calendar_period).setVisibility(View.GONE);
            } else {
                findViewById(R.id.layout_list_calendar_period).setVisibility(View.VISIBLE);
                period.setText(appointment.periodFrom + "");
            }
            lesson.setText(appointment.description);
            classroom.setText(appointment.location);

            if (appointment.startDate != null && appointment.endDate != null) {
                time.setText(DateUtils.formatDate(appointment.startDate, "HH:mm") + " - "
                        + DateUtils.formatDate(appointment.endDate, "HH:mm"));
            } else {
                time.setText("");
            }
        } else {
            findViewById(R.id.layout_appointment).setVisibility(View.GONE);
        }
    }

    private void setupGradeCard() {
        NewGradesDB gradesdb = new NewGradesDB(this);
        Grade[] grades = gradesdb.getNewGrades();
        Grade grade = null;

        if (grades != null && grades.length > 0) {
            if (configUtil.getBoolean("pass_grades_only")) {
                grades = filterGrades(grades);
            }
            if (grades != null && grades.length > 0) {
                Collections.reverse(Arrays.asList(grades));
                grade = grades[0];
            }
        }
        /*
        Grade sampleGrade = new Grade();
        sampleGrade.isSufficient = false;
        sampleGrade.grade = "2.3";
        sampleGrade.subject = new SubSubject();
        sampleGrade.subject.name = "Latijn";
        grade = sampleGrade;*/


        gradeMain.setVisibility(View.VISIBLE);
        gradeMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), NewGradeActivity.class));
            }
        });


        TextView gradeTxt = (TextView) findViewById(R.id.grade);
        TextView subjectTxt = (TextView) findViewById(R.id.subject);
        if (grade != null) {
            if (grade.description != null) {
                subjectTxt.setText(grade.subject.name + " - " + grade.description);
            } else {
                subjectTxt.setText(grade.subject.name);
            }
            gradeTxt.setText(grade.grade);
        } else {
            gradeTxt.setText(R.string.msg_no_grade_short);
            subjectTxt.setText(R.string.msg_no_grade);
        }
        getDaylightSaving();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void retrieveGrades() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Magister magister = GlobalAccount.MAGISTER;
                if (magister != null && magister.isExpired()) {
                    try {
                        magister.login();
                        GlobalAccount.MAGISTER = magister;
                    } catch (IOException e) {
                        Log.e(TAG, "run: No connection during login", e);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mSwipeRefreshLayout.setRefreshing(false);
                                setupGradeCard();
                            }
                        });
                        return;
                    } catch (InvalidParameterException e) {
                        e.printStackTrace();
                        int fails = configUtil.getInteger("failed_auth");
                        fails++;
                        configUtil.setInteger("failed_auth", fails);
                        Log.w(TAG, "run: Amount of failed Authentications: " + fails);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(DashboardActivity.this, R.string.err_failed_login_dashboard, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else if (magister == null) {
                    User user = new Gson().fromJson(configUtil.getString("User"), User.class);
                    School school = new Gson().fromJson(configUtil.getString("School"), School.class);
                    try {
                        GlobalAccount.MAGISTER = Magister.login(school, user.username, user.password);
                        magister = GlobalAccount.MAGISTER;
                    } catch (IOException | ParseException | IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mSwipeRefreshLayout.setRefreshing(false);
                            setupGradeCard();
                        }
                    });
                    if (magister == null) {
                        return;
                    }
                }
                GradeHandler gradeHandler = new GradeHandler(magister);
                //Grade[] Grades;
                try {
                    Grades = gradeHandler.getRecentGrades();
                } catch (IOException e) {
                    Grades = null;
                    Log.e(TAG, "run: No connection...", e);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setupGradeCard();
                        }
                    });
                }
                if (Grades != null && Grades.length != 0) {
                    NewGradesDB db = new NewGradesDB(getApplicationContext());
                    db.addGrades(Grades);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setupGradeCard();
                        }
                    });
                } else if (Grades != null && Grades.length < 1) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setupGradeCard();
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setupGradeCard();
                        }
                    });
                }

            }
        }).start();
    }

    private void setupChangeCard() {
        ScheduleChangeDB db = new ScheduleChangeDB(this);

        Appointment[] rawAppointments = db.getChanges();


        /*rawAppointments = new Appointment[4];

        rawAppointments[0] = new Appointment();
        rawAppointments[0].description = "Roosterwijziging 1";
        rawAppointments[0].startDate = new Date();
        rawAppointments[0].endDate = rawAppointments[0].startDate;
        rawAppointments[0].location = "Lokatie 1";
        rawAppointments[0].periodFrom = 2;

        rawAppointments[1] = new Appointment();
        rawAppointments[1].description = "Roosterwijziging 2";
        rawAppointments[1].startDate = new Date();
        rawAppointments[1].endDate = rawAppointments[1].startDate;
        rawAppointments[1].location = "Lokatie 2";
        rawAppointments[1].periodFrom = 6;

        rawAppointments[2] = new Appointment();
        rawAppointments[2].description = "Roosterwijziging 3";
        rawAppointments[2].startDate = DateUtils.addDays(new Date(),1);
        rawAppointments[2].endDate = rawAppointments[2].startDate;
        rawAppointments[2].location = "Lokatie 3";
        rawAppointments[2].periodFrom = 4;

        rawAppointments[3] = new Appointment();
        rawAppointments[3].description = "Roosterwijziging 4";
        rawAppointments[3].startDate = new Date();
        rawAppointments[3].endDate = rawAppointments[3].startDate;
        rawAppointments[3].location = "Lokatie 4";
        rawAppointments[3].periodFrom = 9;*/



        Appointment[] appointments = null;

        if (rawAppointments != null && rawAppointments.length > 0) {
            if (rawAppointments.length == 1) {
                appointments = new Appointment[1];
                appointments[0] = rawAppointments[0];
            } else if (rawAppointments.length == 2) {
                appointments = new Appointment[2];
                appointments[0] = rawAppointments[0];
                appointments[1] = rawAppointments[1];
            } else {
                appointments = new Appointment[3];
                appointments[0] = rawAppointments[0];
                appointments[1] = rawAppointments[1];
                appointments[2] = rawAppointments[2];
            }
        }


        scheduleChangeMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), ScheduleChangeActivity.class));
            }
        });

        if (appointments != null && appointments.length > 0) {
            findViewById(R.id.layout_no_schedule_changes).setVisibility(View.GONE);

            ListView listView = (ListView) findViewById(R.id.list_scheduleChanges);
            ScheduleChangeAdapter adapter = new ScheduleChangeAdapter(this, appointments);
            RelativeLayout rootView = (RelativeLayout) findViewById(R.id.card_schedulechanges_layout);

            listView.setVisibility(View.VISIBLE);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    startActivity(new Intent(DashboardActivity.this, ScheduleChangeActivity.class));
                }
            });

            final float scale = this.getResources().getDisplayMetrics().density;
            rootView.getLayoutParams().height = (int) (90 * appointments.length * scale + 160f);

        } else {
            findViewById(R.id.layout_schedule_changes).setVisibility(View.GONE);
            findViewById(R.id.layout_no_schedule_changes).setVisibility(View.VISIBLE);
        }
    }

    private void getPurchases() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                    ownedItems = mService.getPurchases(3, getPackageName(), "inapp", null);

                    int response = ownedItems.getInt("RESPONSE_CODE");
                    if (response == 0) {
                        ArrayList<String> ownedSkus =
                                ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
                        ArrayList<String> purchaseDataList =
                                ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
                        ArrayList<String> signatureList =
                                ownedItems.getStringArrayList("INAPP_DATA_SIGNATURE_LIST");

                        for (int i = 0; i < purchaseDataList.size(); ++i) {
                            String purchaseData = purchaseDataList.get(i);
                            String signature = signatureList.get(i);
                            String sku = ownedSkus.get(i);

                            JSONObject jo = new JSONObject(purchaseData);
                            String token = jo.getString("purchaseToken");

                            boughtSKU.add(sku);
                            boughtToken.add(token);

                            Log.i(TAG, "run: Purchased item " + i + ": SKU: " + sku +
                                    ", purchaseData:" + purchaseData + ", Signature: " + signature);

                            configUtil.setBoolean("disable_ads", false);
                            configUtil.setBoolean("pro_unlocked", false);

                            if (boughtSKU.contains(SKU_FIFTY_CENTS)) {
                                configUtil.setBoolean("disable_ads", true);
                                configUtil.setString("token_fifty_cents", token);
                            } else if (boughtSKU.contains(SKU_ONE_EURO)) {
                                configUtil.setBoolean("disable_ads", true);
                                configUtil.setBoolean("pro_unlocked", true);
                                configUtil.setString("token_one_euro", token);
                            } else if (boughtSKU.contains(SKU_TWO_EURO)) {
                                configUtil.setBoolean("disable_ads", true);
                                configUtil.setBoolean("pro_unlocked", true);
                                configUtil.setString("token_two_euro", token);
                            } else if (boughtSKU.contains(SKU_FIVE_EURO)) {
                                configUtil.setBoolean("disable_ads", true);
                                configUtil.setBoolean("pro_unlocked", true);
                                configUtil.setString("token_five_euro", token);
                            }
                        }
                    }

                } catch (RemoteException e) {
                    if (mService != null) {
                        unbindService(mServiceConn);
                    }
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    private void getDaylightSaving() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String response = HttpUtil.convertInputStreamReaderToString(
                            HttpUtil.httpGet("https://api.z3r0byteapps.eu/timezones/daylightsavings/nextchange"));
                    String date = response.substring(0, response.indexOf(","));
                    String offset = response.substring(response.indexOf(",") + 1, response.length());
                    configUtil.setString("nextChange", date);
                    configUtil.setInteger("nextOffset", Integer.parseInt(offset));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    String response = HttpUtil.convertInputStreamReaderToString(
                            HttpUtil.httpGet("https://api.z3r0byteapps.eu/timezones/daylightsavings/currentoffset"));
                    configUtil.setInteger("currentOffset", Integer.parseInt(response));
                } catch (IOException e) {
                    e.printStackTrace();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            unbindService(mServiceConn);
        }
    }
}
