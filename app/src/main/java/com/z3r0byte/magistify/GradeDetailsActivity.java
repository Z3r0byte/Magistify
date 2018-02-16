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

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.z3r0byte.magistify.Util.DateUtils;

import net.ilexiconn.magister.container.Grade;

public class GradeDetailsActivity extends AppCompatActivity {
    private static final String TAG = "GradeDetailsActivity";

    Grade mGrade;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grade_details);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mGrade = new Gson().fromJson(extras.getString("Grade"), Grade.class);
        } else {
            Log.e(TAG, "onCreate: Impossible to show details of a null Grade!", new IllegalArgumentException());
            Toast.makeText(GradeDetailsActivity.this, R.string.err_unknown, Toast.LENGTH_SHORT).show();
            finish();
        }

        Toolbar toolbar = findViewById(R.id.Toolbar);
        if (mGrade.description != null) {
            toolbar.setTitle(mGrade.description);
        } else {
            toolbar.setTitle(getString(R.string.msg_details));
        }
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView grade = findViewById(R.id.cijfer);
        TextView gradeCircle = findViewById(R.id.grade);
        TextView test = findViewById(R.id.toets);
        TextView subject = findViewById(R.id.vak);
        TextView dateSubmitted = findViewById(R.id.datum);
        TextView isPassGrade = findViewById(R.id.isVoldoende);
        TextView wage = findViewById(R.id.weging);
        TextView testDate = findViewById(R.id.toetsDatum);

        if (mGrade.grade != null) {
            grade.setText(mGrade.grade);
            gradeCircle.setText(mGrade.grade);
        }
        if (mGrade.description != null) {
            test.setText(mGrade.description);
        }
        if (mGrade.subject.name != null) {
            subject.setText(mGrade.subject.name);
        }
        if (mGrade.filledInDate != null) {
            dateSubmitted.setText(DateUtils.formatDate(mGrade.filledInDate, "dd MMMM yyyy 'om' HH:mm"));
        }
        if (!mGrade.isSufficient) {
            isPassGrade.setText(R.string.msg_no);
            isPassGrade.setTextColor(getResources().getColor(R.color.md_red_500));
            grade.setTextColor(getResources().getColor(R.color.md_red_500));
        } else {
            isPassGrade.setText(R.string.msg_yes);
        }
        if (mGrade.wage != null) {
            wage.setText(mGrade.wage.toString());
        }
        if (mGrade.testDate != null) {
            testDate.setText(DateUtils.formatDate(mGrade.testDate, "dd MMMM yyyy 'om' HH:mm"));
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
