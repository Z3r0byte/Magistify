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

package com.z3r0byte.magistify.Util;

import android.content.Context;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by bas on 14-11-16.
 */

public class DateUtils {
    private static final String TAG = "DateUtils";

    public static String formatDate(Date date, String format) {
        DateFormat dateFormat = new SimpleDateFormat(format);
        return dateFormat.format(date);
    }

    public static Date parseDate(String date, String format) {
        DateFormat dateFormat = new SimpleDateFormat(format);
        try {
            return dateFormat.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        }
        Date error = new Date(2000, 12, 31);
        return error;
    }

    public static Date getToday() {
        Calendar calendar = Calendar.getInstance();
        return new Date(calendar.getTimeInMillis());
    }

    public static Date addDays(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DATE, days);
        return new Date(calendar.getTimeInMillis());
    }

    public static Date addHours(Date date, int hours) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR, hours);
        return new Date(calendar.getTimeInMillis());
    }

    public static Date addMinutes(Date date, int minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, minutes);
        return new Date(calendar.getTimeInMillis());
    }

    public static void lastLogin(Context context, String date) {
        context.getSharedPreferences("data", Context.MODE_PRIVATE).edit().putString("LastLogin", date).apply();
    }

    public static Date fixTimeZone(Date date, Context context) {
        ConfigUtil configUtil = new ConfigUtil(context);
        String nextChange = configUtil.getString("nextChange");
        Integer offset;
        if (parseDate(nextChange, "dd-MM-yyyy").before(date)) {
            //Log.d(TAG, "fixTimeZone: using next offset");
            offset = configUtil.getInteger("nextOffset");
        } else {
            offset = configUtil.getInteger("currentOffset");
        }

        Calendar Time = toCalendar(date);
        Calendar newTime = Calendar.getInstance();
        newTime.setTimeInMillis(Time.getTimeInMillis() + offset * 1000);
        return newTime.getTime();
    }

    public static Calendar toCalendar(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }
}
