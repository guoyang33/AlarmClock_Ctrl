package com.zzzlarry.reminder;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.ClientProtocolException;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;
import cz.msebera.android.httpclient.util.EntityUtils;

import static android.widget.Toast.LENGTH_LONG;

public class AppUsageUploader extends Activity {

    private String TAG = "AUU";

    private String serverAddr = MainActivity.serverAddr;
    private String userId = MainActivity.userId;

    public static String makeupDateText;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent activityIntent = new Intent(this, MainActivity.class);
        startActivity(activityIntent);
        Toast.makeText(AppUsageUploader.this, "檢查未上傳資料...", LENGTH_LONG).show();

        if (!"".equals(makeupDateText)) {
            String[] makeupDateList = makeupDateText.split("<br>");
            for (String date : makeupDateList) {
                try {
                    doFileUpload(date);
                } catch (FileNotFoundException | ParseException e) {
                    e.printStackTrace();
                }
            }
            Toast.makeText(AppUsageUploader.this, "資料上傳完成!", LENGTH_LONG).show();
        } else {
            Toast.makeText(AppUsageUploader.this, "資料已無任何缺失!", LENGTH_LONG).show();
        }
        makeupDateText = "";
    }

    // 上傳csv至伺服器，每日匯出的csv儲存在/storage/emulated/0/AppUsage/export/daily/<日期>/<檔名>.csv
    private void doFileUpload(final String date) throws FileNotFoundException, ParseException {
        File folder1 = new File("/storage/emulated/0/AppUsage/export/daily/" + date + "/");
        String[] list1 = folder1.list();
        String iscsv;
        if (list1 != null) {
            for (final String s : list1) {
                iscsv = s.substring(s.lastIndexOf("."));
                if (iscsv.equals(".csv")) {
                    File myFile = new File("/storage/emulated/0/AppUsage/export/daily/" + date + "/" + s);
                    RequestParams params = new RequestParams();
                    params.put("uploadedfile", myFile, "text/csv");
                    AsyncHttpClient client = new AsyncHttpClient();
                    Log.d("where", "Try to post file : " + s);
                    String url = serverAddr + "/App_2nd/receive_file_finish.php?id=" + userId;
                    client.post(this, url, params, new AsyncHttpResponseHandler() {
                        @RequiresApi(api = Build.VERSION_CODES.P)
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                            Log.d("where", s + " 傳送成功");
                            String content = new String(responseBody);
                            Toast.makeText(AppUsageUploader.this, "今日資料已上傳" + content, LENGTH_LONG).show();
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                            Log.d("where", s + " 傳送失敗");
                            String content = new String(responseBody);
                            Toast.makeText(AppUsageUploader.this, "上傳失敗!" + content, LENGTH_LONG).show();
                        }

                    });
                }

            }
        } else {
            Log.d("where", "無此資料夾");
            Date date1 = new SimpleDateFormat("yyyy-MM-dd").parse(date);
            Log.d(TAG, "time: " + date1.getTime());
            Date today = new Date();
            Log.d(TAG, "now: " + today.getTime());
            if (today.getTime() - date1.getTime() > 2*24*60*60*1000) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String serverAddr = MainActivity.serverAddr;
                        String userId = MainActivity.userId;

                        try {
                            HttpClient httpClient = HttpClientBuilder.create().build();
                            String uri = serverAddr + "/App_2nd/makeup_cancel.php?id=" + userId + "&date=" + date;
                            Log.d(TAG, "Uri: " + uri);
                            HttpGet get = new HttpGet(uri);
                            httpClient.execute(get);
                        } catch (ClientProtocolException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        }

    }

}
