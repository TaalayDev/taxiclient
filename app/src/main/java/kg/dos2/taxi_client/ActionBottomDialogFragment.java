package kg.dos2.taxi_client;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import static kg.dos2.taxi_client.MainActivity.LOG_TAG;

public class ActionBottomDialogFragment extends BottomSheetDialogFragment {
    public static final String TAG = "ActionBottomDialog";

    private static final String ARG_PARAM1 = "street1";
    private static final String ARG_PARAM2 = "street2";

    private String street1;
    private String street2;

    EditText tvMyLoc;
    DataBaseHelper dbhelper;
    FragmentsListener callback;

    void setCallback(FragmentsListener callback) {
        this.callback = callback;
    }

    public ActionBottomDialogFragment() {
        // Required empty public constructor
    }

    public static ActionBottomDialogFragment newInstance(Bundle bundle) {
        ActionBottomDialogFragment fragment = new ActionBottomDialogFragment();
        Bundle args = bundle;
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            street1 = getArguments().getString(ARG_PARAM1);
            street2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_action_bottom_dialog, container, false);
        try {
            ImageView close = view.findViewById(R.id.frbt_close);
            close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ActionBottomDialogFragment.this.dismiss();
                }
            });
            dbhelper = new DataBaseHelper(getContext());

            LinearLayout scrLy = view.findViewById(R.id.scr_ly);
            SQLiteDatabase sqlDB = dbhelper.getWritableDatabase();

            Cursor cursor = sqlDB.rawQuery("SELECT * FROM `orders` ORDER BY `fav` DESC;", null);
            while ( cursor.moveToNext() ) {
                int id = cursor.getInt(cursor.getColumnIndex("id"));
                String dt = cursor.getString(cursor.getColumnIndex("date"));
                String adr1 = cursor.getString(cursor.getColumnIndex("addr1"));
                String adr2 = cursor.getString(cursor.getColumnIndex("addr2"));
                View v = getLayoutInflater().inflate(R.layout.searchlist_item_view, scrLy, false);

                TextView tvRep1 = v.findViewById(R.id.searchListItemTitle);
                tvRep1.setText(adr1);
                TextView tvRep2 = v.findViewById(R.id.searchListItemTitleBottom);
                tvRep2.setText(adr2);

                v.findViewById(R.id.search_list_item_card).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ActionBottomDialogFragment.this.dismiss();
                        Log.e(LOG_TAG, "favordclick " + id);
                        callback.ordFromFav(id);
                    }
                });
                scrLy.addView(v);
            }
            cursor.close();
            dbhelper.close();
            sqlDB.close();
        } catch (Exception e) {
            Log.e(LOG_TAG, "actionBottomDialogFragmentException " + e.toString());
        }
        return view;
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
