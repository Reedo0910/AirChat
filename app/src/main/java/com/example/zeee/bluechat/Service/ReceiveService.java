package com.example.zeee.bluechat.Service;

import android.bluetooth.BluetoothSocket;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;

import com.example.zeee.bluechat.BlueChatApplication;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReceiveService {

    public static void receiveMessage(Handler handler) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        File file;
        File SdDir = Environment.getExternalStorageDirectory();

        if (BlueChatApplication.bluetoothSocket == null || handler == null) {
            Log.i("bluechat", "socket或handler为null");
            return;
        }
        try {
            InputStream inputStream = BlueChatApplication.bluetoothSocket.getInputStream();
            String json_string;
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            // 获取信息
            while (true) {
                while ((json_string = bufferedReader.readLine()) != null) {
                    // 图片信息接收
                    if ("isimage".equals(json_string)) {
                        Date curDate = new Date(System.currentTimeMillis());
                        String fname = "bluechat_" + formatter.format(curDate) + ".jpg";
                        long picSize = Long.parseLong(bufferedReader.readLine()); //获取目标图片大小

                        File dataDir = new File(SdDir, "bluechat");
                        if (!dataDir.exists()) dataDir.mkdirs(); //创建存储数据文件的路径
                        file = new File(dataDir, fname); //设置数据文件
                        try {
                            file.createNewFile();
                        } catch (IOException e) {
                            Log.e("bluchat", "创建缓存文件失败");
                        }
                        FileOutputStream fileOutputStream = new FileOutputStream(file);
                        int length;
                        int size = 0;
                        byte[] bytes = new byte[1024];
                        while ((length = inputStream.read(bytes)) != -1) {
                            fileOutputStream.write(bytes, 0, length);
                            size += length;
                            if (size >= picSize) {
                                break;
                            }
                        }
                        fileOutputStream.close();
                        Message message = new Message();
                        message.obj = file.getAbsolutePath(); // 获取文件的绝对路径
                        message.what = 2;
                        handler.sendMessage(message);
                    } else if ("istext".equals(json_string)) {
                        String message_txt = bufferedReader.readLine();
                        Message message = new Message();
                        message.obj = message_txt;
                        message.what = 1; // 文本信息
                        handler.sendMessage(message);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
