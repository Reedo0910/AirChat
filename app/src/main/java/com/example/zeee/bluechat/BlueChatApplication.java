package com.example.zeee.bluechat;

import android.app.Application;
import android.bluetooth.BluetoothSocket;

public class BlueChatApplication extends Application {
//    private static BlueChatApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
//        instance = this;
    }

//    public static BlueChatApplication getInstance() {
//        return instance;
//    }

    // 存储socket对象
    public static BluetoothSocket bluetoothSocket;
}
