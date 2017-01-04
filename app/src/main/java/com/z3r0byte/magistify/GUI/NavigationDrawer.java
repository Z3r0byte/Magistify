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

package com.z3r0byte.magistify.GUI;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.holder.BadgeStyle;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.z3r0byte.magistify.AppointmentActivity;
import com.z3r0byte.magistify.AutoSilentActivity;
import com.z3r0byte.magistify.Networking.GetRequest;
import com.z3r0byte.magistify.NewGradeActivity;
import com.z3r0byte.magistify.R;
import com.z3r0byte.magistify.SettingsActivity;

import net.ilexiconn.magister.container.Profile;
import net.ilexiconn.magister.container.Status;
import net.ilexiconn.magister.container.User;

import java.io.IOException;

/**
 * Created by bas on 3-11-16.
 */

public class NavigationDrawer {
    private static final String TAG = "NavigationDrawer";

    Drawer drawer;

    AppCompatActivity activity;
    Toolbar toolbar;
    Profile profile;
    User user;
    String selection;

    public NavigationDrawer(AppCompatActivity activity, Toolbar toolbar, Profile profile, User user, String selection) {
        this.activity = activity;
        this.toolbar = toolbar;
        this.profile = profile;
        this.user = user;
        this.selection = selection;
    }


    static PrimaryDrawerItem dashboardItem = new PrimaryDrawerItem().withName(R.string.title_dashboard)
            .withIcon(GoogleMaterial.Icon.gmd_dashboard);
    static PrimaryDrawerItem autoSilentItem = new PrimaryDrawerItem().withName(R.string.title_auto_silent)
            .withIcon(GoogleMaterial.Icon.gmd_do_not_disturb_on);
    static PrimaryDrawerItem appointmentItem = new PrimaryDrawerItem().withName(R.string.title_appointments)
            .withIcon(GoogleMaterial.Icon.gmd_event);
    static PrimaryDrawerItem newGradesItem = new PrimaryDrawerItem().withName(R.string.title_new_grades)
            .withIcon(GoogleMaterial.Icon.gmd_inbox);
    static SecondaryDrawerItem statusItem = new SecondaryDrawerItem().withName(R.string.drawer_status)
            .withIcon(GoogleMaterial.Icon.gmd_dns).withSelectable(false)
            .withBadgeStyle(new BadgeStyle(Color.GRAY, Color.GRAY).withTextColor(Color.WHITE)).withBadge("?").withIdentifier(123);
    static SecondaryDrawerItem aboutItem = new SecondaryDrawerItem().withName(R.string.title_about)
            .withIcon(GoogleMaterial.Icon.gmd_info).withSelectable(false);
    static SecondaryDrawerItem settingsItem = new SecondaryDrawerItem().withName(R.string.title_settings)
            .withIcon(GoogleMaterial.Icon.gmd_settings).withSelectable(false);


    public void SetupNavigationDrawer() {
        getStatus();

        final AccountHeader accountHeader = new AccountHeaderBuilder()
                .withActivity(activity)
                .withHeaderBackground(R.drawable.header_bg)
                .addProfiles(
                        new ProfileDrawerItem().withName(profile.nickname).withEmail(user.username)
                                .withIcon(R.drawable.magistify_512_circle)
                )
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile) {
                        drawer.closeDrawer();
                        return false;
                    }
                })
                .withSelectionListEnabledForSingleProfile(false)
                .build();


        drawer = new DrawerBuilder()
                .withAccountHeader(accountHeader)
                .withActivity(activity)
                .withToolbar(toolbar)
                .addDrawerItems(
                        dashboardItem,
                        autoSilentItem,
                        appointmentItem,
                        newGradesItem,
                        new SectionDrawerItem().withName(R.string.drawer_tools),
                        statusItem,
                        aboutItem,
                        settingsItem
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (drawerItem == dashboardItem && selection != "Dashboard") {
                            closeActivity();
                            drawer.closeDrawer();
                        } else if (drawerItem == autoSilentItem && selection != "Auto-silent") {
                            activity.startActivity(new Intent(activity, AutoSilentActivity.class));
                            closeActivity();
                            drawer.closeDrawer();
                        } else if (drawerItem == newGradesItem && selection != "New-grades") {
                            activity.startActivity(new Intent(activity, NewGradeActivity.class));
                            closeActivity();
                            drawer.closeDrawer();
                        } else if (drawerItem == appointmentItem && selection != "Appointment") {
                            activity.startActivity(new Intent(activity, AppointmentActivity.class));
                            closeActivity();
                            drawer.closeDrawer();
                        } else if (drawerItem == aboutItem) {
                            new LibsBuilder()
                                    .withActivityTitle(activity.getString(R.string.title_about))
                                    .withAboutDescription("Magistify: alle tools voor Magister.<br/><b>Broncode:</b>" +
                                            "<br /><a href=\"https://github.com/z3r0byte/magistify\">https://github.com/z3r0byte/magistify</a><br />" +
                                            "<b>Licentie:</b><br /><a href=\"https://github.com/Z3r0byte/Magistify/blob/master/LICENSE\">Apache 2.0</a>" +
                                            "<br /><br /> Magistify maakt gebruik van de volgende libraries (De magister.java library is aangepast):")
                                    .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                                    .withAboutAppName("Magistify")
                                    .start(activity);
                            drawer.closeDrawer();
                        } else if (drawerItem == settingsItem) {
                            activity.startActivity(new Intent(activity, SettingsActivity.class));
                            drawer.closeDrawer();
                        }
                        return true;
                    }
                })
                .build();

        setSelection(selection);
    }

    private void setSelection(String selection) {
        switch (selection) {
            case "Dashboard":
                drawer.setSelection(dashboardItem);
                break;
            case "Auto-silent":
                drawer.setSelection(autoSilentItem);
                break;
            case "New-grades":
                drawer.setSelection(newGradesItem);
                break;
            case "Appointment":
                drawer.setSelection(appointmentItem);
                break;
            case "":
                drawer.setSelection(-1);
                break;
        }
    }

    private void closeActivity() {
        if (selection != "Dashboard") {
            activity.finish();
        } else {
            drawer.setSelection(dashboardItem);
        }
    }

    private void getStatus() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Status status;
                try {
                    status = Status.getStatusByString(GetRequest.getRequest("https://status.magistify.nl/API/status", null));
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                Log.d(TAG, "run: Status: " + status.getStatus());
                if (status == Status.OK) {
                    statusItem.withBadgeStyle(new BadgeStyle(Color.rgb(0, 153, 0), Color.rgb(0, 153, 0)).withTextColor(Color.WHITE)).withBadge("✔");
                } else if (status == Status.SLOW) {
                    statusItem.withBadgeStyle(new BadgeStyle(Color.rgb(255, 128, 0), Color.rgb(255, 128, 0)).withTextColor(Color.WHITE)).withBadge("~");
                } else if (status == Status.OFFLINE) {
                    statusItem.withBadgeStyle(new BadgeStyle(Color.RED, Color.RED).withTextColor(Color.WHITE)).withBadge("✖");
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        drawer.updateItem(statusItem);
                    }
                });
            }
        }).start();
    }

    private void reportBug() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
        alertDialogBuilder.setTitle(activity.getString(R.string.dialog_bug_title));
        alertDialogBuilder.setMessage(activity.getString(R.string.dialog_bug_desc));
        alertDialogBuilder.setPositiveButton("Oké", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
