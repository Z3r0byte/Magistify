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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.z3r0byte.magistify.R;
import com.z3r0byte.magistify.Util.DateUtils;

import net.ilexiconn.magister.container.Appointment;


public class ScheduleChangeAdapter extends ArrayAdapter<Appointment> {

    private static final String TAG = "ScheduleChangeAdapter";

    private final Context context;
    private final Appointment[] scheduleChanges;

    public ScheduleChangeAdapter(Context context, Appointment[] scheduleChanges) {
        super(context, -1, scheduleChanges);
        this.context = context;
        this.scheduleChanges = scheduleChanges;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.list_schedule_change, parent, false);

        RelativeLayout dateLayout = (RelativeLayout) rowView.findViewById(R.id.day_layout);
        TextView dateTextView = (TextView) rowView.findViewById(R.id.date_textview);

        TextView lessonTextView = (TextView) rowView.findViewById(R.id.text_lesson);
        TextView periodTextView = (TextView) rowView.findViewById(R.id.text_list_period);
        TextView classroomTextView = (TextView) rowView.findViewById(R.id.text_classroom);
        TextView timeTextView = (TextView) rowView.findViewById(R.id.text_time);

        if (position == 0) {
            dateLayout.setVisibility(View.VISIBLE);
            dateTextView.setText(DateUtils.formatDate(scheduleChanges[position].startDate, "EEEE dd MMMM"));
        } else {
            if (DateUtils.formatDate(scheduleChanges[position].startDate, "EEEE dd MM").equals(
                    DateUtils.formatDate(scheduleChanges[position - 1].startDate, "EEEE dd MM"))) {
                dateLayout.setVisibility(View.GONE);
            } else {
                dateLayout.setVisibility(View.VISIBLE);
                dateTextView.setText(DateUtils.formatDate(scheduleChanges[position].startDate, "EEEE dd MMMM"));
            }
        }
        if (scheduleChanges[position].description == null) {
            lessonTextView.setText(R.string.msg_cancelled_class);
            lessonTextView.setTextColor(context.getResources().getColor(R.color.md_red_500));
        } else {
            lessonTextView.setText(scheduleChanges[position].description);
        }

        if (scheduleChanges[position].periodFrom == 0) {
            periodTextView.setText("");
            rowView.findViewById(R.id.layout_list_calendar_period).setBackgroundResource(0);
        } else {
            periodTextView.setText(String.valueOf(scheduleChanges[position].periodFrom));
        }

        classroomTextView.setText(scheduleChanges[position].location);
        timeTextView.setText(DateUtils.formatDate(scheduleChanges[position].startDate, "HH:mm")
                + " - " + DateUtils.formatDate(scheduleChanges[position].endDate, "HH:mm"));


        return rowView;
    }
}
