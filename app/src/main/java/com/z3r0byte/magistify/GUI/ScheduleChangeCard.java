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
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.z3r0byte.magistify.Adapters.ScheduleChangeAdapter;
import com.z3r0byte.magistify.R;
import com.z3r0byte.magistify.ScheduleChangeActivity;

import net.ilexiconn.magister.container.Appointment;

import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by basva on 12-7-2017.
 */

public class ScheduleChangeCard extends Card {
    private static final String TAG = "ScheduleChangeCard";

    android.content.Context Context;
    Appointment[] appointments;

    public ScheduleChangeCard(Context context, Appointment[] appointments) {
        this(context, appointments, R.layout.card_schedule_changes);
    }

    public ScheduleChangeCard(Context context, Appointment[] appointments, int innerLayout) {
        super(context, innerLayout);
        Context = context;
        this.appointments = appointments;
        init();
    }

    private void init() {
    }


    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {
        if (appointments != null && appointments.length > 0) {
            view.findViewById(R.id.layout_no_appointment).setVisibility(View.GONE);

            ListView listView = (ListView) view.findViewById(R.id.list_scheduleChanges);
            ScheduleChangeAdapter adapter = new ScheduleChangeAdapter(mContext, appointments);
            RelativeLayout rootView = (RelativeLayout) view.findViewById(R.id.scheduleChangesCardLayout);

            listView.setVisibility(View.VISIBLE);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Context.startActivity(new Intent(Context, ScheduleChangeActivity.class));
                }
            });

            final float scale = getContext().getResources().getDisplayMetrics().density;
            if (appointments.length != 0) {
                int pixels = (int) (90 * appointments.length * scale + 0.5f);
                rootView.getLayoutParams().height = pixels;
            }

        } else {
            view.findViewById(R.id.layout_appointment).setVisibility(View.GONE);
            view.findViewById(R.id.layout_no_appointment).setVisibility(View.VISIBLE);
        }
    }
}
