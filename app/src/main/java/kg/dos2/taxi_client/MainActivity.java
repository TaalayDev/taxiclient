package kg.dos2.taxi_client;

import android.Manifest;
import android.animation.Animator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

import kg.dos2.taxi_client.lib.MyNetwork;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, FragmentsListener, MyNetwork.MyNetworkListener {


    public static String LOG_TAG = "taxi_client_tag";
    private final int PERMISSIONS_REQUESTS_CODE = 4;
    // Идентификатор уведомления
    private static final int NEW_MESSAGE_NOTIFY_ID = 101;
    // Идентификатор канала
    private static String NEW_MESSAGE_CHANNEL_ID = "Cat channel";

    Intent taxiIntent;
    FloatingActionButton fab_call, fab_chat;
    NavigationView navigationView;

    public static final int FRAGMENT_MAIN = 0;
    public static final int FRAGMENT_CHAT = 1;

    public static final int STATE_OFF = 0;
    public static final int STATE_ON = 1;
    public static final int STATE_ON_WAIT_ORDER_SYNC = 2;
    public static final int STATE_ON_DRIVER_WAIT = 3;
    public static final int STATE_ON_RIDE = 4;

    private int state = STATE_OFF, currentFragment = FRAGMENT_MAIN;

    public final int NOT_ID = 47345;

    public static ArrayList<Trf> tariffList = new ArrayList<>();
    public static ArrayList<Bundle> wishList = new ArrayList<>();

    MainFragment mainFragment;
    ChatDialogFragment chatFragment;

    Bundle bundle;
    DataBaseHelper dbhelper;

    MyNetwork network;

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        
    }

    public static void setState(Context context, int state) {
        Prefs.putInt(context, Prefs.APP_STATE, state);
    }

    public static int getState(Context context) {
        return Prefs.getIntDef(context, Prefs.APP_STATE, 0);
    }

    protected void onRestart() {
        super.onRestart();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();
        bundle = new Bundle();
        dbhelper = new DataBaseHelper(MainActivity.this);

        taxiIntent = new Intent(MainActivity.this, AvtoTaxiService.class);
        taxiIntent.putExtra("pIntent", createPendingResult(0, new Intent(), 0));
        startService(taxiIntent);

        state = getState(this);

        File checkDir = new File(Environment.getExternalStorageDirectory() + "/AutoTaxi/Messages/Audio");
        if (!checkDir.exists()) {
            checkDir.mkdirs();
        }

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        TextView unm = navigationView.getHeaderView(0).findViewById(R.id.nav_user_name);
        unm.setText(Prefs.getStringDef(this, Prefs.USER_NAME, "Гость"));

        fab_call = findViewById(R.id.fab_call);
        fab_call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String number = Prefs.currDrv.getString("phone");
                Uri call = Uri.parse("tel:" + number);
                Intent surf = new Intent(Intent.ACTION_DIAL, call);
                startActivity(surf);
            }
        });
        fab_chat = findViewById(R.id.fab_chat);
        fab_chat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDriverChat();
            }
        });

        wishList.clear();
        String[] wh_arr = getResources().getStringArray(R.array.wish_list);
        for (String s : wh_arr) {
            Bundle bnd = new Bundle();
            bnd.putInt("isCheck", 0);
            bnd.putString("name", s);
            wishList.add(bnd);
        }

        network = new MyNetwork(this, this);
        network.getTarrifes();

        mainFragment = new MainFragment();
        mainFragment.setCallBack(this);
        chatFragment = ChatDialogFragment.newInstance(this, new Bundle());
        replaceFragment(mainFragment);

        if ( !Prefs.getBooleanDef(this, Prefs.IS_LOG, false) ) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }

    }

    public void clearBundle(Bundle bund) {
        if ( !bund.isEmpty() ) {
            bund.clear();
        }
    }

    public void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    public void checkPermission() {
        // Here, thisActivity is the current activity
        if ( ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions(this,
                    new String[] { android.Manifest.permission.RECORD_AUDIO,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUESTS_CODE);

        } else {
            // Permission has already been granted
        }
    }

    public void menuClick(View v) {
        switch ( v.getId() ) {
            case R.id.menu_button:
                final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                } else {
                    drawer.openDrawer(GravityCompat.START);
                }
            break;
            case R.id.izb_address:
                ActionBottomDialogFragment actfrag = new ActionBottomDialogFragment();
                actfrag.setCallback(this);
                actfrag.show(getSupportFragmentManager(), "BOTTOM_DIALOG_FRAGMENT_TAG1");
                break;
            case R.id.header_dispatcher:
                chatFragment = ChatDialogFragment.newInstance(this, new Bundle());
                chatFragment.show(getSupportFragmentManager(), "CHAT_DIALOG_FRAGMENT_TAG");
                setMenuCounter(R.id.nav_dispatcher, 0);
                break;
        }
    }

    public void clickMaster(View v) {

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String result = data.getStringExtra("receive");

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if ( state == STATE_OFF || state == STATE_ON ) {
                if ( currentFragment == FRAGMENT_MAIN ) {
                    if ( mainFragment.onBackPressed() ) {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
                        alertDialog.setTitle("Выйти?");
                        alertDialog.setMessage("Вы действительно хотите выйти?");
                        alertDialog.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                MainActivity.super.onBackPressed();
                                setState(MainActivity.this, STATE_OFF);
                            }
                        });
                        alertDialog.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        alertDialog.show();
                    }
                } else {
                    replaceFragment(mainFragment);
                }
            } else {
                if ( currentFragment != FRAGMENT_MAIN && currentFragment != FRAGMENT_CHAT ) {
                    replaceFragment(mainFragment);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_orders) {
            ActionBottomDialogFragment actfrag = new ActionBottomDialogFragment();
            actfrag.setCallback(this);
            actfrag.show(getSupportFragmentManager(), "BOTTOM_DIALOG_FRAGMENT_TAG1");
        } else if (id == R.id.nav_dispatcher) {
            chatFragment = ChatDialogFragment.newInstance(this, new Bundle());
            chatFragment.show(getSupportFragmentManager(), "CHAT_DIALOG_FRAGMENT_TAG");
            setMenuCounter(R.id.nav_dispatcher, 0);
        } else if (id == R.id.nav_map) {

        } else if ( id == R.id.nav_exit ) {
            Prefs.putBoolean(this, Prefs.IS_LOG, false);
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else if (id == R.id.nav_share) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=kg.dos2.taxi_client");
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent,"Поделиться"));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setMenuCounter(@IdRes int itemId, int count) {
        try {
            Menu menuNav = navigationView.getMenu();
            MenuItem element = menuNav.findItem(itemId);
            if ( itemId == R.id.nav_dispatcher ) {
                String before = "Диспетчерская";
                if ( count > 0 ) {
                    String counter = Integer.toString(count);
                    String s = before + "   " + counter + " ";
                    SpannableString sColored = new SpannableString(s);

                    sColored.setSpan(new BackgroundColorSpan(Color.GRAY), s.length() - (counter.length() + 2), s.length(), 0);
                    sColored.setSpan(new ForegroundColorSpan(Color.WHITE), s.length() - (counter.length() + 2), s.length(), 0);

                    element.setTitle(sColored);
                } else {
                    element.setTitle(before);
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "setMenuCounterException " + e);
        }
    }



    void not(String title, String text, String chanel_id, int notify_id) {
        NotificationManager mNotificationManager;

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getApplicationContext(), chanel_id);
        Intent ii = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, ii, 0);

        NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
        bigText.bigText(text);
        bigText.setBigContentTitle(title);

        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher_round);
        mBuilder.setContentTitle(title);
        mBuilder.setContentText(text);
        mBuilder.setPriority(Notification.PRIORITY_MAX);
        mBuilder.setStyle(bigText);

        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // === Removed some obsoletes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            String channelId = chanel_id;
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    title,
                    NotificationManager.IMPORTANCE_HIGH);
            mNotificationManager.createNotificationChannel(channel);
            mBuilder.setChannelId(channelId);
        }

        mBuilder.setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 });
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        mBuilder.setSound(alarmSound);
        mNotificationManager.notify(notify_id, mBuilder.build());

    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    public void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void curFrag(int f) {
        currentFragment = f;
        if ( f == FRAGMENT_MAIN ) {
            clearBundle(bundle);
            switch (getState(this)) {
                case STATE_ON_WAIT_ORDER_SYNC:
                    // bundle.putInt("temp", 1);
                    network.lastOrderByID(1);
                    break;
                case STATE_ON_DRIVER_WAIT:
                    bundle.putInt("temp", 2);
                    // request(5, bundle);
                    network.lastOrderByID(2);
                    break;
                case STATE_ON_RIDE:
                    bundle.putInt("temp", 3);
                    // request(5, bundle);
                    network.lastOrderByID(3);
                    break;
                default:
                    // bundle.putInt("temp", 0);
                    // request(7, bundle);
                    network.checkFreeRide(new Function1<Integer, Unit>() {
                        @Override
                        public Unit invoke(Integer st) {
                            if ( st.intValue() == 1 ) {
                                if ( currentFragment == FRAGMENT_MAIN || currentFragment == FRAGMENT_CHAT ) {
                                    mainFragment.freeRide();
                                }
                            }
                            return null;
                        }
                    });
            }
        }
    }

    @Override
    public void cancOrd() {
        network.cancelOrder(new Function0<Unit>() {
            @Override
            public Unit invoke() {
                try {
                     setState(MainActivity.this, STATE_ON);
                     if ( currentFragment == FRAGMENT_MAIN ) {
                        mainFragment.cancRide();
                     }
                } catch (Exception e) {
                    Log.e("myimag", "catException $e");
                }
                return null;
            }
        });
    }

    @Override
    public void complOrd(int w) {

    }

    @Override
    public void newOrder(Bundle bun) {
        network.newOrder(bun, new Function0<Unit>() {
            @Override
            public Unit invoke() {
                return null;
            }
        });
    }

    @Override
    public void drvSearch() {
        //        clearBundle(bundle);
        //        bundle.putInt("temp", 0);
        //        request(4, bundle);
        network.getActiveDrivers(new Function1<String, Unit>() {
            @Override
            public Unit invoke(String s) {
                if ( currentFragment == FRAGMENT_MAIN || currentFragment == FRAGMENT_CHAT ) {
                    mainFragment.showDriversOnMap(s);
                }
                return null;
            }
        });
    }

    @Override
    public void ordInfo() {
        //        clearBundle(bundle);
        //        bundle.putInt("temp", 0);
        //        request(5, bundle);
        network.lastOrderByID(0);
    }

    @Override
    public void drvInfo(int drv) {
//        clearBundle(bundle);
//        bundle.putInt("drv", 0);
//        request(6, bundle);
        network.driverInfo(drv, 0);
    }

    void slideInUpAnimation(View view, int duration) {
        YoYo.with(Techniques.RotateInUpLeft).duration(duration)
                .onStart(new YoYo.AnimatorCallback() {
                    @Override
                    public void call(Animator animator) {
                        view.setVisibility(View.VISIBLE);
                    }
                })
                .playOn(view);
    }

    void slideOutDownAnimation(View view, int duration) {
        YoYo.with(Techniques.RotateOutDownRight).duration(duration)
                .onEnd(new YoYo.AnimatorCallback() {
                    @Override
                    public void call(Animator animator) {
                        view.setVisibility(View.GONE);
                    }
                })
                .playOn(view);
    }

    @Override
    public void footerDetailsShowOrHide(boolean show) {
        if ( !show ) {
            slideInUpAnimation(findViewById(R.id.fab_call), 300);
            slideInUpAnimation(findViewById(R.id.fab_chat), 300);
        } else {
            slideOutDownAnimation(findViewById(R.id.fab_call), 300);
            slideOutDownAnimation(findViewById(R.id.fab_chat), 300);
        }
    }

    @Override
    public void ordFromFav(int id) {
        if ( getState(MainActivity.this) == STATE_ON ||
                getState(MainActivity.this) == STATE_OFF ) {
            Bundle b = dbhelper.getOrdById(id);
            if (b != null) {
                if (currentFragment == FRAGMENT_MAIN) {
                    mainFragment.ordFromFav(b);
                }
            }
        }
    }

    @Override
    public void openDriverChat() {
        Bundle b = new Bundle();
        b.putInt("code", 4);
        mainFragment.changeChatCount(0);
        chatFragment = ChatDialogFragment.newInstance(this, b);
        footerDetailsShowOrHide(true);
        chatFragment.show(getSupportFragmentManager(), "CHAT_FRAGMENT_TAG");
        // replaceFragment(chatFragment);
    }

    @Override
    public void dialogDismiss() {
        if ( mainFragment != null && mainFragment.isVisible() ) {
            currentFragment = FRAGMENT_MAIN;
        }
    }

    @Override
    public void myNetworkData() {

    }

    @Override
    public void getLastOrder(int code, int state, @NotNull JSONObject obj, @NotNull Bundle bund) {
        try {
            if (code != 0) dbhelper.insertOrder(bund);
            if (state == 1) {
                setState(this, STATE_ON);
                if (code == 1)
                    mainFragment.complRide();
                else network.lastOrderByID(1);
            }
            if (state == 2) {
                int drv = obj.getInt("drv");
                Prefs.putInt(this, Prefs.DRIVER_ID, drv);
                // bundle.putInt("temp", temp);
                network.driverInfo(drv, code);
            }
            if (state == 3 && code == 0) {
                setState(this, STATE_ON);
                mainFragment.driverCancel(bund);
            }
            if ( state == 0 && ( code != 0 && code != 5 ) ) {
                if (currentFragment == FRAGMENT_MAIN || currentFragment == FRAGMENT_CHAT) {
                    mainFragment.driverSyncWaitState(Prefs.currOrder);
                }
            }
            if ( state == 4 && code == 0 ) {
                setState(this, STATE_ON_RIDE);
                if (currentFragment == FRAGMENT_MAIN) {
                    if ( !mainFragment.startRide() ) {
                        clearBundle(bundle);
                        int drv = obj.getInt("drv");
                        bundle.putInt("temp", 6);
                        // request(6, bundle);
                        network.driverInfo(drv, 6);
                    }
                }
            }
        } catch (JSONException jse) {
            Log.e(LOG_TAG, "MainActivity getLastOrder JSONException ");
        }
    }

    @Override
    public void getDriverInfo(@NotNull Bundle bund) {
        if ( getState(this) == STATE_ON_WAIT_ORDER_SYNC ) {
            setState(this, STATE_ON_DRIVER_WAIT);
            try {
                not(
                        "Водитель найден",
                        "Водитель уже в пути",
                        "NOT_FIND_DRIVER",
                        NOT_ID
                );

                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (currentFragment == FRAGMENT_MAIN || currentFragment == FRAGMENT_CHAT) {
                mainFragment.driverConnect(bund);
            }
        } else if ( getState(this) == STATE_ON_DRIVER_WAIT ) {
            if ( currentFragment == FRAGMENT_MAIN || currentFragment == FRAGMENT_CHAT ) {
                mainFragment.driverMarkerUpdate(bund);
            }
        } else if ( getState(this) == STATE_ON_RIDE ) {
            int code = bund.getInt("code");
            // Log.e(LOG_TAG, "MainActivity getDriverInfo STATEONRIDE " + code);
            if ( code == 6 ) {
                Log.e(LOG_TAG, "MainActivity getDriverInfo STATEONRIDE " + code);
                mainFragment.driverConnect(bund);
                mainFragment.startRide();
            } else {
                mainFragment.checkDriverLockOnRide(bund);
            }
        }
    }

    @Override
    public void getActiveDrivers(@NotNull String s) {
         if ( currentFragment == FRAGMENT_MAIN || currentFragment == FRAGMENT_CHAT ) {
            mainFragment.showDriversOnMap(s);
         }
    }

    @Override
    public void chatData(String sender, @NotNull Bundle b) {
        dbhelper.insertMessage(bundle);

        if (chatFragment.isVisible()) {
            chatFragment.showMessage(bundle);
        } else {
            if ( sender.equals("Диспетчер") ) {
                setMenuCounter(R.id.nav_dispatcher, 1);
            } else {
                try { mainFragment.changeChatCount(1); } catch (Exception e) { }
            }
            not(sender, bundle.getString("message"), NEW_MESSAGE_CHANNEL_ID, NEW_MESSAGE_NOTIFY_ID);
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            // Vibrate for 500 milliseconds
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                //deprecated in API 26
                v.vibrate(500);
            }
        }
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
