package com.example.weatherroute;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "MapData.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Table creation operations
        String createCityTable = "CREATE TABLE cities (id INTEGER PRIMARY KEY, name TEXT, lat REAL, lon REAL)";
        String createRoadTable = "CREATE TABLE roads (from_id INTEGER, to_id INTEGER, distance INTEGER)";

        db.execSQL(createCityTable);
        db.execSQL(createRoadTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS cities");
        db.execSQL("DROP TABLE IF EXISTS roads");
        onCreate(db);
    }

    // Insert operation
    public void insertCity(int id, String name, double lat, double lon) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", id);
        values.put("name", name);
        values.put("lat", lat);
        values.put("lon", lon);
        db.insert("cities", null, values);
        db.close();
    }

    // Query operation
    public List<City> getAllCities() {
        List<City> cityList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM cities", null);

        if (cursor.moveToFirst()) {
            do {
                cityList.add(new City(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getDouble(2),
                        cursor.getDouble(3)
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return cityList;
    }
    // Insert a road (Edge)
    public void insertRoad(int fromId, int toId, int distance) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("from_id", fromId);
        values.put("to_id", toId);
        values.put("distance", distance);
        db.insert("roads", null, values);
        db.close();
    }

    // Get all roads
    public List<Road> getAllRoads() {
        List<Road> roadList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM roads", null);

        if (cursor.moveToFirst()) {
            do {
                roadList.add(new Road(
                        cursor.getInt(0),
                        cursor.getInt(1),
                        cursor.getInt(2)
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return roadList;
    }
}