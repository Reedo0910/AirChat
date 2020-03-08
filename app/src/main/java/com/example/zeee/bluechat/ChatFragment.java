package com.example.zeee.bluechat;


import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zeee.bluechat.Manager.BlueToothManager;
import com.example.zeee.bluechat.Service.ReceiveService;
import com.example.zeee.bluechat.Service.SendService;

import java.io.File;
import java.io.IOException;


public class ChatFragment extends Fragment {

    private LinearLayout chatContent;
    private EditText messageBox;
    private ScrollView scrollView;

    private static final String IMAGE_UNSPECIFIED = "image/*";
    private final int IMAGE_CODE = 0;
    String path, s;

    public ChatFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        TextView titleView = (TextView) getActivity() .findViewById(R.id.ChatTitle);
        titleView.setText(BlueToothManager.getInstance().getUsername());
        messageBox = (EditText) getActivity().findViewById(R.id.message_box);
        Button sendImgBtn = (Button) getActivity().findViewById(R.id.send_img_btn);
        scrollView = (ScrollView) getActivity().findViewById(R.id.scroll_view);
        chatContent = (LinearLayout) getActivity().findViewById(R.id.chat_content);
        messageBox.setOnEditorActionListener(new MessageBoxKeyboardSendListener());
        sendImgBtn.setOnClickListener(new SendImgBtnListener());

        new Thread(new Runnable() {
            @Override
            public void run() {
                ReceiveService.receiveMessage(handler);
            }
        }).start();
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:// 图片状态:
                    if (TextUtils.isEmpty(msg.obj.toString()) || getActivity() == null) {
                        return;
                    }
                    Snackbar.make(getView(), msg.obj.toString(), Snackbar.LENGTH_SHORT).show();
                    break;
                case 1://文本消息
                    if (TextUtils.isEmpty(msg.obj.toString()) || getActivity() == null) {
                        return;
                    }
                    chatContent.addView(getTextView(msg.obj.toString(), 1));
                    scrollViewAutoScroll();
                    break;
                case 2: //图片消息
                    if (TextUtils.isEmpty(msg.obj.toString()) || getActivity() == null) {
                        return;
                    }
                    String filepath = msg.obj.toString();
                    File pic = new File(filepath);
                    if (pic.exists()) {
                        Bitmap bm = BitmapFactory.decodeFile(filepath);
                        ImageView imageView = getImgView(ThumbnailUtils.extractThumbnail(bm, 600, 450), filepath, 1);
                        chatContent.addView(imageView);
                    } else {
                        chatContent.addView(getTextView("[!图片接收失败]", 1));
                    }
                    scrollViewAutoScroll();
                    break;
            }
        }
    };

    //键盘发送键按下
    private class MessageBoxKeyboardSendListener implements TextView.OnEditorActionListener {
        @Override
        public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
            if (i == EditorInfo.IME_ACTION_SEND) {
                if (TextUtils.isEmpty(messageBox.getText().toString().trim())) {
                    return false;
                }
                s = messageBox.getText().toString().trim();
                chatContent.addView(getTextView(s, 0));
                messageBox.setText("");
                scrollViewAutoScroll();
                // 使用服务器发送信息
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        SendService.sendMessage(s);
                    }
                }).start();
                return true;
            }
            return false;
        }
    }

    // 查看图片
    private class PicViewListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            String p = view.getTag().toString();
            File f = new File(p);
            if (f.exists()) {
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(f), IMAGE_UNSPECIFIED);
                startActivity(intent);
            } else {
                Snackbar.make(getView(), "图片已被删除", Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    // 发送图片
    private class SendImgBtnListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(Intent.ACTION_PICK, null);
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, IMAGE_UNSPECIFIED);
            startActivityForResult(intent, IMAGE_CODE);
        }
    }

    private void scrollViewAutoScroll() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    /**
     * 界面文本信息处理
     * Tag 状态标记 (0: 发送方, 1: 接收方)
     */
    private TextView getTextView(String message, int Tag) {
        TextView textView = new TextView(getActivity());
        LinearLayout.LayoutParams layoutParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        layoutParams.setMargins(20, 20, 20, 20);
        textView.setPadding(40, 20, 40, 20);
        textView.setTextSize(18);
        if (Tag == 0) {
            layoutParams.gravity = Gravity.END;
            textView.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        } else {
            layoutParams.gravity = Gravity.START;
            textView.setBackgroundColor(getResources().getColor(R.color.colorAccent));
        }
        textView.setTextColor(getResources().getColor(R.color.white));
        textView.setLayoutParams(layoutParams);
        textView.setText(message);
        return textView;
    }

    /**
     * 界面图片信息处理
     * Tag 状态标记 (0: 发送方, 1: 接收方)
     */
    private ImageView getImgView(Bitmap bm, String picpath, int Tag) {
        ImageView imageView = new ImageView(getActivity());
        LinearLayout.LayoutParams layoutParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        layoutParams.setMargins(20, 20, 20, 20);
        imageView.setPadding(40, 20, 40, 20);
        imageView.setClickable(true);
        imageView.setFocusable(true);
        imageView.setTag(picpath);
        imageView.setClickable(true);
        imageView.setFocusable(true);
        imageView.setOnClickListener(new PicViewListener());
        if (Tag == 0) {
            layoutParams.gravity = Gravity.END;
            imageView.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        } else {
            layoutParams.gravity = Gravity.START;
            imageView.setBackgroundColor(getResources().getColor(R.color.colorAccent));
        }
        imageView.setLayoutParams(layoutParams);
        imageView.setImageBitmap(bm);
        return imageView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Bitmap bm = null;
        path = "";
        // 外界的程序访问ContentProvider所提供数据 可以通过ContentResolver接口
        ContentResolver resolver = getActivity().getContentResolver();
        if (requestCode == IMAGE_CODE) {
            try {
                Uri originalUri = data.getData(); // 获得图片的uri
                bm = MediaStore.Images.Media.getBitmap(resolver, originalUri);
                String[] proj = { MediaStore.Images.Media.DATA };
                // android多媒体数据库的封装接口
                Cursor cursor = getActivity().managedQuery(originalUri, proj, null, null, null);
                // 获得用户选择的图片的索引值
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                // 根据索引值获取图片路径
                path = cursor.getString(column_index);

                chatContent.addView(getImgView(ThumbnailUtils.extractThumbnail(bm, 600, 450), path, 0));
                scrollViewAutoScroll();
                //利用服务器传送图片
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        SendService.sendImage(path, handler);
                    }
                }).start();
            } catch (IOException e) {
                Log.e("app", e.toString());
            }
            finally {
                return;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
