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

import com.z3r0byte.magistify.R;

import net.ilexiconn.magister.container.ScheduleChange;

/**
 * Created by bas on 10-4-17.
 */

public class ScheduleChangeAdapter extends ArrayAdapter<ScheduleChange> {

    private static final String TAG = "ScheduleChangeAdapter";

    private final Context context;
    private final ScheduleChange[] scheduleChanges;

    public ScheduleChangeAdapter(Context context, ScheduleChange[] scheduleChanges) {
        super(context, -1, scheduleChanges);
        this.context = context;
        this.scheduleChanges = scheduleChanges;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.list_lesson, parent, false);


        return rowView;
    }
}
