package kg.dos2.taxi_client;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.gigamole.navigationtabstrip.NavigationTabStrip;
import com.gjiazhe.panoramaimageview.GyroscopeObserver;
import com.gjiazhe.panoramaimageview.PanoramaImageView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.jaredrummler.materialspinner.MaterialSpinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static kg.dos2.taxi_client.MainActivity.LOG_TAG;
import static kg.dos2.taxi_client.MainActivity.tariffList;
import static kg.dos2.taxi_client.Prefs.URL_CHECK_CLIENT;
import static kg.dos2.taxi_client.Prefs.URL_CHECK_CLIENT_PHONE;
import static kg.dos2.taxi_client.Prefs.URL_GET_CITIES_LIST;
import static kg.dos2.taxi_client.Prefs.URL_TRF_INFO;

public class LoginActivity extends AppCompatActivity {

    private GyroscopeObserver gyroscopeObserver;

    Button loginButton;
    FrameLayout frmLay;

    private ArrayList<Bundle> cities_list = new ArrayList<Bundle>();

    private static final String KEY_VERIFY_IN_PROGRESS = "key_verify_in_progress";

    private static final int STATE_INITIALIZED = 1;
    private static final int STATE_CODE_SENT = 2;
    private static final int STATE_VERIFY_FAILED = 3;
    private static final int STATE_VERIFY_SUCCESS = 4;
    private static final int STATE_SIGNIN_FAILED = 5;
    private static final int STATE_SIGNIN_SUCCESS = 6;

    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]

    private boolean mVerificationInProgress = false, verifyState = false, timerState = false;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    private EditText mPhoneNumberField;
    private EditText mUserNameField;
    private EditText mVerificationField;

    TextView sendCodeAgainText, cancelText;

    Timer timer;
    private int s = 0, m = 0;

    @Override
    protected void onResume() {
        super.onResume();
        // Register GyroscopeObserver.
        gyroscopeObserver.register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister GyroscopeObserver.
        gyroscopeObserver.unregister();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Restore instance state
        if (savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState);
        }

        // Initialize GyroscopeObserver.
        gyroscopeObserver = new GyroscopeObserver();
        // Set the maximum radian the device should rotate to show image's bounds.
        // It should be set between 0 and π/2.
        // The default value is π/9.
        gyroscopeObserver.setMaxRotateRadian(Math.PI/2);

        PanoramaImageView panoramaImageView = findViewById(R.id.panorama_image_view);
        // Set GyroscopeObserver for PanoramaImageView.
        panoramaImageView.setGyroscopeObserver(gyroscopeObserver);

        final NavigationTabStrip navigationTabStrip = findViewById(R.id.navTabStrip);
        navigationTabStrip.setTitles("Вход");
        navigationTabStrip.setTabIndex(0, true);

        frmLay = findViewById(R.id.frm_lay);
        loginButton = findViewById(R.id.loginButton);

        mPhoneNumberField = findViewById(R.id.phone);
        mUserNameField = findViewById(R.id.username);
        mVerificationField = findViewById(R.id.verification_code);

        sendCodeAgainText = findViewById(R.id.send_code_again_text);
        cancelText = findViewById(R.id.cancel_text);

        sendCodeAgainText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( !timerState ) {
                    String phone = mPhoneNumberField.getText().toString();
                    if (!phone.contains("+996")) {
                        phone = phone.replace(" ", "");
                        if (phone.charAt(0) == '0') {
                            phone = phone.substring(1);
                            phone = "+996" + phone;
                        } else if (phone.startsWith("996")) {
                            phone = "+" + phone;
                        } else {
                            phone = "+996" + phone;
                        }
                    }
                    resendVerificationCode(phone, mResendToken);
                }
            }
        });
        cancelText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( !timerState )
                    updateUI(STATE_INITIALIZED);
            }
        });

        request(0);

        // [START initialize_auth]
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        // [END initialize_auth]

        // Initialize phone auth callbacks
        // [START phone_auth_callbacks]
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                Log.d(LOG_TAG, "onVerificationCompleted:" + credential);
                mVerificationInProgress = false;
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                Log.w(LOG_TAG, "onVerificationFailed", e);
                mVerificationInProgress = false;

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    // [START_EXCLUDE]
                    mPhoneNumberField.setError("Invalid phone number.");
                    // [END_EXCLUDE]
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    // [START_EXCLUDE]
                    // Snackbar.make(findViewById(android.R.id.content), "Quota exceeded.",
                            // Snackbar.LENGTH_SHORT).show();
                    // [END_EXCLUDE]
                }
            }

            @Override
            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                Log.d(LOG_TAG, "onCodeSent:" + verificationId);

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;
                updateUI(STATE_CODE_SENT);
            }
        };
        // [END phone_auth_callbacks]

    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        // updateUI(currentUser);

        // [START_EXCLUDE]
        if (mVerificationInProgress && validatePhoneNumber()) {
            startPhoneNumberVerification(mPhoneNumberField.getText().toString());
        }
        // [END_EXCLUDE]
    }

    private boolean validatePhoneNumber() {
        String phoneNumber = mPhoneNumberField.getText().toString();
        if (TextUtils.isEmpty(phoneNumber)) {
            mPhoneNumberField.setError("Invalid phone number.");
            return false;
        }

        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_VERIFY_IN_PROGRESS, mVerificationInProgress);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mVerificationInProgress = savedInstanceState.getBoolean(KEY_VERIFY_IN_PROGRESS);
    }


    private void startPhoneNumberVerification(String phoneNumber) {

        // [START start_phone_auth]
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks);        // OnVerificationStateChangedCallbacks
        // [END start_phone_auth]

        mVerificationInProgress = true;
    }

    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        // [START verify_with_code]
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        // [END verify_with_code]
        signInWithPhoneAuthCredential(credential);
    }

    // [START resend_verification]
    private void resendVerificationCode(String phoneNumber,
                                        PhoneAuthProvider.ForceResendingToken token) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks,         // OnVerificationStateChangedCallbacks
                token);             // ForceResendingToken from callbacks
    }
    // [END resend_verification]

    // [START sign_in_with_phone]
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        loginButton.setEnabled(true);
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(LOG_TAG, "signInWithCredential:success");

                            FirebaseUser user = task.getResult().getUser();
                            login();
                            // [START_EXCLUDE]
                            updateUI(STATE_SIGNIN_SUCCESS);
                            // [END_EXCLUDE]
                        } else {
                            // Sign in failed, display a message and update the UI
                            Log.w(LOG_TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                // [START_EXCLUDE silent]
                                mVerificationField.setError("Invalid code.");
                                Toast.makeText(LoginActivity.this, "Неправильный код", Toast.LENGTH_LONG).show();
                                // [END_EXCLUDE]
                            }
                            // [START_EXCLUDE silent]
                            // Update UI
                            // updateUI(STATE_SIGNIN_FAILED);
                            // [END_EXCLUDE]
                        }
                    }
                });
    }
    // [END sign_in_with_phone]

    public void login() {
        String name = mUserNameField.getText().toString();
        String phone = mPhoneNumberField.getText().toString();
        int city = cities_list.get(((Spinner) findViewById(R.id.cities_spinner))
                .getSelectedItemPosition())
                .getInt("id");
        loginButton.setEnabled(false);
        loginRequest(0, name, phone, city);
    }

    public void login(View view) {
        String name = mUserNameField.getText().toString();
        String phone = mPhoneNumberField.getText().toString();
        int city = cities_list.get(((Spinner) findViewById(R.id.cities_spinner)).getSelectedItemPosition()).getInt("id");
        if ( !verifyState && !mVerificationInProgress ) {
            Log.e(LOG_TAG, name + " " + phone);
            Pattern regex = Pattern.compile("[$&+,:;=\\\\?@#|/'<>.^*()%!-]");

            if (name.isEmpty()) {
                mUserNameField.setError("Заполните это поле!");
            } else if (regex.matcher(name).find()) {
                mUserNameField.setError("Имя не должно содержать символов[$&+,:;=\\\\?@#|/'<>.^*()%!-]");
            } else if (name.length() < 3) {
                mUserNameField.setError("Имя должен содержать больше 3 букв");
            } else if (phone.isEmpty()) {
                mPhoneNumberField.setError("Заполните это поле!");
            } else if (phone.length() < 9) {
                mPhoneNumberField.setError("Номер должен содержать больше 9 цифр");
            } else {
                loginButton.setEnabled(false);
                loginButton.setText("Проверка...");
                loginRequest(1, name, phone, city);
            }
        } else if ( verifyState && !mVerificationInProgress ) {
            String code = mVerificationField.getText().toString();
            loginButton.setEnabled(false);
            verifyPhoneNumberWithCode(mVerificationId, code);
        }
    }

    public void updateUI(int state) {
        switch (state) {
            case STATE_INITIALIZED:
                verifyState = false;
                sendCodeAgainText.setVisibility(View.GONE);
                cancelText.setVisibility(View.GONE);
                mUserNameField.setEnabled(true);
                mPhoneNumberField.setEnabled(true);
                mVerificationField.setVisibility(View.GONE);
                mVerificationField.setEnabled(false);
                loginButton.setEnabled(true);
                loginButton.setText("Войти");
                if ( timer != null ) {
                    timer.cancel();
                }
                break;
            case STATE_CODE_SENT:
                timer = new Timer();
                m = 0;
                s = 60;
                sendCodeAgainText.setText("Послать код заново 1:00");
                sendCodeAgainText.setTextColor(getResources().getColor(R.color.color_grey));
                sendCodeAgainText.setVisibility(View.VISIBLE);
                timerState = true;
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if ( s > 0 ) {
                                    s--;
                                    if ( s == 0 ) {

                                    }
                                    sendCodeAgainText.setText("Послать код заново " + m + ":" + s + "");
                                } else {
                                    sendCodeAgainText.setText("Послать код заново");
                                    sendCodeAgainText.setTextColor(getResources().getColor(R.color.color_1));
                                    cancelText.setVisibility(View.VISIBLE);
                                    timerState = false;
                                    cancel();
                                }
                            }
                        });
                    }
                }, 10, 1000L);
                Toast.makeText(this, "На ваш номер был выслан смс для подтверждения", Toast.LENGTH_SHORT).show();
                verifyState = true;
                mUserNameField.setEnabled(false);
                mPhoneNumberField.setEnabled(false);
                mVerificationField.setVisibility(View.VISIBLE);
                mVerificationField.setEnabled(true);
                loginButton.setEnabled(true);
                loginButton.setText("Подтвердить");
                break;
        }
    }

    public void loginRequest(int code, final String name, final String phone, int city) {
        String url = URL_CHECK_CLIENT;
        if ( code == 1 ) url = URL_CHECK_CLIENT_PHONE;
        final StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>(){
            @Override
            public void onResponse(String s) {
                try {
                    if ( code == 0 ) {
                        loginButton.setEnabled(true);
                        loginButton.setText("Войти");
                        Log.e(LOG_TAG, "login result: " + s);
                        if (!s.equals("-1")) {
                            JSONArray arr = new JSONArray(s);
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                Prefs.putInt(LoginActivity.this, Prefs.USER_ID, obj.getInt("ID"));
                                Prefs.putBoolean(LoginActivity.this, Prefs.IS_LOG, true);
                                Prefs.putString(LoginActivity.this, Prefs.USER_NAME, name);
                                Prefs.putString(LoginActivity.this, Prefs.USER_PHONE, phone);
                                Prefs.putInt(LoginActivity.this, Prefs.USER_CITY, city);
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();
                            }
                        }
                    } else if ( code == 1 ) {
                        if ( s.equals("1") ) {
                            loginRequest(0, name, phone, city);
                        } else if ( s.equals("0") ) {
                            verifyState = true;
                            String phn = phone;
                            if ( !phn.contains("+996") ) {
                                phn = phn.replace(" ", "");
                                if ( phn.charAt(0) == '0' ) {
                                    phn = phn.substring(1);
                                    phn = "+996" + phn;
                                } else if ( phn.startsWith("996") ) {
                                    phn = "+" + phn;
                                } else {
                                    phn = "+996" + phn;
                                }
                            }
                            startPhoneNumberVerification(phn);
                        }
                    }
                } catch (Exception e) {
                    Log.e("myimag", "catException " + e);
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                loginButton.setEnabled(true);
                loginButton.setText("Войти");
                Log.e("myimag", "VolleyError: " + volleyError);
            }
        }) {
            @Override
            public Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<>();
                params.put("nm", name);
                params.put("phn", phone);
                params.put("city", String.valueOf(city));
                return params;
            }

            @Override
            public Priority getPriority() {
                return Priority.IMMEDIATE;
            }
        };
        RequestQueue rQueue = Volley.newRequestQueue(getApplicationContext());
        rQueue.add(request);
    }

    void onRequestsFinish() {
        if ( Prefs.getBooleanDef(this, Prefs.IS_LOG, false) ) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            Spinner mspin = findViewById(R.id.cities_spinner);

            List<String> list = new ArrayList<>();
            for ( int i = 0; i < cities_list.size(); i++ ) {
                list.add(cities_list.get(i).getString("city"));
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, list);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            mspin.setAdapter(adapter);

            YoYo.with(Techniques.FadeOutDown)
                    .duration(300)
                    .onEnd(new YoYo.AnimatorCallback() {
                        @Override
                        public void call(Animator animator) {
                            frmLay.setVisibility(View.GONE);
                        }
                    })
                    .playOn(frmLay);
        }
    }

    void request(int code) {
        String url = URL_GET_CITIES_LIST;
        final StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                try {
                    Log.e(LOG_TAG, "mainactivity request -> code: " + code + "; response:  " + s);
                    if ( code == 0 ) {
                        JSONArray arr = new JSONArray(s);
                        for ( int i = 0; i < arr.length(); i++ ) {
                            JSONObject obj = arr.getJSONObject(i);
                            Bundle bundle = new Bundle();
                            bundle.putInt("id", obj.getInt("id"));
                            bundle.putString("city", obj.getString("city"));
                            cities_list.add(bundle);
                        }
                        onRequestsFinish();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e("myimag", "JSONException " + e);
                } catch (Exception e) {
                    Log.e("myimag", "catException " + e);
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e("myimag", "VolleyError: " + volleyError);
                onRequestsFinish();
            }
        }) {
            @Override
            public Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<>();
                if ( code == 0 ) {

                }
                return params;
            }

            @Override
            public Priority getPriority() {
                return Priority.IMMEDIATE;
            }
        };
        RequestQueue rQueue = Volley.newRequestQueue(getApplicationContext());
        rQueue.add(request);
    }

}