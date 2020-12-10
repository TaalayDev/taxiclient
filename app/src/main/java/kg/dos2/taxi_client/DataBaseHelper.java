package kg.dos2.taxi_client;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static kg.dos2.taxi_client.MainActivity.LOG_TAG;

public class DataBaseHelper extends SQLiteOpenHelper {

    DataBaseHelper(Context context) {
        super(context, "kg_dos2_taxi_client.db", null, 4);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.e(LOG_TAG, "databasehelper oncreate");
        db.execSQL("CREATE TABLE IF NOT EXISTS `orders` (`id` INTEGER, `addr1` TEXT, `addr2` TEXT, " +
                "`desc` TEXT, `phone` TEXT, `cost` INTEGER, `dist` REAL, " +
                "`lat1` REAL, `lng1` REAL, `lat2` REAL, `lng2` REAL, `date` TEXT, `fav` INTEGER);");
        db.execSQL("CREATE TABLE IF NOT EXISTS `chats` (`id` INTEGER, `mCode` INTEGER, `sender` INTEGER, " +
                "`getter` INTEGER, `senderLogin` TEXT, `getterLogin` TEXT, `message` TEXT, `date` TEXT, `getterIsRead` INTEGER);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.e(LOG_TAG, "databasehelper onupgrade");
        db.execSQL("DROP TABLE IF EXISTS `orders`;");
        db.execSQL("DROP TABLE IF EXISTS `chats`;");
    }

    public void insertOrder(Bundle order) {
        int id = order.getInt("id", 0);
        if ( id > 0 ) {
            SQLiteDatabase db = getWritableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM `orders` WHERE id = " + id, null);
            if ( cursor.getCount() == 0 ) {
                String addr1 = order.getString("addr1", "");
                String addr2 = order.getString("addr2", "");
                cursor = db.rawQuery("SELECT * FROM `orders` WHERE `addr1` = '" + addr1 + "' AND `addr2` = '" + addr2 + "'", null);
                if ( cursor.getCount() == 0 ) {
                    ContentValues values = new ContentValues();
                    values.put("`id`", id);
                    values.put("`addr1`", addr1);
                    values.put("`addr2`", addr2);
                    values.put("`desc`", order.getString("desc", ""));
                    values.put("`phone`", order.getString("phone", ""));
                    values.put("`cost`", order.getInt("price", 0));
                    values.put("`dist`", order.getFloat("dist", 0));
                    values.put("`lat1`", (float) order.getDouble("lat1", 0));
                    values.put("`lng1`", (float) order.getDouble("lng1", 0));
                    values.put("`lat2`", (float) order.getDouble("lat2", 0));
                    values.put("`lng2`", (float) order.getDouble("lng2", 0));
                    SimpleDateFormat simpleDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss",
                            Locale.getDefault());
                    String strDt = simpleDate.format(new Date());
                    values.put("`date`", strDt);
                    values.put("`fav`", 1);
                    db.insert("`orders`", null, values);
                    Log.e(LOG_TAG, "insertorder #" + id);
                } else {
                    try {
                        if (cursor.moveToNext()) {
                            int fav = cursor.getInt(cursor.getColumnIndex("fav"));
                            int idd = cursor.getInt(cursor.getColumnIndex("id"));
                            db.execSQL("UPDATE `orders` SET `fav` = " + fav + " WHERE id = " + idd);
                        }
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "update order fav exception " + e.getMessage());
                    }
                }
            }
            cursor.close();
            db.close();
        }
    }

    public void insertMessage(Bundle bundle) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", bundle.getInt("id"));
        values.put("mCode", bundle.getInt("mCode"));
        values.put("sender", bundle.getInt("sender"));
        values.put("getter", bundle.getInt("getter"));
        values.put("senderLogin", bundle.getString("senderLogin"));
        values.put("getterLogin", bundle.getString("getterLogin"));
        values.put("message", bundle.getString("message"));
        values.put("date", bundle.getString("date"));
        values.put("getterIsRead", bundle.getInt("getterIsRead"));
        db.insert("chats", null, values);
        db.close();
    }


    public void deleteOrder(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM `orders` WHERE `id` = " + id);
        db.close();
    }

    public Bundle getOrdById(int id) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM `orders` WHERE id = " + id, null);
        if ( cursor.moveToNext() ) {
            Bundle bundle = new Bundle();
            bundle.putInt("id", id);
            bundle.putString("addr1", cursor.getString(cursor.getColumnIndex("addr1")));
            bundle.putString("addr2", cursor.getString(cursor.getColumnIndex("addr2")));
            bundle.putString("desc", cursor.getString(cursor.getColumnIndex("desc")));
            bundle.putString("phone", cursor.getString(cursor.getColumnIndex("phone")));
            bundle.putInt("price", cursor.getInt(cursor.getColumnIndex("cost")));
            bundle.putFloat("dist", cursor.getFloat(cursor.getColumnIndex("dist")));
            bundle.putFloat("lat1", cursor.getFloat(cursor.getColumnIndex("lat1")));
            bundle.putFloat("lng1", cursor.getFloat(cursor.getColumnIndex("lng1")));
            bundle.putFloat("lat2", cursor.getFloat(cursor.getColumnIndex("lat2")));
            bundle.putFloat("lng2", cursor.getFloat(cursor.getColumnIndex("lng2")));
            return bundle;
        }
        db.close();
        return null;
    }

}
