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

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.app.OnNavigationBlockedListener;
import com.heinrichreimersoftware.materialintro.slide.FragmentSlide;
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide;
import com.heinrichreimersoftware.materialintro.slide.Slide;
import com.z3r0byte.magistify.Fragments.LoginFragment;
import com.z3r0byte.magistify.Fragments.SearchSchoolFragment;

public class SetupActivity extends IntroActivity {

    private static final String TAG = "SetupActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSkipEnabled(false);

        addSlide(new SimpleSlide.Builder()
                .title(R.string.setup_title_1)
                .description(R.string.setup_desc_1)
                .image(R.drawable.magistify_512)
                .background(R.color.setup_color_1)
                .backgroundDark(R.color.setup_color_1)
                .build());

        final Slide permissionsSlide;
        permissionsSlide = new SimpleSlide.Builder()
                .title(R.string.setup_title_2)
                .description(R.string.setup_desc_2)
                .background(R.color.setup_color_2)
                .backgroundDark(R.color.setup_color_2)
                .permission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .build();
        addSlide(permissionsSlide);

        final FragmentSlide searchSlide = new FragmentSlide.Builder()
                .background(R.color.setup_color_3)
                .backgroundDark(R.color.setup_color_3)
                .fragment(SearchSchoolFragment.newInstance())
                .build();
        addSlide(searchSlide);

        final FragmentSlide loginSlide = new FragmentSlide.Builder()
                .background(R.color.setup_color_5)
                .backgroundDark(R.color.setup_color_5)
                .fragment(LoginFragment.newInstance())
                .build();
        addSlide(loginSlide);

        addOnNavigationBlockedListener(new OnNavigationBlockedListener() {
            @Override
            public void onNavigationBlocked(int position, int direction) {
                View contentView = findViewById(android.R.id.content);
                Slide slide = getSlide(position);

                if (slide == permissionsSlide) {
                    Snackbar.make(contentView, R.string.snackbar_no_permissions, Snackbar.LENGTH_LONG).show();
                } else if (slide == searchSlide) {
                    Snackbar.make(contentView, R.string.snackbar_no_school_selected, Snackbar.LENGTH_LONG).show();
                } else if (slide == loginSlide) {
                    Snackbar.make(contentView, R.string.snackbar_not_logged_in, Snackbar.LENGTH_LONG).show();
                }
            }
        });

        addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (positionOffset == 0 && position == 4) {
                    Intent intent = new Intent(getApplicationContext(), StartActivity.class);
                    startActivity(intent);
                    finish();
                    overridePendingTransition(0, 0);
                }
            }

            @Override
            public void onPageSelected(int position) {
            }


            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });


    }
}
