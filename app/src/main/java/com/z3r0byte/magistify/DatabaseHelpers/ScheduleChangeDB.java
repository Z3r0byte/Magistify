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

package com.z3r0byte.magistify.DatabaseHelpers;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.gson.Gson;
import com.z3r0byte.magistify.Util.DateUtils;

import net.ilexiconn.magister.container.Appointment;
import net.ilexiconn.magister.container.type.AppointmentType;
import net.ilexiconn.magister.util.DateUtil;

import java.text.ParseException;
import java.util.Date;

import static com.z3r0byte.magistify.Util.DateUtils.addMinutes;
import static com.z3r0byte.magistify.Util.DateUtils.formatDate;
import static com.z3r0byte.magistify.Util.DateUtils.getToday;
import static java.lang.Integer.parseInt;

public class ScheduleChangeDB extends SQLiteOpenHelper {
    private static final String TAG = "ScheduleChangeDB";

    private static final int DATABASE_VERSION = 14;

    private static final String DATABASE_NAME = "scheduleChangesDB";
    private static final String TABLE_NAME = "changes";

    private static final String KEY_ID = "id";
    private static final String KEY_DESC = "description";
    private static final String KEY_CALENDAR_ID = "calendarId";
    private static final String KEY_CLASS_ROOMS = "classrooms";
    private static final String KEY_START = "start";
    private static final String KEY_END = "end";
    private static final String KEY_SORTABLE_DATE = "sortableDate";
    private static final String KEY_PERIOD_FROM = "periodFrom";
    private static final String KEY_PERIOD_TO = "periodTo";
    private static final String KEY_LOCATION = "location";
    private static final String KEY_TYPE = "type";
    private static final String KEY_SUBJECTS = "subjects";


    private Context context;

    public ScheduleChangeDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CALENDAR_TABLE = "CREATE TABLE IF NOT EXISTS "
                + TABLE_NAME + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_DESC + " TEXT,"
                + KEY_CALENDAR_ID + " INTEGER,"
                + KEY_CLASS_ROOMS + " TEXT,"
                + KEY_END + " TEXT,"
                + KEY_SORTABLE_DATE + " INTEGER,"
                + KEY_LOCATION + " TEXT,"
                + KEY_PERIOD_FROM + " INTEGER,"
                + KEY_PERIOD_TO + " INTEGER,"
                + KEY_SUBJECTS + " TEXT,"
                + KEY_START + " TEXT,"
                + KEY_TYPE + " TEXT"
                + ")";
        db.execSQL(CREATE_CALENDAR_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade: New Version!");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void addItems(Appointment[] appointments) {
        if (appointments == null || appointments.length == 0) {
            return;
        } else {
            removeAll();
        }
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();


        for (Appointment item :
                appointments) {
            Integer id = item.id;

            contentValues.put(KEY_CALENDAR_ID, id);
            contentValues.put(KEY_DESC, item.description);
            contentValues.put(KEY_CLASS_ROOMS, new Gson().toJson(item.classrooms));
            contentValues.put(KEY_END, item.endDateString);
            contentValues.put(KEY_SORTABLE_DATE, DateUtils.formatDate(item.startDate, "YYYYMMddHHmm"));
            contentValues.put(KEY_LOCATION, item.location);
            contentValues.put(KEY_PERIOD_FROM, item.periodFrom);
            contentValues.put(KEY_PERIOD_TO, item.periodUpToAndIncluding);
            contentValues.put(KEY_START, item.startDateString);
            contentValues.put(KEY_SUBJECTS, new Gson().toJson(item.subjects));
            contentValues.put(KEY_TYPE, item.type.getID());


            db.insert(TABLE_NAME, null, contentValues);
        }

    }

    public Appointment[] getChanges() {
        SQLiteDatabase db = this.getWritableDatabase();

        String today = DateUtils.formatDate(DateUtils.getToday(), "yyyyMMddHHmm");
        String query = "SELECT * FROM " + TABLE_NAME + " WHERE " + KEY_SORTABLE_DATE + ">"
                + today + " ORDER BY " + KEY_SORTABLE_DATE + " ASC";

        Cursor cursor = db.rawQuery(query, null);


        Appointment[] results = new Appointment[cursor.getCount()];
        int i = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Appointment appointment = new Appointment();
                    appointment.id = cursor.getInt(cursor.getColumnIndex(KEY_CALENDAR_ID));
                    appointment.type = AppointmentType.getTypeById(cursor.getInt(cursor.getColumnIndex(KEY_TYPE)));
                    appointment.description = cursor.getString(cursor.getColumnIndex(KEY_DESC));
                    appointment.periodFrom = cursor.getInt(cursor.getColumnIndex(KEY_PERIOD_FROM));
                    appointment.periodUpToAndIncluding = cursor.getInt(cursor.getColumnIndex(KEY_PERIOD_TO));
                    appointment.startDateString = cursor.getString(cursor.getColumnIndex(KEY_START));
                    appointment.endDateString = cursor.getString(cursor.getColumnIndex(KEY_END));
                    try {
                        appointment.startDate = DateUtil.stringToDate(appointment.startDateString);
                        appointment.endDate = DateUtil.stringToDate(appointment.endDateString);
                    } catch (ParseException e) {
                        appointment.startDate = DateUtils.getToday();
                        appointment.endDate = DateUtils.getToday();
                    }
                    appointment.location = cursor.getString(cursor.getColumnIndex(KEY_LOCATION));


                    results[i] = appointment;
                    i++;
                } while (cursor.moveToNext());
            }
        }
        cursor.close();
        db.close();

        return results;
    }

    public Boolean isInDatabase(Appointment appointment){
        SQLiteDatabase db = this.getWritableDatabase();

        String query = "SELECT * FROM " + TABLE_NAME + " WHERE "
                + KEY_START + " = '" + appointment.startDateString +
                "' AND " + KEY_END + " = '" + appointment.endDateString + "'";

        Cursor cursor = db.rawQuery(query, null);
        if (cursor.getCount() > 0){
            cursor.close();
            return true;
        } else {
            cursor.close();
            return false;
        }
    }

    public Appointment[] getNotificationAppointments() {
        SQLiteDatabase db = this.getWritableDatabase();
        Date now = getToday();
        Date start = addMinutes(now, 25);
        Date end = addMinutes(now, -15);

        Integer startdateInt = parseInt(formatDate(start, "yyyyMMddHHmm"));
        Integer enddateInt = parseInt(formatDate(end, "yyyyMMddHHmm"));
        String Query = "SELECT * FROM " + TABLE_NAME + " WHERE " + KEY_SORTABLE_DATE + " <= " + startdateInt + " AND "
                + KEY_SORTABLE_DATE + " >= " + enddateInt;
        Log.d(TAG, "getNotificationAppointments: Query: " + Query);
        Cursor cursor = db.rawQuery(Query, null);

        Appointment[] results = new Appointment[cursor.getCount()];
        int i = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Appointment appointment = new Appointment();
                    appointment.id = cursor.getInt(cursor.getColumnIndex(KEY_CALENDAR_ID));
                    appointment.type = AppointmentType.getTypeById(cursor.getInt(cursor.getColumnIndex(KEY_TYPE)));
                    appointment.description = cursor.getString(cursor.getColumnIndex(KEY_DESC));
                    appointment.periodFrom = cursor.getInt(cursor.getColumnIndex(KEY_PERIOD_FROM));
                    appointment.periodUpToAndIncluding = cursor.getInt(cursor.getColumnIndex(KEY_PERIOD_TO));
                    appointment.startDateString = cursor.getString(cursor.getColumnIndex(KEY_START));
                    appointment.endDateString = cursor.getString(cursor.getColumnIndex(KEY_END));
                    try {
                        appointment.startDate = DateUtil.stringToDate(appointment.startDateString);
                        appointment.endDate = DateUtil.stringToDate(appointment.endDateString);
                    } catch (ParseException e) {
                        appointment.startDate = DateUtils.getToday();
                        appointment.endDate = DateUtils.getToday();
                    }
                    appointment.location = cursor.getString(cursor.getColumnIndex(KEY_LOCATION));

                    results[i] = appointment;
                    i++;
                } while (cursor.moveToNext());
            }
        }
        cursor.close();

        return results;
    }


    private void removeAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, null, null);
    }
}
