package com.zzzlarry.reminder;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.ClientProtocolException;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;
import cz.msebera.android.httpclient.util.EntityUtils;

public class CheckQuesLackReceiver extends BroadcastReceiver {

    private static final String TAG = "CQLR";

    public static boolean quesLack;
    private String userId = MainActivity.user_id;
    private String yearNo = "109";

//    private NotificationManager notifications;

    @Override
    public void onReceive(Context context, Intent intent) {
        int notification_id = 113;
        String silenceNotiChannelId = MainActivity.silenceNotiChannelId;

        Log.d(TAG, "Starting fetch questionnaire data");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpClient httpClient = HttpClientBuilder.create().build();
                    HttpGet get = new HttpGet("http://120.108.111.131/App_2nd/alarmclock_panel/get_ques_lack.php?id=" + userId + "&yearno=" + yearNo);
//                    HttpGet get = new HttpGet("http://120.108.111.131/App_2nd/Ctrl_daily/check_ques_lack.php?id=user1092206&yearno=" + yearNo);
                    HttpResponse response = httpClient.execute(get);
                    String responseText = EntityUtils.toString(response.getEntity());
                    JSONObject obj = new JSONObject(responseText);
                    JSONArray lackList = obj.getJSONObject("contents").getJSONArray("lack_list");
                    Log.d(TAG, "ques lack count: " + lackList.length());
                    if (lackList.length() > 0) {
                        Log.d(TAG, "got some questionnaire not finished.");
                        CheckQuesLackReceiver.quesLack = true;
                    } else {
                        CheckQuesLackReceiver.quesLack = false;
                    }
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        if (quesLack) {
            Log.d(TAG, "posting noti...");
            Uri uri = Uri.parse("http://120.108.111.131/App_2nd/alarmclock_panel/questionnaire_status.php?id=" + userId + "&yearno=" + yearNo);
            Intent newIntent = new Intent(Intent.ACTION_VIEW, uri);
            PendingIntent pi = PendingIntent.getActivity(context, 0, newIntent, 0);
            Notification notification = new NotificationCompat.Builder(context, silenceNotiChannelId)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle("尚有每週評估單未填寫")
                    .setContentText("點擊這裡以查看")
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentIntent(pi)
                    .build();
            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
            notificationManagerCompat.notify(notification_id, notification);
        }
    }

}