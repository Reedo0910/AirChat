package com.example.zeee.bluechat.Service;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.zeee.bluechat.BlueChatApplication;
import com.example.zeee.bluechat.Manager.BlueToothManager;

import java.io.IOException;

// 蓝牙服务
public class BlueToothService {

    // 设置为单例
    private BlueToothService() {
        createBlueToothService();
    }

    private static class BlueToothServices {
        private static BlueToothService blueToothService = new BlueToothService();
    }

    public static BlueToothService getInstance() {
        return BlueToothServices.blueToothService;
    }

    private BluetoothServerSocket bluetoothServerSocket;
    private BluetoothSocket socket;

    public BluetoothSocket getSocket() {
        return socket;
    }

    public BluetoothServerSocket getBluetoothServerSocket() {
        return bluetoothServerSocket;
    }

    //在获得蓝牙适配器后从中创建一个蓝牙服务作为服务端
    private void createBlueToothService() {
        try {
            if (BlueToothManager.getInstance().getBluetoothAdapter() != null && BlueToothManager.getInstance().getBluetoothAdapter().isEnabled()) {
                bluetoothServerSocket = BlueToothManager.getInstance().getBluetoothAdapter().listenUsingInsecureRfcommWithServiceRecord("com.example.zeee.bluechat", BlueToothManager.getInstance().SPP_UUID);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run(Handler handler) {
        while (true) {
            try {
                BlueToothManager.getInstance().stopSearchDevice();
                //线程阻塞至等待蓝牙设备连接完毕后才执行
                socket = getBluetoothServerSocket().accept();
                if (socket != null) {
                    // 保存socket
                    BlueChatApplication.bluetoothSocket = socket;
                    Message message = new Message();
                    message.what = 1;
                    message.obj = socket.getRemoteDevice();
                    handler.sendMessage(message);
                    getBluetoothServerSocket().close();
                }
            } catch (IOException e) {
                try {
                    getBluetoothServerSocket().close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                break;
            }
        }
    }

    public void cancel() {
        try {
            getBluetoothServerSocket().close();
        } catch (IOException e) {
            Log.e("app", "关闭服务器的socket失败" + e);
        }
    }
}
