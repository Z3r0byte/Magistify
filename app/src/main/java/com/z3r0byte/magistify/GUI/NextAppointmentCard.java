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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.z3r0byte.magistify.R;
import com.z3r0byte.magistify.Util.DateUtils;

import net.ilexiconn.magister.container.Appointment;

import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by z3r0byte on 24-12-16.
 */

public class NextAppointmentCard extends Card {
    private static final String TAG = "NextAppointmentCard";

    Context Context;
    Appointment appointment;
    Boolean ready = false;

    public NextAppointmentCard(Context context, Appointment appointment) {
        this(context, appointment, R.layout.card_next_appointment);
    }

    public NextAppointmentCard(Context context, Appointment appointment, int innerLayout) {
        super(context, innerLayout);
        Context = context;
        this.appointment = appointment;
        init();
    }

    private void init() {
    }


    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {
        if (appointment != null) {
            view.findViewById(R.id.layout_no_appointment).setVisibility(View.GONE);
            TextView period = (TextView) view.findViewById(R.id.text_list_period);
            TextView lesson = (TextView) view.findViewById(R.id.text_lesson);
            TextView classroom = (TextView) view.findViewById(R.id.text_classroom);
            TextView time = (TextView) view.findViewById(R.id.text_time);


            if (appointment.periodFrom == 0) {
                Log.d(TAG, "setupInnerViewElements: No valid period");
                period.setText("");
                view.findViewById(R.id.layout_list_calendar_period).setVisibility(View.GONE);
            } else {
                view.findViewById(R.id.layout_list_calendar_period).setVisibility(View.VISIBLE);
                period.setText(appointment.periodFrom + "");
            }
            lesson.setText(appointment.description);
            classroom.setText(appointment.location);

            if (appointment.startDate != null && appointment.endDate != null) {
                //TODO make the time-corrections automatically
                time.setText(DateUtils.formatDate(appointment.startDate, "HH:mm") + " - "
                        + DateUtils.formatDate(appointment.endDate, "HH:mm"));
            } else {
                time.setText("");
            }
        } else {
            view.findViewById(R.id.layout_appointment).setVisibility(View.GONE);
        }
    }
}
