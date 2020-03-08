package com.example.zeee.bluechat.Service;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.example.zeee.bluechat.BlueChatApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class SendService {

    // 发送文本信息
    public static void sendMessage(String message) {
        if (BlueChatApplication.bluetoothSocket == null || TextUtils.isEmpty(message)) {
            return;
        }
        try {
            OutputStream outputStream = BlueChatApplication.bluetoothSocket.getOutputStream();
            outputStream.write("istext\n".getBytes("utf-8"));
            message += "\n";
            outputStream.write(message.getBytes("utf-8"));
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 发送图片信息
    public static void sendImage(String path, Handler handler) {
        if (BlueChatApplication.bluetoothSocket == null || TextUtils.isEmpty((path)) || handler == null) {
            return;
        }
        try {
            OutputStream outputStream = BlueChatApplication.bluetoothSocket.getOutputStream();
            File file = new File(path);
            if (!file.exists()) {
                Log.i("bluechat", path + "：文件不存在");
                return;
            }
            if (file.isDirectory()) return;
            outputStream.write("isimage\n".getBytes("utf-8"));
            outputStream.write((String.valueOf(file.length()) + "\n").getBytes("utf-8"));
            // 将文件写入流
            FileInputStream fileInputStream = new FileInputStream(file);
            // 每次传1M
            byte[] bytes = new byte[1024];
            int length;
            int size = 0; // 检测上传进度
            while ((length = fileInputStream.read(bytes)) != -1) {
                size += length;
                // 将文件写入socket输出流
                outputStream.write(bytes, 0, length);
            }
            fileInputStream.close();
            Message message = new Message();
            message.what = 0;
            message.obj = "图片发送成功";
            handler.sendMessage(message);
            outputStream.write("\n".getBytes("utf-8"));
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
