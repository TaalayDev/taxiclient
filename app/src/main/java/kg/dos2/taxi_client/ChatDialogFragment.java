package kg.dos2.taxi_client;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static kg.dos2.taxi_client.MainActivity.FRAGMENT_CHAT;
import static kg.dos2.taxi_client.MainActivity.LOG_TAG;
import static kg.dos2.taxi_client.Prefs.URL_GET_DISP_LIST;
import static kg.dos2.taxi_client.Prefs.URL_SEND_MESS;

public class ChatDialogFragment extends BottomSheetDialogFragment {

    private int code = 0;

    class Disp {
        int id = 0;
        String login = "";
        String fname = "";
        String lname = "";
        String phone = "";
        String email = "";
        Disp() { }
        Disp(int id, String login, String fname, String lname, String phone, String email) {
            this.id = id;
            this.login = login;
            this.fname = fname;
            this.lname = lname;
            this.phone = phone;
            this.email = email;
        }
    }
    private ArrayList<Disp> dispList = new ArrayList<>();
    private LinearLayout messagesLayout;
    private EditText editMessageText;
    private FrameLayout sendMessageButton;
    NestedScrollView chatScroll;

    private DataBaseHelper dbhelper;

    private FragmentsListener callBack;
    public void setCallBack(FragmentsListener callBack) {
        this.callBack = callBack;
    }

    public ChatDialogFragment() {
        // Required empty public constructor
    }

    public static ChatDialogFragment newInstance(FragmentsListener callBack, Bundle args) {
        ChatDialogFragment fragment = new ChatDialogFragment();
        fragment.setArguments(args);
        fragment.setCallBack(callBack);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            code = getArguments().getInt("code", 3);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_chat_dialog, container, false);
        callBack.curFrag(FRAGMENT_CHAT);
        ImageView close = rootView.findViewById(R.id.chat_dialog_close);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChatDialogFragment.this.dismiss();
            }
        });
        dbhelper = new DataBaseHelper(getContext());
        TextView chat_with = rootView.findViewById(R.id.tv_chat_with);
        messagesLayout = rootView.findViewById(R.id.messagesLay);
        editMessageText = rootView.findViewById(R.id.edit_message_text);

        chatScroll = rootView.findViewById(R.id.chat_scroll);

        FloatingActionButton callButton = rootView.findViewById(R.id.callActionButton);
        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( code == 4 ) {
                    String phone = Prefs.currOrder.getString("phone", "");
                    Uri call = Uri.parse("tel:" + phone);
                    Intent surf = new Intent(Intent.ACTION_DIAL, call);
                    startActivity(surf);
                } else {
                    for ( int i = 0; i < dispList.size(); i++ ) {
                        if ( !dispList.get(i).phone.isEmpty() ) {
                            String phone = dispList.get(i).phone;
                            Uri call = Uri.parse("tel:" + phone);
                            Intent surf = new Intent(Intent.ACTION_DIAL, call);
                            startActivity(surf);
                            break;
                        }
                    }
                }
            }
        });

        sendMessageButton = rootView.findViewById(R.id.send_msg_view);
        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mess = editMessageText.getText().toString();
                if ( !mess.isEmpty() ) {
                    int getter = 0;
                    if ( code == 4 ) {
                        getter = Prefs.getIntDef(getContext(), Prefs.DRIVER_ID, 0);
                    }
                    Bundle bundle = new Bundle();
                    bundle.putInt("id", 0);
                    bundle.putInt("mCode", code);
                    bundle.putInt("getter", Prefs.getIntDef(getContext(), Prefs.DRIVER_ID, 0));
                    bundle.putInt("sender", 0);
                    bundle.putString("getterLogin", Prefs.getString(getContext(), Prefs.DRIVER));
                    bundle.putString("senderLogin", "");
                    bundle.putString("message", mess);
                    bundle.putString("date", "");
                    bundle.putInt("getterIsRead", 0);
                    request(code, getter, mess);
                    editMessageText.setText("");
                    hideKeyboard(editMessageText);
                    showMessage(bundle);
                    dbhelper.insertMessage(bundle);
                } else {
                    Toast.makeText(getContext(), "Введите текст сообщения!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        chatScroll.post(new Runnable() {
            @Override
            public void run() {
                chatScroll.fullScroll(View.FOCUS_DOWN);
            }
        });

        if ( code == 3 ) {
            chat_with.setText("Чат с диспетчером");
            request(164375, 0, "");
        } else {
            chat_with.setText("Чат с водителем");
        }

        openChat(code);
        return rootView;
    }

    private void hideKeyboard(View v) {
        InputMethodManager imm =  (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    public void showMessage(Bundle bundle) {
        if ( bundle.getInt("mCode") == 2 || bundle.getInt("mCode") == 5 ) {
            View view = getLayoutInflater().inflate(R.layout.getmess, messagesLayout, false);
            TextView tv = (TextView) view.findViewById(R.id.get_message_text);
            tv.setText(bundle.getString("message"));
            messagesLayout.addView(view);
        } else if ( bundle.getInt("mCode") == code ) {
            View view = getLayoutInflater().inflate(R.layout.sendmess, messagesLayout, false);
            TextView tv = view.findViewById(R.id.send_message_text);
            tv.setText(bundle.getString("message"));
            messagesLayout.addView(view);
        }
        chatScroll.post(new Runnable() {
            @Override
            public void run() {
                chatScroll.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        callBack.dialogDismiss();
    }

    int diffCode(int code) {
        switch (code) {
            case 3:
                return 2;
            case 4:
                return 5;
            default:
                return 0;
        }
    }

    public void openChat(int code)
    {
        try {
            messagesLayout.removeAllViews();
            String query = "SELECT * FROM `chats` WHERE (`mCode` = " + code + " OR `mCode` = " + diffCode(code) + " )";
            if ( code != 3 ) {
                int driver = Prefs.getIntDef(getContext(), Prefs.DRIVER_ID, 0);
                query += " AND ( `Sender` = " + driver + " OR `Getter` = " + driver + " )";
            }
            Log.e(LOG_TAG, "open chat query " + query);
            Cursor cursor = dbhelper.getReadableDatabase().rawQuery(query, null);
            while (cursor.moveToNext()) {
                Bundle bundle = new Bundle();
                bundle.putInt("id", cursor.getInt(cursor.getColumnIndex("id")));
                bundle.putInt("mCode", cursor.getInt(cursor.getColumnIndex("mCode")));
                bundle.putInt("sender", cursor.getInt(cursor.getColumnIndex("sender")));
                bundle.putInt("getter", cursor.getInt(cursor.getColumnIndex("getter")));
                bundle.putString("senderLogin", cursor.getString(cursor.getColumnIndex("senderLogin")));
                bundle.putString("getterLogin", cursor.getString(cursor.getColumnIndex("getterLogin")));
                bundle.putString("message", cursor.getString(cursor.getColumnIndex("message")));
                bundle.putString("date", cursor.getString(cursor.getColumnIndex("date")));
                bundle.putInt("getterIsRead", cursor.getInt(cursor.getColumnIndex("getterIsRead")));
                showMessage(bundle);
            }
        } catch (Exception e) {

        }
    }

    public void request(int code, int getter, String mess) {
        String url = URL_SEND_MESS;
        if ( code == 164375 ) url = URL_GET_DISP_LIST;
        StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>(){
            @Override
            public void onResponse(String s) {
                try {
                    Log.e(LOG_TAG, "chat request code " + code + " " + s);
                    if (code == 1) {
                        JSONArray arr = new JSONArray(s);
                        Log.e("myimag", "products size - " + arr.length());
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            final int idd = obj.getInt("id");
                            String login = obj.getString("login");
                            String fname = obj.getString("fname");
                            String lname = obj.getString("lname");
                            String phone = obj.getString("phone");
                            String email = obj.getString("email");
                            Disp disp = new Disp(idd, login, fname, lname, phone, email);
                            dispList.add(disp);
                        }
                    }
                } catch (JSONException jse) {
                    Log.e(LOG_TAG, "chfr json exc " + jse.getMessage());
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e("myimag", "VolleyError: " + volleyError);
                request(code, getter, mess);
            }
        }) {
            @Override
            public Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<>();
                if ( code == 4 || code == 3 ) {
                    params.put("code", "" + code);
                    params.put("getter", "" + getter);
                    params.put("sender", "" + Prefs.getIntDef(getContext(), Prefs.DRIVER_ID, 0));
                    params.put("text", mess);
                } else if ( code == 1 ) {
                    params.put("id", "0");
                }
                return params;
            }

            @Override
            public Priority getPriority() {
                return Priority.IMMEDIATE;
            }
        };
        RequestQueue rQueue = Volley.newRequestQueue(getContext());
        rQueue.add(request);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void onDetach() {
        super.onDetach();

    }

}
