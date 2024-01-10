package com.yuxie.demo.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
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
import com.yuxie.demo.status.DownStatus;
import com.yuxie.demo.widget.ClearEditText;
import com.yuxie.demo.widget.VideoMessageFetcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends BaseActivity {

    private static final int YOUR_PERMISSION_REQUEST_CODE = 89999;
    TextView tvExplain;

    ClearEditText etUrl;

    Context mContext;

    TableLayout tableLayout;
    TextView downMsgView;
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
        tableLayout = findViewById(R.id.resultLayout);
        downMsgView = findViewById(R.id.downMsg);
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

        List<DownStatus> flagList = new ArrayList<>();

        tableLayout.removeAllViews();
        downMsgView.setText("加载信息中。。。");


        executorService.execute(new Runnable() {
            @Override
            public void run() {
                List<JSONObject> list = VideoMessageFetcher.getVideoMsgByBv(finalUrl);

                // 遍历列表并添加到表格中
                for (JSONObject object : list) {
                    String targetUrl = VideoMessageFetcher.getAudioUrl(object.getString("targetUrl"));
                    String filename = object.getString("filename");

                    // 创建一个新的TableRow
                    TableRow tableRow = new TableRow(MainActivity.this);

                    // 创建两个TextView分别表示文件名和下载状态
                    TextView filenameTextView = new TextView(MainActivity.this);
                    filenameTextView.setText(filename);
                    filenameTextView.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    filenameTextView.setBackgroundResource(R.drawable.table_border);
                    filenameTextView.setGravity(Gravity.CENTER);
                    filenameTextView.setPadding(8, 8, 8, 8); // 根据需要调整
                    filenameTextView.setMaxLines(1); // 设置最大显示行数为1行
                    filenameTextView.setEllipsize(TextUtils.TruncateAt.END); // 在超过行数时显示省略号

                    TextView statusTextView = new TextView(MainActivity.this);
                    statusTextView.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    statusTextView.setBackgroundResource(R.drawable.table_border);
                    statusTextView.setGravity(Gravity.CENTER);
                    statusTextView.setPadding(8, 8, 8, 8); // 根据需要调整
                    statusTextView.setMaxLines(1); // 设置最大显示行数

                    // 在主线程中更新UI组件
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // 在主线程中更新UI组件
                            statusTextView.setText(DownStatus.LOADING.getMsg());
                            // 将TextView添加到TableRow中
                            tableRow.addView(filenameTextView);
                            tableRow.addView(statusTextView);

                            // 将TableRow添加到TableLayout中
                            tableLayout.addView(tableRow);
                        }
                    });

                    boolean downFlag = false;
                    DownStatus downStatus = DownStatus.ERROR;
                    for (int index = 0; (index < 5 && !downFlag); index++) {
                        // 实际下载逻辑
                        downStatus = VideoMessageFetcher.downloadFile(targetUrl, object.getString("targetUrl"), filename);
                        downFlag = DownStatus.OK.equals(downStatus)||DownStatus.NODOWN.equals(downStatus);
                        if (!downFlag) {
                            Log.e("MAIN-ERROR",filename+"第"+index+"次尝试失败 url="+targetUrl +"\n 重置url");
                            targetUrl = VideoMessageFetcher.getAudioUrl(object.getString("targetUrl"));
                        }
                    }
                    flagList.add(downStatus);
                    Log.i(this.getClass().getSimpleName(), filename + (downStatus.getMsg()));

                    // 更新下载状态
                    DownStatus finalDownStatus = downStatus;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // 在主线程中更新UI组件
                            statusTextView.setText(finalDownStatus.getMsg());
                        }
                    });
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 在主线程中更新UI组件
                        Map<String, Integer> countTrueValues = countTvShows(flagList);
                        String downMsg = "下载已结束,下载统计:" +
                                "总下载数="+flagList.size()+"; ";
                        for (Map.Entry<String, Integer> entry : countTrueValues.entrySet()) {
                            downMsg += entry.getKey() +"=" + entry.getValue()+"; ";
                        }
                        downMsgView.setText(downMsg);
                        Log.i("TAG", downMsg);
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


    private static Map<String, Integer> countTvShows(List<DownStatus> tvShows) {
        // 创建一个Map用于存储每个美剧及其出现次数
        Map<String, Integer> tvShowCountMap = new HashMap<>();

        for (DownStatus value : DownStatus.values()) {
            tvShowCountMap.put(value.getMsg(),0);
        }

        // 遍历List，统计每个美剧出现的次数
        for (DownStatus tvShow : tvShows) {
            tvShowCountMap.put(tvShow.getMsg(),tvShowCountMap.get(tvShow.getMsg()) + 1);
        }

        return tvShowCountMap;
    }

}
