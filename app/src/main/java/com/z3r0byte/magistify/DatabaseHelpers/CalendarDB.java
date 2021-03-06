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
import net.ilexiconn.magister.container.sub.Classroom;
import net.ilexiconn.magister.container.sub.Link;
import net.ilexiconn.magister.container.sub.SubSubject;
import net.ilexiconn.magister.container.sub.Teacher;
import net.ilexiconn.magister.container.type.AppointmentType;
import net.ilexiconn.magister.container.type.InfoType;
import net.ilexiconn.magister.util.DateUtil;

import java.text.ParseException;
import java.util.Date;

import static com.z3r0byte.magistify.Util.DateUtils.addHours;
import static com.z3r0byte.magistify.Util.DateUtils.addMinutes;
import static com.z3r0byte.magistify.Util.DateUtils.formatDate;
import static com.z3r0byte.magistify.Util.DateUtils.getToday;
import static com.z3r0byte.magistify.Util.DateUtils.parseDate;
import static java.lang.Integer.parseInt;


public class CalendarDB extends SQLiteOpenHelper {

    private static final String TAG = "CalendarDB";

    private static final int DATABASE_VERSION = 15;

    private static final String DATABASE_NAME = "calendarDB";
    private static final String TABLE_CALENDAR = "calendar";

    private static final String KEY_ID = "id";
    private static final String KEY_DESC = "description";
    private static final String KEY_CALENDAR_ID = "calendarId";
    private static final String KEY_CLASS_ROOMS = "classrooms";
    private static final String KEY_TEACHER = "teachers";
    private static final String KEY_CONTENT = "content";
    private static final String KEY_START = "start";
    private static final String KEY_END = "end";
    private static final String KEY_FORMATTED_END = "formatend";
    private static final String KEY_FORMATTED_START = "formatstart";
    private static final String KEY_FORMATTED_END_2 = "formatend2";
    private static final String KEY_FORMATTED_START_2 = "formatstart2";
    private static final String KEY_PERIOD_FROM = "periodFrom";
    private static final String KEY_PERIOD_TO = "periodTo";
    private static final String KEY_TAKES_ALL_DAY = "takesAllDay";
    private static final String KEY_LOCATION = "location";
    private static final String KEY_STATE = "state";
    private static final String KEY_FINISHED = "finished";
    private static final String KEY_LINKS = "links";
    private static final String KEY_TYPE = "type";
    private static final String KEY_INFO_TYPE = "infoType";
    private static final String KEY_SUBJECTS = "subjects";
    private static final String KEY_PREV_CONTENT = "previousContent";

    private Context context;

    public CalendarDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CALENDAR_TABLE = "CREATE TABLE IF NOT EXISTS "
                + TABLE_CALENDAR + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_DESC + " TEXT,"
                + KEY_CALENDAR_ID + " INTEGER,"
                + KEY_CLASS_ROOMS + " TEXT,"
                + KEY_CONTENT + " TEXT,"
                + KEY_END + " TEXT,"
                + KEY_FINISHED + " BOOLEAN,"
                + KEY_FORMATTED_END + " INTEGER,"
                + KEY_FORMATTED_START + " INTEGER,"
                + KEY_FORMATTED_END_2 + " INTEGER,"
                + KEY_FORMATTED_START_2 + " INTEGER,"
                + KEY_INFO_TYPE + " TEXT,"
                + KEY_LINKS + " TEXT,"
                + KEY_LOCATION + " TEXT,"
                + KEY_PERIOD_FROM + " INTEGER,"
                + KEY_PERIOD_TO + " INTEGER,"
                + KEY_SUBJECTS + " TEXT,"
                + KEY_START + " TEXT,"
                + KEY_STATE + " TEXT,"
                + KEY_TEACHER + " TEXT,"
                + KEY_TAKES_ALL_DAY + " BOOLEAN,"
                + KEY_TYPE + " TEXT,"
                + KEY_PREV_CONTENT + " TEXT"
                + ")";
        db.execSQL(CREATE_CALENDAR_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade: New Version!");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CALENDAR);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade: New Version!");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CALENDAR);
        onCreate(db);
    }

    public void addItems(Appointment[] appointments) {
        if (appointments.length == 0 || appointments == null) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();

        String day = "";

        for (Appointment item :
                appointments) {
            Integer id = item.id;

            /*
            Fixing the Timezone Bug
             */
            String startDateString;
            try {
                startDateString = DateUtil.dateToString(DateUtils.fixTimeZone(item.startDate, context));
            } catch (ParseException e) {
                startDateString = null;
                e.printStackTrace();
            }
            String endDateString;
            String secondEndDateString;
            try {
                //Not adding 2 hours to prevent appointments from being showed one day too long.
                endDateString = DateUtil.dateToString(DateUtils.fixTimeZone(item.endDate, context));
                secondEndDateString = DateUtil.dateToString(DateUtils.fixTimeZone(item.endDate, context));
            } catch (ParseException e) {
                endDateString = null;
                secondEndDateString = null;
                e.printStackTrace();
            }
            /*
             End of the bug fix
             */

            if (!day.equals(startDateString.replaceAll("-", "").substring(0, 8))) {
                deleteAppointmentByDateString(startDateString);
            }
            day = startDateString.replaceAll("-", "").substring(0, 8);

            contentValues.put(KEY_CALENDAR_ID, id);
            contentValues.put(KEY_DESC, item.description);
            contentValues.put(KEY_CLASS_ROOMS, new Gson().toJson(item.classrooms));
            contentValues.put(KEY_CONTENT, item.content);
            contentValues.put(KEY_END, item.endDateString);
            contentValues.put(KEY_FINISHED, item.finished);
            contentValues.put(KEY_FORMATTED_END, endDateString.replaceAll("-", "").substring(0, 8));
            contentValues.put(KEY_FORMATTED_START, startDateString.replaceAll("-", "").substring(0, 8));
            contentValues.put(KEY_FORMATTED_END_2,
                    Integer.parseInt(secondEndDateString.replaceAll("[T:-]", "").substring(4, 12)) + 100000000); //Adding number to fix bug
            contentValues.put(KEY_FORMATTED_START_2,
                    Integer.parseInt(startDateString.replaceAll("[T:-]", "").substring(4, 12)) + 100000000); //Adding number to fix bug
            try {
                contentValues.put(KEY_INFO_TYPE, item.infoType.getID());
            } catch (NullPointerException e) {
                Log.e(TAG, "addItems: No infotype!", e);
                contentValues.put(KEY_INFO_TYPE, 0);
            }
            contentValues.put(KEY_LINKS, new Gson().toJson(item.links));
            contentValues.put(KEY_LOCATION, item.location);
            contentValues.put(KEY_PERIOD_FROM, item.periodFrom);
            contentValues.put(KEY_PERIOD_TO, item.periodUpToAndIncluding);
            contentValues.put(KEY_START, item.startDateString);
            contentValues.put(KEY_STATE, item.classState);
            contentValues.put(KEY_SUBJECTS, new Gson().toJson(item.subjects));
            contentValues.put(KEY_TEACHER, new Gson().toJson(item.teachers));
            contentValues.put(KEY_TAKES_ALL_DAY, item.takesAllDay);
            contentValues.put(KEY_TYPE, item.type.getID());


            db.insert(TABLE_CALENDAR, null, contentValues);

        }

    }

    public void deleteAppointmentByDate(Date date) {
        Integer dateInt = parseInt(formatDate(date, "yyyyMMdd"));
        deleteAppointmentByDateInt(dateInt);
    }

    public void deleteAppointmentByDateString(String date) {
        Integer dateInt = parseInt(date.replaceAll("[T:Z.-]", "").substring(0, 8));
        deleteAppointmentByDateInt(dateInt);
    }

    private void deleteAppointmentByDateInt(Integer date) {
        SQLiteDatabase db = this.getWritableDatabase();
        String Query = "DELETE FROM " + TABLE_CALENDAR + " WHERE " + KEY_FORMATTED_START + " <= " + date + " AND "
                + KEY_FORMATTED_END + " >= " + date;

        db.execSQL(Query);

    }

    public Appointment[] getNotificationAppointments() {
        SQLiteDatabase db = this.getWritableDatabase();
        Date now = getToday();
        Date start = addMinutes(now, 25);
        Date end = addMinutes(now, -15);

        Integer startdateInt = parseInt(formatDate(start, "1MMddHHmm"));
        Integer enddateInt = parseInt(formatDate(end, "1MMddHHmm"));
        String Query = "SELECT * FROM " + TABLE_CALENDAR + " WHERE " + KEY_FORMATTED_START_2 + " <= " + startdateInt + " AND "
                + KEY_FORMATTED_END_2 + " >= " + startdateInt + " AND " + KEY_FORMATTED_START_2 + " >= " + enddateInt;
        Cursor cursor = db.rawQuery(Query, null);

        Appointment[] results = new Appointment[cursor.getCount()];
        int i = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Appointment appointment = new Appointment();
                    appointment.id = cursor.getInt(cursor.getColumnIndex(KEY_CALENDAR_ID));
                    appointment.startDate = DateUtils.parseDate(cursor.getString(cursor.getColumnIndex(KEY_FORMATTED_START_2)), "1MMddHHmm");
                    appointment.description = cursor.getString(cursor.getColumnIndex(KEY_DESC));
                    appointment.type = AppointmentType.getTypeById(cursor.getInt(cursor.getColumnIndex(KEY_TYPE)));
                    appointment.location = cursor.getString(cursor.getColumnIndex(KEY_LOCATION));

                    results[i] = appointment;
                    i++;
                } while (cursor.moveToNext());
            }
        }
        cursor.close();

        return results;
    }

    public Appointment[] getNextAppointments() {
        SQLiteDatabase db = this.getWritableDatabase();
        Date now = getToday();
        Date start = addMinutes(now, 15);
        Date end = parseDate(formatDate(now, "ddMMyyyy"), "ddMMyyyy");
        end = addHours(end, 23);
        end = addMinutes(end, 59);


        Integer startdateInt = parseInt(formatDate(start, "1MMddHHmm"));
        Integer enddateInt = parseInt(formatDate(end, "1MMddHHmm"));
        String Query = "SELECT * FROM " + TABLE_CALENDAR + " WHERE " + KEY_FORMATTED_START_2 + " <= " + enddateInt + " AND "
                + KEY_FORMATTED_END_2 + " >= " + startdateInt;
        Cursor cursor = db.rawQuery(Query, null);

        Appointment[] results = new Appointment[cursor.getCount()];
        int i = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Appointment appointment = new Appointment();
                    appointment.id = cursor.getInt(cursor.getColumnIndex(KEY_CALENDAR_ID));
                    appointment.startDate = DateUtils.parseDate(cursor.getString(cursor.getColumnIndex(KEY_FORMATTED_START_2)), "1MMddHHmm");
                    appointment.description = cursor.getString(cursor.getColumnIndex(KEY_DESC));
                    appointment.type = AppointmentType.getTypeById(cursor.getInt(cursor.getColumnIndex(KEY_TYPE)));
                    appointment.location = cursor.getString(cursor.getColumnIndex(KEY_LOCATION));
                    appointment.periodFrom = cursor.getInt(cursor.getColumnIndex(KEY_PERIOD_FROM));

                    results[i] = appointment;
                    i++;
                } while (cursor.moveToNext());
            }
        }
        cursor.close();

        return results;
    }

    public Appointment[] getSilentAppointments(int margin) {
        SQLiteDatabase db = this.getWritableDatabase();
        Integer startdateInt = parseInt(formatDate(addMinutes(getToday(), margin), "1MMddHHmm"));
        Integer enddateInt = parseInt(formatDate(addMinutes(getToday(), -margin), "1MMddHHmm"));
        String Query = "SELECT * FROM " + TABLE_CALENDAR + " WHERE " + KEY_FORMATTED_START_2 + " <= " + startdateInt + " AND "
                + KEY_FORMATTED_END_2 + " >= " + enddateInt + " AND " + KEY_TAKES_ALL_DAY + " = 0";
        Cursor cursor = db.rawQuery(Query, null);


        Appointment[] results = new Appointment[cursor.getCount()];
        int i = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Appointment appointment = new Appointment();
                    appointment.id = cursor.getInt(cursor.getColumnIndex(KEY_CALENDAR_ID));
                    appointment.type = AppointmentType.getTypeById(cursor.getInt(cursor.getColumnIndex(KEY_TYPE)));

                    results[i] = appointment;
                    i++;
                } while (cursor.moveToNext());
            }
        }
        cursor.close();


        return results;
    }

    public Appointment[] getAppointmentsWithHomework() {
        Date now = DateUtils.getToday();
        SQLiteDatabase db = this.getWritableDatabase();
        Integer dateStart = Integer.parseInt(formatDate(now, "yyyyMMdd"));
        Integer dateEnd = Integer.parseInt(formatDate(DateUtils.addDays(now, 14), "yyyyMMdd"));
        String Query = "SELECT * FROM " + TABLE_CALENDAR + " WHERE " + KEY_FORMATTED_START + " <= " + dateEnd + " AND "
                + KEY_FORMATTED_END + " >= " + dateStart + " AND " + KEY_CONTENT + " IS NOT NULL AND " + KEY_INFO_TYPE + " IS NOT 0";

        Cursor cursor = db.rawQuery(Query, null);
        Appointment[] results = new Appointment[cursor.getCount()];
        int i = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Gson gson = new Gson();
                    Appointment appointment = new Appointment();
                    appointment.id = cursor.getInt(cursor.getColumnIndex(KEY_CALENDAR_ID));
                    appointment.startDate = DateUtils.parseDate(cursor.getString(cursor.getColumnIndex(KEY_START)), "yyyy-MM-dd'T'HH:mm:ss.0000000'Z'");
                    appointment.classrooms = gson.fromJson(cursor.getString(cursor.getColumnIndex(KEY_CLASS_ROOMS)), Classroom[].class);
                    appointment.content = cursor.getString(cursor.getColumnIndex(KEY_CONTENT));
                    appointment.endDate = DateUtils.parseDate(cursor.getString(cursor.getColumnIndex(KEY_END)), "yyyy-MM-dd'T'HH:mm:ss.0000000'Z'");
                    appointment.finished = cursor.getInt(cursor.getColumnIndex(KEY_FINISHED)) > 0;
                    appointment.infoType = InfoType.getTypeById(cursor.getInt(cursor.getColumnIndex(KEY_INFO_TYPE)));
                    appointment.links = gson.fromJson(cursor.getString(cursor.getColumnIndex(KEY_LINKS)), Link[].class);
                    appointment.location = cursor.getString(cursor.getColumnIndex(KEY_LOCATION));
                    appointment.periodFrom = cursor.getInt(cursor.getColumnIndex(KEY_PERIOD_FROM));
                    appointment.periodUpToAndIncluding = cursor.getInt(cursor.getColumnIndex(KEY_PERIOD_TO));
                    appointment.description = cursor.getString(cursor.getColumnIndex(KEY_DESC));
                    appointment.classState = cursor.getInt(cursor.getColumnIndex(KEY_STATE));
                    appointment.subjects = gson.fromJson(cursor.getString(cursor.getColumnIndex(KEY_SUBJECTS)), SubSubject[].class);
                    appointment.teachers = gson.fromJson(cursor.getString(cursor.getColumnIndex(KEY_TEACHER)), Teacher[].class);
                    appointment.type = AppointmentType.getTypeById(cursor.getInt(cursor.getColumnIndex(KEY_TYPE)));

                    results[i] = appointment;
                    i++;
                } while (cursor.moveToNext());
            }
        }
        cursor.close();

        return results;
    }

    public Boolean isModified(Appointment appointment, Boolean updateState) {
        SQLiteDatabase db = this.getWritableDatabase();
        String Query = "SELECT * FROM " + TABLE_CALENDAR + " WHERE " + KEY_CALENDAR_ID + "=" + appointment.id;
        Cursor cursor = db.rawQuery(Query, null);
        String prevContent = "";

        if (cursor.getCount() == 1) {
            if (cursor.moveToFirst()) {
                Log.d(TAG, "getModifiedHomework: content: [" + cursor.getString(cursor.getColumnIndex(KEY_CONTENT)) + "]");
                Log.d(TAG, "getModifiedHomework: prev_content: [" + cursor.getString(cursor.getColumnIndex(KEY_PREV_CONTENT)) + "]");
                prevContent = cursor.getString(cursor.getColumnIndex(KEY_PREV_CONTENT));
            }
        } else {
            return false;
        }
        cursor.close();
        if (updateState) homeworkSeen(appointment, db);

        if (appointment.content != null && prevContent == null) {
            return true;
        } else if (appointment.content == null && prevContent != null) {
            return true;
        } else if (appointment.content == null && prevContent == null) {
            return false;
        } else return !prevContent.equals(appointment.content);
    }

    public void homeworkSeen(Appointment appointment, SQLiteDatabase db) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_PREV_CONTENT, appointment.content);
        db.update(TABLE_CALENDAR, contentValues, KEY_CALENDAR_ID + " = " + appointment.id, null);
        Log.d(TAG, "homeworkSeen: Updating content for " + appointment.description);
    }

    public Appointment[] getUnfinishedAppointments(Date unfinishedBefore) {
        SQLiteDatabase db = this.getWritableDatabase();
        Integer dateStart = Integer.parseInt(formatDate(unfinishedBefore, "yyyyMMdd"));
        Integer dateNow = Integer.parseInt(formatDate(new Date(), "yyyyMMdd"));
        String Query = "SELECT * FROM " + TABLE_CALENDAR + " WHERE " + KEY_FORMATTED_START + " <= " + dateStart + " AND "
                + KEY_FORMATTED_START + " > " + dateNow + " AND " + KEY_CONTENT + " IS NOT NULL AND " + KEY_INFO_TYPE + " IS NOT 0 AND " + KEY_FINISHED + " = 0";

        Cursor cursor = db.rawQuery(Query, null);
        Appointment[] results = new Appointment[cursor.getCount()];
        int i = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Gson gson = new Gson();
                    Appointment appointment = new Appointment();
                    appointment.id = cursor.getInt(cursor.getColumnIndex(KEY_CALENDAR_ID));
                    appointment.startDate = DateUtils.parseDate(cursor.getString(cursor.getColumnIndex(KEY_START)), "yyyy-MM-dd'T'HH:mm:ss.0000000'Z'");
                    appointment.classrooms = gson.fromJson(cursor.getString(cursor.getColumnIndex(KEY_CLASS_ROOMS)), Classroom[].class);
                    appointment.content = cursor.getString(cursor.getColumnIndex(KEY_CONTENT));
                    appointment.endDate = DateUtils.parseDate(cursor.getString(cursor.getColumnIndex(KEY_END)), "yyyy-MM-dd'T'HH:mm:ss.0000000'Z'");
                    appointment.finished = cursor.getInt(cursor.getColumnIndex(KEY_FINISHED)) > 0;
                    appointment.infoType = InfoType.getTypeById(cursor.getInt(cursor.getColumnIndex(KEY_INFO_TYPE)));
                    appointment.links = gson.fromJson(cursor.getString(cursor.getColumnIndex(KEY_LINKS)), Link[].class);
                    appointment.location = cursor.getString(cursor.getColumnIndex(KEY_LOCATION));
                    appointment.periodFrom = cursor.getInt(cursor.getColumnIndex(KEY_PERIOD_FROM));
                    appointment.periodUpToAndIncluding = cursor.getInt(cursor.getColumnIndex(KEY_PERIOD_TO));
                    appointment.description = cursor.getString(cursor.getColumnIndex(KEY_DESC));
                    appointment.classState = cursor.getInt(cursor.getColumnIndex(KEY_STATE));
                    appointment.subjects = gson.fromJson(cursor.getString(cursor.getColumnIndex(KEY_SUBJECTS)), SubSubject[].class);
                    appointment.teachers = gson.fromJson(cursor.getString(cursor.getColumnIndex(KEY_TEACHER)), Teacher[].class);
                    appointment.type = AppointmentType.getTypeById(cursor.getInt(cursor.getColumnIndex(KEY_TYPE)));

                    results[i] = appointment;
                    i++;
                } while (cursor.moveToNext());
            }
        }
        cursor.close();

        return results;
    }

    public void finishAppointment(Appointment appointment) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_FINISHED, appointment.finished);
        db.update(TABLE_CALENDAR, contentValues, KEY_CALENDAR_ID + "=" + appointment.id, null);

    }

    public void deleteAppointment(Appointment appointment) {
        deleteAppointment(appointment.id);
    }

    public void deleteAppointment(int id) {

        SQLiteDatabase db = this.getWritableDatabase();
        String Query = "DELETE FROM " + TABLE_CALENDAR + " WHERE " + KEY_CALENDAR_ID + " = " + id + "";
        db.execSQL(Query);
    }

    public void removeAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CALENDAR, null, null);
    }

    public void removeAll(SQLiteDatabase db) {
        db.delete(TABLE_CALENDAR, null, null);
    }
}
