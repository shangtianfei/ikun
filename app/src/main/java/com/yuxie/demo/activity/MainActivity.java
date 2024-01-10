package com.yuxie.demo.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.alibaba.fastjson.JSONObject;
import com.blankj.utilcode.util.ClipboardUtils;
import com.blankj.utilcode.util.ToastUtils;

import com.yuxie.baselib.utils.CommonUtils;
import com.yuxie.baselib.base.BaseActivity;
import com.yuxie.demo.R;
import com.yuxie.demo.widget.ClearEditText;
import com.yuxie.demo.widget.VideoMessageFetcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends BaseActivity {

    private static final int YOUR_PERMISSION_REQUEST_CODE = 89999;
    TextView tvExplain;

    ClearEditText etUrl;

    Context mContext;
    TextView resultLayout;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        // 在这里请求权限
        requestStoragePermission();

        initView();
    }

    protected void initView() {
        setTitle("b站音频下载");
        tvExplain = findViewById(R.id.tvExplain);
        etUrl = findViewById(R.id.et_url);
        resultLayout = findViewById(R.id.resultLayout);

        findViewById(R.id.download).setOnClickListener(v -> {
            download();
        });

        //调试使用
//        if (AppUtils.isAppDebug()) {
//            etUrl.setText("https://v.douyin.com/ArBxFg5/");
//            etUrl.setText("http://192.168.31.79:8080/#/pages/model/gift/gift");
//            etUrl.setText("https://www.baidu.com");
//        }

        etUrl.setText("https://www.bilibili.com/video/BV1rV4y1v7k9");
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerClipEvents();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            home.addCategory(Intent.CATEGORY_HOME);
            startActivity(home);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 注册剪切板复制、剪切事件监听
     */
    private void registerClipEvents() {
        CharSequence content = ClipboardUtils.getText();
        Log.i("TAG", "content:" + content);
        if (!TextUtils.isEmpty(content)) {
            String msgFromDouYin = content.toString();
            String url = CommonUtils.extractUrl(msgFromDouYin);
            etUrl.setText(url);
        }
    }

    private void openDouYinApp() {
        Intent intent = new Intent();
        ComponentName comp = new ComponentName("com.ss.android.ugc.aweme", "com.ss.android.ugc.aweme.splash.SplashActivity");
        intent.setComponent(comp);
        intent.setAction("android.intent.action.MAIN");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void download() {
        String url = etUrl.getText().toString();
        if (TextUtils.isEmpty(url)) {
            ToastUtils.showShort("请输入链接！");
            return;
        }

        if (CommonUtils.isDoubleClick(2000)) {
            ToastUtils.showShort("请稍后在下载！");
            return;
        }

        url = CommonUtils.extractUrl(url);
        if (TextUtils.isEmpty(url)) {
            ToastUtils.showShort("没有获取到链接!");
            return;
        }

        String finalUrl = url;

        resultLayout.setText("下载信息\n");
        List<Boolean> flagList = new ArrayList<>();


        executorService.execute(new Runnable() {
            @Override
            public void run() {
                List<JSONObject> list = VideoMessageFetcher.getVideoMsgByBv(finalUrl);
                for (JSONObject object : list) {
                    String targetUrl = VideoMessageFetcher.getAudioUrl(object.getString("targetUrl"));
                    String filename = object.getString("filename");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // 在主线程中更新UI组件
                            resultLayout.append(filename + "下载中\n");
                        }
                    });
                    boolean downFlag = VideoMessageFetcher.downloadFile(targetUrl, finalUrl, filename);
                    flagList.add(downFlag);
                    Log.i(this.getClass().getSimpleName(), filename + " 下载 " + (downFlag ? "成功" : "失败"));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // 在主线程中更新UI组件
                            resultLayout.append(filename + "下载" + (downFlag ? "成功" : "失败") + "\n");
                        }
                    });
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 在主线程中更新UI组件
                        int countTrueValues = countTrueValues(flagList);
                        resultLayout.append("下载已结束,下载统计:成功=" + countTrueValues + " 失败=" + (flagList.size() - countTrueValues));
                    }
                });
            }
        });
    }

    private void requestStoragePermission() {
        // 检查是否已经有了写入外部存储的权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 如果没有权限，就请求权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, YOUR_PERMISSION_REQUEST_CODE);
        } else {
            // 如果已有权限，执行文件写入操作
            // ...
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == YOUR_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户授予了写入外部存储的权限
                // 执行文件写入操作
                // ...
            } else {
                // 用户拒绝了权限请求，处理相应的逻辑
            }
        }
    }

    public static int countTrueValues(List<Boolean> booleanList) {
        int trueCount = 0;

        for (boolean value : booleanList) {
            if (value) {
                trueCount++;
            }
        }

        return trueCount;
    }
}
