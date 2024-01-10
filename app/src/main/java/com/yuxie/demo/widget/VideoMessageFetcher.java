package com.yuxie.demo.widget;

import android.os.Environment;
import android.util.Log;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class VideoMessageFetcher {

    public static final String TAG = "DOWNLOAD";


    /**
     * headers = {
     * <p>
     * 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3970.5 Safari/537.36',
     * <p>
     * 'Refer'
     * <p>
     * 'er': 'https://www.bilibili.com/'
     * <p>
     * }
     *
     * @param url
     * @return
     * @throws IOException
     */
    public static List<JSONObject> getVideoMsgByBv(String url) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3970.5 Safari/537.36")
                .addHeader("Refer", "")
                .addHeader("er", "https://www.bilibili.com/")
                .build();
        //调用enqueue异步请求方式
        List<JSONObject> list = new ArrayList<>();
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            Log.i(TAG, "testAsyncRequest==>result=");
            // Using regex to extract video message
            Pattern pattern = Pattern.compile(">window.__INITIAL_STATE__=(.*?)</script><script>", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(responseBody);

            if (matcher.find()) {
                String videoPlayInfoMsg = matcher.group(1);
                String videoMsgStr = videoPlayInfoMsg.split(";")[0];
                JSONObject videoMsg = new Gson().fromJson(videoMsgStr, new TypeToken<JSONObject>() {
                }.getType());
                JSONArray jsonArray = videoMsg.getJSONObject("videoData").getJSONArray("pages");
                String innerUrl = "https://www.bilibili.com/video/" + videoMsg.getString("bvid");
                String filename = "未知.mp3";
                for (Object obj : jsonArray) {
                    JSONObject jsonObject = new JSONObject((Map<String, Object>) obj);
                    filename = jsonObject.getString("part").replace("/", ".") + ".mp3";
                    String targetUrl = innerUrl + "?p=" + jsonObject.getInteger("page");

                    JSONObject object = new JSONObject();
                    object.put("targetUrl",targetUrl);
                    object.put("filename",filename);
                    list.add(object);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }



    /**
     * 获取音频链接
     *
     * @param url
     * @return
     */
    public static String getAudioUrl(String url) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3970.5 Safari/537.36")
                .addHeader("Refer", "")
                .addHeader("er", "https://www.bilibili.com/")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Unexpected code " + response);
            }

            String responseBody = response.body().string();

            // Using regex to extract video message
            Pattern pattern = Pattern.compile(">window.__playinfo__=(.*?)</script><script>", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(responseBody);

            if (matcher.find()) {
                String videoPlayInfoMsg = matcher.group(1);
                Type type = new TypeToken<JSONObject>() {
                }.getType();

                // 使用 Gson 将 JSON 字符串转换为 Map 对象
                Gson gson = new Gson();
                JSONObject map = gson.fromJson(videoPlayInfoMsg, type);
                Object o = map.getJSONObject("data")
                        .getJSONObject("dash")
                        .getJSONArray("audio")
                        .get(0);

                return new JSONObject((Map<String, Object>) o).getString("baseUrl");
            } else {
                throw new IOException("Video message not found in the response");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean downloadFile(String url, String homeUrl, String fileName) {
        OkHttpClient client = new OkHttpClient().newBuilder().build();

        // 获取外部存储的根目录
        File externalStorageDir = Environment.getExternalStorageDirectory();

        // 指定文件夹名称，例如 "MyAppDownloads"
        String folderName = "tt";

        // 构建文件夹路径
        File downloadDir = new File(externalStorageDir, folderName);

        // 如果文件夹不存在，则创建它
        if (!downloadDir.exists()) {
            if (!downloadDir.mkdirs()) {
                Log.e(TAG, "Failed to create directory: " + downloadDir.getAbsolutePath());
                return false;
            }
        }

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Referer", homeUrl)
                .build();

        try {
            Response response = client.newCall(request).execute();

            // 构建文件路径
            File outputFile = new File(downloadDir, fileName);

            try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
                fileOutputStream.write(response.body().bytes());
                fileOutputStream.flush();
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return false;
    }

}
