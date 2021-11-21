package com.zzzlarry.reminder;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Looper;
import android.provider.ContactsContract;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
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

    public static String makeupContents;

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d(TAG, "Fetching make up date...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                String serverAddr = MainActivity.serverAddr;
                String yearNo = MainActivity.yearNo;
                String userId = MainActivity.userId;

                try {
                    HttpClient httpClient = HttpClientBuilder.create().build();
                    String uri = serverAddr + "/App_2nd/daily/data_makeup.php?id=" + userId;
                    Log.d(TAG, "Uri: " + uri);
                    HttpGet get = new HttpGet(uri);
                    HttpResponse response = httpClient.execute(get);
                    String contents = EntityUtils.toString(response.getEntity());
                    AppUsageUploader.makeupDateText = contents;
                    AppUsageUploaderReceiver.makeupContents = contents;
                    Log.d(TAG, "Fetch response code: " + response.getStatusLine().getStatusCode());
                    Log.d(TAG, "MakeUp Contents: " + contents);
                    Log.d(TAG, "Not empty: " + !"".equals(contents));
                    if (!"".equals(contents)) {
                        Log.d(TAG, "Pushing makeup notify...");
                        AppUsageUploaderReceiver.pushMakeupNoti(context);
                    }
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void pushMakeupNoti(Context context) {
        int notificationId = 114;
        String silenceNotiChannelId = MainActivity.silenceNotiChannelId;

        Intent newIntent = new Intent(context, AppUsageUploader.class);
        PendingIntent pi = PendingIntent.getActivity(context, 0, newIntent, 0);
        Notification notification = new NotificationCompat.Builder(context, silenceNotiChannelId)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("尚有資料未上傳")
                .setContentText("點擊這裡進行上傳作業")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pi)
                .build();
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManagerCompat.notify(notificationId, notification);
    }

}
