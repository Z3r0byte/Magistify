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

import com.z3r0byte.magistify.Util.DateUtils;

import net.ilexiconn.magister.container.Appointment;
import net.ilexiconn.magister.container.type.InfoType;
import net.ilexiconn.magister.util.DateUtil;

import java.text.ParseException;
import java.util.Date;

import static com.z3r0byte.magistify.Util.DateUtils.formatDate;

public class HomeworkDB extends SQLiteOpenHelper {
    private static final String TAG = "HomeworkDB";

    private final static int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "homeworkDB";
    private static final String TABLE_CALENDAR = "homework";

    private static final String KEY_ID = "id";
    private static final String KEY_DESC = "description";
    private static final String KEY_CALENDAR_ID = "calendarId";
    private static final String KEY_CONTENT = "content";
    private static final String KEY_FORMATTED_END = "formatend";
    private static final String KEY_FORMATTED_START = "formatstart";
    private static final String KEY_PREV_CONTENT = "previousContent";
    private static final String KEY_FINISHED = "finished";
    private static final String KEY_INFO_TYPE = "infoType";

    private Context context;

    public HomeworkDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CALENDAR_TABLE = "CREATE TABLE IF NOT EXISTS "
                + TABLE_CALENDAR + "("
                //+ KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_DESC + " TEXT,"
                + KEY_CALENDAR_ID + " INTEGER PRIMARY KEY,"
                + KEY_CONTENT + " TEXT,"
                + KEY_FINISHED + " BOOLEAN,"
                + KEY_FORMATTED_END + " INTEGER,"
                + KEY_FORMATTED_START + " INTEGER,"
                + KEY_INFO_TYPE + " TEXT,"
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

            day = startDateString.replaceAll("-", "").substring(0, 8);

            String content = getContent(id);
            if (content != null) contentValues.put(KEY_PREV_CONTENT, content);
            contentValues.put(KEY_CALENDAR_ID, id);
            contentValues.put(KEY_DESC, item.description);
            contentValues.put(KEY_CONTENT, item.content);
            contentValues.put(KEY_FINISHED, item.finished);
            contentValues.put(KEY_FORMATTED_END, endDateString.replaceAll("-", "").substring(0, 8));
            contentValues.put(KEY_FORMATTED_START, startDateString.replaceAll("-", "").substring(0, 8));
            try {
                contentValues.put(KEY_INFO_TYPE, item.infoType.getID());
            } catch (NullPointerException e) {
                Log.e(TAG, "addItems: No infotype!", e);
                contentValues.put(KEY_INFO_TYPE, 0);
            }


            db.replace(TABLE_CALENDAR, null, contentValues);

        }

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
                    Appointment appointment = new Appointment();
                    appointment.id = cursor.getInt(cursor.getColumnIndex(KEY_CALENDAR_ID));
                    appointment.startDate = DateUtils.parseDate(cursor.getString(cursor.getColumnIndex(KEY_FORMATTED_START)), "yyyyMMdd");
                    appointment.content = cursor.getString(cursor.getColumnIndex(KEY_CONTENT));
                    appointment.endDate = DateUtils.parseDate(cursor.getString(cursor.getColumnIndex(KEY_FORMATTED_END)), "yyyyMMdd");
                    appointment.finished = cursor.getInt(cursor.getColumnIndex(KEY_FINISHED)) > 0;
                    appointment.infoType = InfoType.getTypeById(cursor.getInt(cursor.getColumnIndex(KEY_INFO_TYPE)));
                    appointment.description = cursor.getString(cursor.getColumnIndex(KEY_DESC));

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

    private String getContent(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        String Query = "SELECT * FROM " + TABLE_CALENDAR + " WHERE " + KEY_CALENDAR_ID + "=" + id;
        Cursor cursor = db.rawQuery(Query, null);

        if (cursor.getCount() == 1) {
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndex(KEY_PREV_CONTENT));
            }
            return null;
        } else {
            return null;
        }
    }
}
