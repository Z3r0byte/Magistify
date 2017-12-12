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

package com.z3r0byte.magistify.Adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.z3r0byte.magistify.R;
import com.z3r0byte.magistify.Util.DateUtils;

import net.ilexiconn.magister.container.Appointment;

/**
 * Created by basva on 12-12-2017.
 */

public class HomeworkAdapter extends ArrayAdapter<Appointment> {
    private final Context context;
    private final Appointment[] appointments;
    String previousDay = "";

    public HomeworkAdapter(Context context, Appointment[] appointments) {
        super(context, -1, appointments);
        this.context = context;
        this.appointments = appointments;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.list_homework, parent, false);

        TextView period = rowView.findViewById(R.id.text_period);
        TextView lesson = rowView.findViewById(R.id.text_lesson);
        TextView homework = rowView.findViewById(R.id.text_homework);
        TextView headerText = rowView.findViewById(R.id.header_text);
        RelativeLayout header = rowView.findViewById(R.id.header);

        String day = DateUtils.formatDate(appointments[position].startDate, "yyyyMMdd");
        if (position != 0) {
            previousDay = DateUtils.formatDate(appointments[position - 1].startDate, "yyyyMMdd");
            if (!previousDay.equals(day)) {
                headerText.setText(DateUtils.formatDate(appointments[position].startDate, "EEEE dd MMM"));
            } else {
                header.setVisibility(View.GONE);
            }
        } else {
            headerText.setText(DateUtils.formatDate(appointments[position].startDate, "EEEE dd MMM"));
        }


        period.setText(appointments[position].periodFrom + "");
        if (appointments[position].periodFrom == 0) {
            period.setText("");
            rowView.findViewById(R.id.layout_period).setBackgroundResource(0);
        }
        lesson.setText(appointments[position].description);
        CharSequence content = Html.fromHtml(appointments[position].content);
        homework.setText(content);


        if (appointments[position].finished && appointments[position].infoType.getID() == 1) {
            lesson.setPaintFlags(lesson.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            homework.setPaintFlags(homework.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }

        //fixing a weird bug
        period.setTextColor(Color.WHITE);
        lesson.setTextColor(Color.BLACK);

        return rowView;
    }
}
