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

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.z3r0byte.magistify.R;

import net.ilexiconn.magister.container.Grade;

import it.gmariotti.cardslib.library.internal.Card;


public class NewGradeCard extends Card {
    private static final String TAG = "NewGradeCard";

    Grade grade;
    Context context;

    public NewGradeCard(Context context, Grade grade) {
        this(context, grade, R.layout.card_new_grade);
    }

    public NewGradeCard(Context context, Grade grade, int innerLayout) {
        super(context, innerLayout);
        this.context = context;
        this.grade = grade;
        init();
    }

    private void init() {
    }


    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {
        if (grade != null) {
            TextView gradeTxt = (TextView) view.findViewById(R.id.grade);
            TextView subjectTxt = (TextView) view.findViewById(R.id.subject);
            gradeTxt.setText(grade.grade);
            subjectTxt.setText(grade.subject.name);
        } else {
            TextView gradeTxt = (TextView) view.findViewById(R.id.grade);
            TextView subjectTxt = (TextView) view.findViewById(R.id.subject);
            gradeTxt.setText(R.string.msg_no_grade_short);
            subjectTxt.setText(R.string.msg_no_grade);
        }
    }
}
