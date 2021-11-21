package com.zzzlarry.reminder;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.Notification;
    import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.ClientProtocolException;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS;

public class AppUsageDetectReceiver extends BroadcastReceiver {

    private static final String TAG = "AUDR";

    public static int appUsageStatus;

//    private NotificationManager notifications;

    @Override
    public void onReceive(Context context, Intent intent) {
        int notificationId = 112;
        String silenceNotiChannelId = MainActivity.silenceNotiChannelId;

        if (appUsageIsRunning()) {
            Log.d(TAG, "App Usage is running.");
            appUsageStatus = 1;
        } else {
            Log.d(TAG, "App Usage is not running.");
            appUsageStatus = 0;
            Intent newIntent = new Intent(context, AppUsageLauncher.class);
            PendingIntent pi = PendingIntent.getActivity(context, 0, newIntent, 0);
            Notification notification = new NotificationCompat.Builder(context, silenceNotiChannelId)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("App Usage現在處於未運行狀態")
                .setContentText("點擊這裡以啟動")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pi)
                .build();
            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
            notificationManagerCompat.notify(notificationId, notification);
        }

        Log.d(TAG, "Updating AU status data to server...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                String serverAddr = MainActivity.serverAddr;
                String userId = MainActivity.userId;
                int appUsageStatus = AppUsageDetectReceiver.appUsageStatus;
                long ts = System.currentTimeMillis();
                String sendTime = Long.toString(ts);

                try {
                    HttpClient httpClient = HttpClientBuilder.create().build();
                    String uri = serverAddr + "/App_2nd/app_status_update.php?id=" + userId + "&appstatus=" + appUsageStatus + "&sendtime=" + sendTime;
                    Log.d(TAG, "update uri: " + uri);
                    HttpGet get = new HttpGet(uri);
                    HttpResponse response = httpClient.execute(get);
                    Log.d(TAG, "Update response code: " + response.getStatusLine().getStatusCode());
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
//        Log.d("AUDR", Locale.getDefault().getDisplayLanguage());

    }

    public boolean appUsageIsRunning() {
        NotificationCollectorService notificationCollectorService = NotificationCollectorService.get();
        for (StatusBarNotification notification : notificationCollectorService.getActiveNotifications()) {
//            Log.i(TAG, "    " + notification.toString());
//            Log.i(TAG, "    " + notification.getPackageName() + " / " + notification.getTag() + " / " + notification.getId());
            if (notification.getPackageName().equals("com.a0soft.gphone.uninstaller") && notification.getId() == 103) {
                return true;
            }
        }
        return false;
    }

}
