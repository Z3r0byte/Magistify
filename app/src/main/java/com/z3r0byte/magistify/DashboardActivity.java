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
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
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
import net.ilexiconn.magister.handler.GradeHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.text.ParseException;
import java.util.ArrayList;

import it.gmariotti.cardslib.library.internal.CardHeader;
import it.gmariotti.cardslib.library.view.CardViewNative;

public class DashboardActivity extends AppCompatActivity {
    private static final String TAG = "DashboardActivity";

    Toolbar mToolbar;
    CardViewNative appointmentMain;
    CardViewNative gradeMain;
    SwipeRefreshLayout mSwipeRefreshLayout;
    ConfigUtil configUtil;


    final static String SKU_FIFTY_CENTS = "fifty_cents";
    final static String SKU_ONE_EURO = "one_euro";
    final static String SKU_TWO_EURO = "two_euro";
    final static String SKU_FIVE_EURO = "five_euro";

    IInAppBillingService mService;
    Bundle ownedItems;
    ArrayList<String> boughtSKU = new ArrayList<>();
    ArrayList<String> boughtToken = new ArrayList<>();

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
                        retrieveGrades();
                    }
                }
        );

        configUtil = new ConfigUtil(this);

        if (!configUtil.getBoolean("disable_ads")) {
            MobileAds.initialize(getApplicationContext(), getString(R.string.app_ad_id));
            AdView mAdView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder()
                    .addTestDevice("BEF9819F219452AE8661484A2AA03C59")
                    .build();
            mAdView.loadAd(adRequest);
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

        appointmentMain = (CardViewNative) findViewById(R.id.card_next_appointment);

        gradeMain = (CardViewNative) findViewById(R.id.card_new_grade);
        gradeMain.setVisibility(View.GONE);

        setupAppointmentCard();
        retrieveGrades();

        getPurchases();
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
        }

        NextAppointmentCard mainCardContent = new NextAppointmentCard(this, appointment);
        CardHeader cardHeader = new CardHeader(this);
        cardHeader.setTitle(getString(R.string.msg_next_appointment));

        mainCardContent.addCardHeader(cardHeader);
        appointmentMain.setCard(mainCardContent);
        appointmentMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), AppointmentActivity.class));
            }
        });
    }

    private void setupGradeCard() {
        NewGradesDB gradesdb = new NewGradesDB(this);
        Grade[] grades = gradesdb.getNewGrades();
        Grade grade = null;

        if (grades != null && grades.length > 0) {
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
        gradeMain.setVisibility(View.VISIBLE);
        gradeMain.setCard(mainCardContent);
        gradeMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), NewGradeActivity.class));
            }
        });

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
                    }
                } else if (magister == null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mSwipeRefreshLayout.setRefreshing(false);
                            setupGradeCard();
                        }
                    });
                    return;
                }
                GradeHandler gradeHandler = new GradeHandler(magister);
                Grade[] Grades;
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

                            // do something with this purchase information
                            // e.g. display the updated list of products owned by user
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
                }
            }
        }).start();
    }
}
