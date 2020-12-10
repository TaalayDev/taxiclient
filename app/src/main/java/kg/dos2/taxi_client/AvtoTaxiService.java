package kg.dos2.taxi_client;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static kg.dos2.taxi_client.MainActivity.LOG_TAG;

public class AvtoTaxiService extends Service {

    PendingIntent pend;
    public AvtoTaxiService() { }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        Log.e(LOG_TAG, "service onstart");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        if ( intent != null ) {
            pend = (PendingIntent) intent.getParcelableExtra("pIntent");
        }
        return super.onStartCommand(intent,flags,startID);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(LOG_TAG, "serrvice destroy");
    }
}
