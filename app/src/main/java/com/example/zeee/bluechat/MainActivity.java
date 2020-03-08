package com.example.zeee.bluechat;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.example.zeee.bluechat.Service.BlueToothService;

public class MainActivity extends AppCompatActivity {

    private FragmentManager fm;
    private FragmentTransaction ft;

    private ScanFragment sf;
    private ChatFragment cf;
    private RecentFragment rf;

    private static final String POSITION = "position";
    private int position;

    private static MainActivity mainActivity;

    public static MainActivity getMainActivity() {
        return mainActivity;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //屏幕旋转时记录位置
        outState.putInt(POSITION, position);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        //屏幕恢复时取出位置
        showFragment(savedInstanceState.getInt(POSITION));
        super.onRestoreInstanceState(savedInstanceState);
    }

    public boolean showFragment(int id) {
        fm = getFragmentManager();
        ft = fm.beginTransaction();
        hideFragment(ft);
        position = id;

        switch (id) {
            case R.id.navigation_scan:
                if (sf == null) {
                    sf = new ScanFragment();
                    ft.add(R.id.fragment_content, sf);
                } else {
                    ft.show(sf);
                }
                ft.commit();
                return true;
            case R.id.navigation_chat:
                if (cf == null) {
                    cf = new ChatFragment();
                    ft.add(R.id.fragment_content, cf);
                } else {
                    ft.show(cf);
                }
                ft.commit();
                return true;
            case R.id.navigation_recent:
                if (rf == null) {
                    rf = new RecentFragment();
                    ft.add(R.id.fragment_content, rf);
                } else {
                    ft.show(rf);
                }
                ft.commit();
                return true;
        }
        return false;
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            return showFragment(item.getItemId());
        }

    };

    private void hideFragment(FragmentTransaction ft) {
        if (sf != null) {
            ft.hide(sf);
        }
        if (cf != null) {
            ft.hide(cf);
        }
        if (rf != null) {
            ft.hide(rf);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mainActivity = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        fm = getFragmentManager();
        showFragment(R.id.navigation_scan);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BlueToothService.getInstance().cancel();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        if(outState != null) {
            String FRAGMENTS_TAG = "Android:support:fragments";
            // 删除保存的Fragment
            outState.remove(FRAGMENTS_TAG);
        }
    }

}
