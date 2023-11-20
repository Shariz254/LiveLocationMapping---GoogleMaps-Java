package com.example.livelocationmapping;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;

public class dbHelper {

    static String TAG = dbHelper.class.getSimpleName();
    static Context act;
    public static boolean loaded_db = false;
    public static SQLiteDatabase database = null;

    public dbHelper(Context act){

        dbHelper.act = act;

        if (!loaded_db){
            try {
                setup_db();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        SharedPreferences sharedPref = act.getApplicationContext().getSharedPreferences("com.example.alltestsandroid", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("version_code", ""+getAppVersion(act.getApplicationContext()));
        editor.apply();

    }

    public int getAppVersion(Context cont){
        PackageInfo pinfo = null;
        try {
            pinfo = cont.getPackageManager().getPackageInfo(cont.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return pinfo.versionCode;
    }

    public void updateDBVersion(String version_code){
        ContentValues cv = new ContentValues();
        cv.put("version_code", version_code);
        cv.put("version_name", version_code);

        database.insert("app_version", null, cv);
    }

//    public void RunDBUpdates(Context act) {
//        dbHelper.act = act;
//        if (!loaded_db) {
//            try {
//                setup_db();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }

    public dbHelper(Context ctx, Activity act) {
        String dbpath = act.getExternalFilesDir(null).getAbsolutePath() + "/" + "alltest.db";
        SQLiteDatabase.loadLibs(act);
        act=act;
        Log.v("gggggggg here ", "jjjjjjjjjj===== init");
        AESHelper aesHelper = new AESHelper(ctx);
        if (database == null)
            database = SQLiteDatabase.openOrCreateDatabase(dbpath, aesHelper.getkey2(), null);
    }

    public dbHelper(Context ctx, Context act) {
        String dbpath = act.getExternalFilesDir(null).getAbsolutePath() + "/" + "alltest.db";
        SQLiteDatabase.loadLibs(act);
        Log.v("gggggggg here ", "jjjjjjjjjj===== init");
        AESHelper aesHelper = new AESHelper(ctx);
        if (database == null)
            database = SQLiteDatabase.openOrCreateDatabase(dbpath, aesHelper.getkey2(), null);

    }


    void setup_db(){
        String dbpath = act.getExternalFilesDir(null).getAbsolutePath() + "/" + "alltest.db";
        SQLiteDatabase.loadLibs(act);

        Log.v("db ---", "hereee ===>");

        AESHelper aesHelper = new AESHelper(act);
        database = SQLiteDatabase.openOrCreateDatabase(dbpath, aesHelper.getkey2(), null);

        initdb();
        create_indexs();
    }

    public void create_indexs() {
        try {

//            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS search_indexs_inventory  ON TBL_inventory (supplier_id, receipt_no,center_id, session_no, tanktaskid, tapperNo, name);");

        } catch (Exception e) {
            Log.e("this_index_exist", "==========> " + e.getMessage());
        }

    }

    public int runningDBVersion(Context ct){
        int code = 0;
        try {
            code =  ct.getSharedPreferences("com.capturesolutions.weightcapture", Context.MODE_PRIVATE).getInt("version_code", 0);
        }
        catch (Exception e){
            code =  0;
        }
        return code;
    }

    public void initdb(){
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS TBL_polygon (id INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL , latitude VARCHAR, longi VARCHAR, acerage VARCHAR, polygon_id VARCHAR, polygon_name VARCHAR)");
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS TBL_polygon_cache (id INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL , latitude VARCHAR, longi VARCHAR, acerage VARCHAR, polygon_id VARCHAR, polygon_name VARCHAR)");

    }
    //maps
    public void deleteExistingCacheLastPoint() {
        int max_id = 0;

        Cursor cursor = database.rawQuery("SELECT MAX(id)  from TBL_polygon_cache", null);
        if (!cursor.isAfterLast()) {
            cursor.moveToFirst();
            do {
                max_id = cursor.getInt(0);
            } while (cursor.moveToNext());
        }
        cursor.close();
        try {
            database.execSQL("DELETE FROM TBL_polygon_cache WHERE id = ?", new String[]{max_id + ""});
        } catch (Exception ex) {
        }
    }

    public void deleteExistingCachePolygon() {

        try {
            database.execSQL("DELETE FROM TBL_polygon_cache WHERE id > ?", new String[]{0 + ""});
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
    public ArrayList<String> fetchExistingPolygonIds(){
        ArrayList<String> polygonIdsList = new ArrayList<String>();

        Cursor cursor = database.rawQuery("SELECT polygon_id from TBL_polygon GROUP BY polygon_id ", null);
        if (!cursor.isAfterLast()) {
            cursor.moveToFirst();
            do {
                polygonIdsList.add(cursor.getString(cursor.getColumnIndexOrThrow("polygon_id")));
            } while (cursor.moveToNext());
        }

        return polygonIdsList;
    }

    public ArrayList<LatLng> fetchExistingPolygonLatLngs(String polygon_id){
        ArrayList<LatLng> arrayPoints = new ArrayList<LatLng>();
        if (polygon_id == null) {
            return null;
        }

        Cursor cursor = database.rawQuery("SELECT latitude, longi from TBL_polygon WHERE polygon_id =? ", new String[]{polygon_id});
        if (!cursor.isAfterLast()) {
            cursor.moveToFirst();
            do {
                LatLng point = new LatLng(cursor.getDouble(0), cursor.getDouble(1));
                arrayPoints.add(point);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return arrayPoints;
    }

    public String fetchPolygonAssignedName(String polygon_id) {
        String name = null;
        Cursor c = database.rawQuery("SELECT polygon_name FROM TBL_polygon WHERE polygon_id=? ", new String[]{polygon_id});
        if (!c.isAfterLast()) {
            c.moveToFirst();
            do {
                name = c.getString(c.getColumnIndexOrThrow("polygon_name"));
            } while (c.moveToNext());
        }
        c.close();

        return name;
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    public void insertMappedPolygon(ContentValues cv){
        Log.e("INSERT MAPPED POLYGON", "status: "+database.insertOrThrow("TBL_polygon", null, cv));
    }
    public void insertCachePolygonMappingProgress(ContentValues cv){
        Log.e("INSERT MAPPED POLYGON", "status: "+database.insertOrThrow("TBL_polygon_cache", null, cv));
    }
    //maps
}
