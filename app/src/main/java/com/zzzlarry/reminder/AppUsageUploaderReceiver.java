package com.zzzlarry.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.ClientProtocolException;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;
import cz.msebera.android.httpclient.util.EntityUtils;

import static android.widget.Toast.LENGTH_LONG;

public class AppUsageUploaderReceiver extends BroadcastReceiver {

    private static final String TAG = "AUUR";

    private static String serverAddr = MainActivity.serverAddr;
    private static String yearNo = MainActivity.yearNo;
    private static String userId = MainActivity.userId;

    static String[] makeupList;

    @Override
    public void onReceive(final Context context, Intent intent) {
        // start fetching data makeup
        Log.d(TAG, "fetching data_makeup");

        new Thread(new Runnable() {
            @Override
            public void run() {
                String serverAddr = MainActivity.serverAddr;
                String yearNo = MainActivity.yearNo;
                String userId = MainActivity.userId;

                try {
                    HttpClient httpClient = HttpClientBuilder.create().build();
                    String uri = serverAddr + "App_2nd/Data_MakeUp.php?id=" + userId + "&yearno=" + yearNo;
                    Log.d(TAG, "Uri: " + uri);
                    HttpGet get = new HttpGet(uri);
                    HttpResponse response = httpClient.execute(get);
                    String responseText = EntityUtils.toString(response.getEntity());
                    Log.d(TAG, "Update response code: " + response.getStatusLine().getStatusCode());
                    Log.d(TAG, "Contents: " + responseText);
                    if (!"".equals(response)) {
                        makeupList = responseText.split("<br>");
                    }
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        if (makeupList != null) {
            for (String s : makeupList) {
                try {
                    doFileUpload(context, s);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 上傳csv至伺服器，每日匯出的csv儲存在/storage/emulated/0/AppUsage/export/daily/<日期>/<檔名>.csv
    private void doFileUpload(Context context, String date) throws FileNotFoundException {
        File folder1 = new File("/storage/emulated/0/AppUsage/export/daily/" + date + "/");
        String[] list1 = folder1.list();
        String iscsv;
        if (list1 != null) {
            Log.d(TAG, "folder1: " + folder1.toPath());
            Log.d(TAG, "length: " + list1.length);
            for (final String s : list1) {
                Log.d(TAG, "s: " + s);
                iscsv = s.substring(s.lastIndexOf("."));
                if (iscsv.equals(".csv")) {
                    File myFile = new File("/storage/emulated/0/AppUsage/export/daily/" + date + "/" + s);
                    Log.d(TAG, "file: " + myFile.toString());
                    RequestParams params = new RequestParams();
                    params.put("uploadedfile", myFile, "text/csv");
                    AsyncHttpClient client = new AsyncHttpClient();
                    Log.d("where", "Try to post file : " + s);
                    client.post(context, "http://120.108.111.131/App_2nd/receive_file_finish.php?id=" + userId, params, new AsyncHttpResponseHandler() {
                        @RequiresApi(api = Build.VERSION_CODES.P)
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                            Log.d("where", s + " 傳送成功");
                            String content = new String(responseBody);
//                            Toast.makeText(MainActivity.this, "今日資料已上傳" + content, LENGTH_LONG).show();
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                            Log.d("where", s + " 傳送失敗");
                            String content = new String(responseBody);
//                            Toast.makeText(MainActivity.this, "上傳失敗!" + content, LENGTH_LONG).show();
                        }

                    });
                }
            }
        } else {
            Log.d("where", "無此資料夾");
        }

    }

}
