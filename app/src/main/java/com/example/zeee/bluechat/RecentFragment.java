package com.example.zeee.bluechat;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import static android.R.layout.simple_list_item_2;

public class RecentFragment extends Fragment {
    public RecentFragment() {
        // Required empty public constructor
    }

    SQLiteDatabase db;
    private ListView listView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_recent, container, false);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initDataBase();
        initView();
    }

    private void initDataBase() {
        db = getActivity().openOrCreateDatabase("bluechat.db", Context.MODE_PRIVATE, null);
        String b_table = "CREATE TABLE IF NOT EXISTS b_table(_id integer primary key autoincrement, device_name text, device_address text, time text)";
        db.execSQL(b_table);
    }

    private void initView() {
        listView = (ListView) getActivity().findViewById(R.id.recent_device_list);
        Cursor cursor = db.rawQuery("SELECT * FROM b_table ORDER BY _id DESC",null);
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this.getActivity(),
                R.layout.recent_device_item,
                cursor,
                new String[]{"device_name", "device_address", "time"},
                new int[]{R.id.rname, R.id.raddress, R.id.rtime},
                0);
        listView.setAdapter(adapter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        db.close();
    }
}
