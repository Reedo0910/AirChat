package com.example.zeee.bluechat;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zeee.bluechat.Manager.BlueToothManager;
import com.example.zeee.bluechat.Service.BlueToothService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ScanFragment extends Fragment {

    private FloatingActionButton scanBtn;
    private ListView listView;

    private DeviceItemAdapter deviceItemAdapter;
    private ArrayList<BluetoothDevice> DeviceList;
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.getDefault());
    SQLiteDatabase db;

    public ScanFragment() {}

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        BlueToothManager.getInstance().initManger(this.getActivity());
        initView();
        initData();
    }

    private void initView(){
        listView = (ListView) getActivity().findViewById(R.id.device_list);
        scanBtn = (FloatingActionButton)getActivity().findViewById(R.id.scan_btn);

        TextView textView = new TextView(getActivity());
        textView.setText(R.string.device_header);
        textView.setPadding(50, 0, 0, 0);
        listView.addHeaderView(textView, null, false);
    }

    private void initData(){
        DeviceList = new ArrayList<>();
        deviceItemAdapter = new DeviceItemAdapter();
        scanBtn.setOnClickListener(new ScanButtonListener());
        listView.setOnItemClickListener(new ListViewListener());
        listView.setAdapter(deviceItemAdapter);

        // 初始化数据库
        db = getActivity().openOrCreateDatabase("bluechat.db", Context.MODE_PRIVATE, null);
        String b_table = "CREATE TABLE IF NOT EXISTS b_table(_id integer primary key autoincrement, device_name text, device_address text, time text)";
        db.execSQL(b_table);

        // 打开蓝牙模块
        BlueToothManager.getInstance().openBluetooth(getActivity());
        //注册蓝牙扫描广播
        blueToothRegister();
        // 自动搜索
//        BlueToothManager.getInstance().startSearchDevice(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scan, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        // 创建服务端
        new Thread(new Runnable() {
            @Override
            public void run() {
                BlueToothService.getInstance().run(handler);
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        BlueToothManager.getInstance().unregisterReceiver(this.getActivity());
        db.close();
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1: //某个设备已接入
                    BluetoothDevice device = (BluetoothDevice) msg.obj;
                    Toast.makeText(getActivity(), device.getName() + "已接入", Toast.LENGTH_LONG).show();
                    fragmentReplace(device.getName());
                    break;
                case 2: //已连接上某个设备
                    BluetoothDevice device1 = (BluetoothDevice) msg.obj;
                    Toast.makeText(getActivity(), "已连接上" + device1.getName(), Toast.LENGTH_LONG).show();
                    fragmentReplace(device1.getName());
                    break;
            }
        }
    };

    // 进行Fragment跳转与传参
    private void fragmentReplace(String s) {
        MainActivity.getMainActivity().showFragment(R.id.navigation_chat);
        BlueToothManager.getInstance().setUsername(s);
    }

    // 注册蓝牙回调广播
    private void blueToothRegister() {
        BlueToothManager.getInstance().registerReceiver(this.getActivity(),new BlueToothManager.OnRegisterReceiver(){

            // 搜索到新设备
            @Override
            public void onBluetoothDevice(BluetoothDevice device) {
                // 信号强度
                if (DeviceList != null && !DeviceList.contains(device)) {
                    DeviceList.add(device);
                }
                if (deviceItemAdapter != null) {
                    deviceItemAdapter.notifyDataSetChanged();
                }

                Snackbar sb = Snackbar.make(getView(), "找到新设备", Snackbar.LENGTH_LONG).setActionTextColor(getResources().getColor(R.color.colorPrimaryDark));
                sb.getView().setBackgroundColor(0xff259b24);
                sb.show();
            }

            // 连接中
            @Override
            public void onBluetoothConnect(BluetoothDevice device) {
                Snackbar.make(getView(),"正在配对",Snackbar.LENGTH_LONG).show();
                if (deviceItemAdapter != null) {
                    deviceItemAdapter.notifyDataSetChanged();
                }
            }

            // 连接完成
            @Override
            public void onBluetoothEnd(BluetoothDevice device) {
                Snackbar.make(getView(),"配对成功",Snackbar.LENGTH_LONG).show();
                if (deviceItemAdapter != null) {
                    deviceItemAdapter.notifyDataSetChanged();
                }
                // 将数据插入数据库
                Date preDate = new Date(System.currentTimeMillis());
                String formattedDate = formatter.format(preDate);
                ContentValues deviceData = new ContentValues();
                deviceData.put("device_name", device.getName());
                deviceData.put("device_address", device.getAddress());
                deviceData.put("time", formattedDate);
                db.insert("b_table", null, deviceData);
            }

            // 未连接
            @Override
            public void onBluetoothNone(BluetoothDevice device) {
                Snackbar.make(getView(),"配对取消",Snackbar.LENGTH_LONG).show();
                if (deviceItemAdapter != null) {
                    deviceItemAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private class DeviceItemAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return DeviceList.size();
        }

        @Override
        public Object getItem(int position) {
            return DeviceList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            ViewHolder viewHolder;
            BluetoothDevice device = DeviceList.get(position);
            if (convertView == null) {
                // 加载行布局文件，产生具体的一行
                view = getActivity().getLayoutInflater().inflate(R.layout.device_item, null);
                viewHolder = new ViewHolder();
                // 将该行的控件全部存储到viewHolder中
                viewHolder.nameText = (TextView) view.findViewById(R.id.dname);
                viewHolder.adressText = (TextView) view.findViewById(R.id.dadress);
                viewHolder.statusText = (TextView) view.findViewById(R.id.dstatus);
                view.setTag(viewHolder); // 将viewHolder存储到行的Tag中
            } else {
                view = convertView; // 取出隐藏在这一行中的viewHolder控件缓存对象
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.nameText.setText(device.getName());
            viewHolder.adressText.setText(device.getAddress());
            viewHolder.statusText.setText(BlueToothManager.getInstance().getStatus(device.getBondState()));
            return view;
        }

        private class ViewHolder {
            TextView nameText, adressText, statusText;
        }
    }

    // 扫描按钮
    private class ScanButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
//            BlueToothManager.getInstance().openBluetooth(getActivity());
            Snackbar sb = Snackbar.make(view, "开始扫描", Snackbar.LENGTH_LONG).setActionTextColor(getResources().getColor(R.color.colorPrimaryDark));
            sb.getView().setBackgroundColor(0xff3b50ce);
            sb.show();
            BlueToothManager.getInstance().startSearchDevice(getActivity());
        }
    }

    private class ListViewListener implements AdapterView.OnItemClickListener{
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final BluetoothDevice bluetoothDevice = DeviceList.get(position-1);

            Snackbar sb = Snackbar
                    .make(view, "正在连接" + bluetoothDevice.getName(), Snackbar.LENGTH_LONG)
                    .setActionTextColor(getResources().getColor(R.color.colorPrimaryDark));
            sb.getView().setBackgroundColor(0xff3b50ce);
            sb.show();

            // 新建子进程进行蓝牙连接
            new Thread(new Runnable() {
                @Override
                public void run() {
                    BlueToothManager.getInstance().createBond(bluetoothDevice, handler);
                }
            }).start();
        }
    }
}


