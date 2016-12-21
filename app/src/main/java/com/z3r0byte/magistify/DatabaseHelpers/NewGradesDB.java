/*
 * Copyright (c) 2016-2016 Bas van den Boom 'Z3r0byte'
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

import net.ilexiconn.magister.container.Grade;

/**
 * Created by bas on 12-7-16.
 */
public class NewGradesDB extends SQLiteOpenHelper {
    private static final String TAG = "NewGradesDB";

    private static final int DATABASE_VERSION = 3;

    private static final String DATABASE_NAME = "newGradesDB";
    private static final String TABLE_GRADES = "new_grades";

    private static final String KEY_DB_ID = "dbID";
    private static final String KEY_DATE_ADDED = "dateAdded";
    private static final String KEY_IS_SEEN = "isSeen";
    private static final String KEY_HAS_BEEN_NOTIFIED = "notified";


    public NewGradesDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_GRADES_TABLE = "CREATE TABLE IF NOT EXISTS "
                + TABLE_GRADES + "("
                + KEY_DB_ID + " INTEGER PRIMARY KEY,"
                + KEY_DATE_ADDED + " TEXT,"
                + KEY_IS_SEEN + " BOOLEAN,"
                + KEY_HAS_BEEN_NOTIFIED + " TEXT"
                + ")";
        db.execSQL(CREATE_GRADES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade: New Version!");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GRADES);
        onCreate(db);
    }


    public void addGrades(Grade[] grades) {
        if (grades == null || grades.length == 0) {
            Log.d(TAG, "addGrades: No Grades!");
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        for (Grade grade : grades) {
            if (!isInDataBase(grade, db)) {
                contentValues.put(KEY_DATE_ADDED, grade.filledInDateString);
                contentValues.put(KEY_IS_SEEN, false);
                contentValues.put(KEY_HAS_BEEN_NOTIFIED, false);
                db.insert(TABLE_GRADES, null, contentValues);
            }
        }

        db.close();
    }

    public Boolean hasBeenSeen(Grade grade) {
        SQLiteDatabase db = this.getWritableDatabase();
        String Query = "Select * from " + TABLE_GRADES + " where " + KEY_DATE_ADDED + " = '" + grade.filledInDateString + "'";
        Cursor cursor = db.rawQuery(Query, null);
        if (cursor.getCount() == 1) {
            if (cursor.moveToFirst()) {
                if (cursor.getInt(cursor.getColumnIndex(KEY_IS_SEEN)) == 1) {
                    Log.d(TAG, "hasBeenSeen: Seen Before!");
                    return true;
                }
                cursor.close();
            }
        }

        cursor.close();
        isSeen(grade, db);

        return false;
    }

    private void isSeen(Grade grade, SQLiteDatabase db) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_DATE_ADDED, grade.filledInDateString);
        contentValues.put(KEY_IS_SEEN, true);
        db.update(TABLE_GRADES, contentValues, KEY_DATE_ADDED + " = '" + grade.filledInDateString + "'", null);

        db.close();
    }

    private Boolean isInDataBase(Grade grade, SQLiteDatabase db) {
        String Query = "Select * from " + TABLE_GRADES + " where " + KEY_DATE_ADDED + " = '" + grade.filledInDateString + "'";
        Cursor cursor = db.rawQuery(Query, null);
        if (cursor.getCount() >= 1) {
            cursor.close();
            return true;
        }
        cursor.close();

        return false;
    }

    public Boolean hasBeenNotified(Grade grade) {
        SQLiteDatabase db = this.getWritableDatabase();
        String Query = "Select * from " + TABLE_GRADES + " where " + KEY_DATE_ADDED + " = '" + grade.filledInDateString + "'";
        Cursor cursor = db.rawQuery(Query, null);
        if (cursor.getCount() == 1) {
            if (cursor.moveToFirst()) {
                if (cursor.getInt(cursor.getColumnIndex(KEY_HAS_BEEN_NOTIFIED)) == 1) {
                    Log.d(TAG, "hasBeenNotified: Has been notified");
                    return true;
                }
                cursor.close();
            }
        }

        cursor.close();
        hasBeenNotified(grade, db);

        return false;
    }

    private void hasBeenNotified(Grade grade, SQLiteDatabase db) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_DATE_ADDED, grade.filledInDateString);
        contentValues.put(KEY_HAS_BEEN_NOTIFIED, true);
        db.update(TABLE_GRADES, contentValues, KEY_DATE_ADDED + " = '" + grade.filledInDateString + "'", null);

        db.close();
    }

    public void removeAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        removeAll(db);
    }

    public void removeAll(SQLiteDatabase db) {
        db.delete(TABLE_GRADES, null, null);
    }
}
