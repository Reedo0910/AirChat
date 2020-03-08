package com.example.zeee.bluechat.Manager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.example.zeee.bluechat.BlueChatApplication;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class BlueToothManager {

    private BlueToothManager(){}

    private static class BlueToothManagers{
        private static BlueToothManager blueToothManager = new BlueToothManager();
    }

    public static BlueToothManager getInstance(){
        return BlueToothManagers.blueToothManager;
    }

    public static UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private String username = "未匹配用户";

    public String getUsername() {
        return username;
    }

    public void setUsername(String s) {
        this.username = s;
    }

    // 蓝牙管理器
    private BluetoothManager bltManager;

    public BluetoothManager getBluetoothManager() {
        return bltManager;
    }

    // 蓝牙适配器
    private BluetoothAdapter bluetoothAdapter;

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    // 配对成功后的蓝牙套接字
    private BluetoothSocket mBluetoothSocket;

    public BluetoothSocket getmBluetoothSocket() {
        return mBluetoothSocket;
    }


    public void initManger(Context context) {
        if (bltManager != null) {
            return;
        }
        bltManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bltManager != null) {
            bluetoothAdapter = bltManager.getAdapter();
        }
    }

    // 蓝牙状态接口
    private OnRegisterReceiver onRegisterReceiver;

    public interface OnRegisterReceiver {
        void onBluetoothDevice(BluetoothDevice device); //搜索到新设备
        void onBluetoothConnect(BluetoothDevice device); //配对中
        void onBluetoothEnd(BluetoothDevice device); //配对完成
        void onBluetoothNone(BluetoothDevice device);//未配对
    }

    public void registerReceiver(Context context, OnRegisterReceiver onRegisterReceiver) {
        this.onRegisterReceiver = onRegisterReceiver;
        // 用BroadcastReceiver来取得搜索结果
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND); //搜索发现设备
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED); //状态改变
        context.registerReceiver(bluetoothReceiver, intentFilter);
    }

    public void unregisterReceiver(Context context) {
        context.unregisterReceiver(bluetoothReceiver);
        if (getBluetoothAdapter() != null)
            getBluetoothAdapter().cancelDiscovery();
    }

    public void openBluetooth(Context context){
        if(getBluetoothAdapter() != null){
            if(!getBluetoothAdapter().isEnabled()){
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                context.startActivity(intent);
            }
            //得到所有已经被对的蓝牙适配器对象
            Set<BluetoothDevice> devices = getBluetoothAdapter().getBondedDevices();
            if(devices.size() > 0) {
                for (BluetoothDevice bluetoothDevice : devices) {
                    Log.i("已匹配的设备", bluetoothDevice.getAddress() + "(" + bluetoothDevice.getName() + ")");
                }
            }
        }
        else {
            Toast.makeText(context, "当前设备不支持蓝牙功能", Toast.LENGTH_SHORT).show();
        }
    }

    // 蓝牙连接
    private void connect(BluetoothDevice bDevice,Handler handler){
        try {
            // 通过和服务器协商的uuid来进行连接
            mBluetoothSocket = bDevice.createRfcommSocketToServiceRecord(SPP_UUID);
            // 将唯一的socket对象保存在application中
            if (mBluetoothSocket != null) {
                BlueChatApplication.bluetoothSocket = mBluetoothSocket;
            }
            if(getBluetoothAdapter().isDiscovering()){
                getBluetoothAdapter().cancelDiscovery(); // 终止搜索
            }
            if (!getmBluetoothSocket().isConnected()) {
                getmBluetoothSocket().connect();
            }
            if (handler == null) {
                return;
            }
            Message message = new Message();
            message.what = 2;
            message.obj = bDevice;
            handler.sendMessage(message);
        }catch (Exception e){
            Log.e("app", "连接失败");
            try {
                getmBluetoothSocket().close();
            }catch (IOException e1){
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    public void createBond(BluetoothDevice bDevice, Handler handler) {
        // 如果这个设备无配对，则尝试配对
        if (bDevice.getBondState() == BluetoothDevice.BOND_NONE){
            bDevice.createBond();
        }else if(bDevice.getBondState()==BluetoothDevice.BOND_BONDED){
            // 如果这个设备已配对，则尝试连接
            connect(bDevice, handler);
        }
    }

    public String getStatus(int status){
        String s = "未知";
        switch (status) {
            case BluetoothDevice.BOND_BONDING:
                s = "匹配中";
                break;
            case BluetoothDevice.BOND_BONDED:
                s = "已匹配";
                break;
            case BluetoothDevice.BOND_NONE:
                s = "未匹配";
                break;
        }
        return s;
    }

    public boolean startSearchDevice(Context context) {
        // 开始搜索设备
//        openBluetooth(context);
        if (getBluetoothAdapter().isDiscovering()) {
            stopSearchDevice();
        }
        getBluetoothAdapter().startDiscovery();
        return true;
    }

    public boolean stopSearchDevice(){
        // 暂停搜索设备
        return bluetoothAdapter != null && bluetoothAdapter.cancelDiscovery();
    }

    //广播接收者
    private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device;

            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                //获取到新设备
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                onRegisterReceiver.onBluetoothDevice(device);

            }else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                switch (device.getBondState()){
                    case BluetoothDevice.BOND_BONDING: //正在配对:
                        onRegisterReceiver.onBluetoothConnect(device);
                        break;
                    case BluetoothDevice.BOND_BONDED: //配对结束:
                        onRegisterReceiver.onBluetoothEnd(device);
                        break;
                    case BluetoothDevice.BOND_NONE: //未配对:
                        onRegisterReceiver.onBluetoothNone(device);
                        break;
                    default:
                        break;
                }
            }
        }
    };

}
