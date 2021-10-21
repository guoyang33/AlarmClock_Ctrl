package com.zzzlarry.reminder;

import android.app.Service;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.concurrent.Semaphore;

public class NotificationCollectorService extends NotificationListenerService {

    private static final String TAG = "NCS";
    static NotificationCollectorService _this;
    static Semaphore sem = new Semaphore(0);

    public static NotificationCollectorService get() {
        sem.acquireUninterruptibly();
        NotificationCollectorService ret = _this;
        sem.release();
        return ret;
    }

    @Override
    public void onListenerConnected() {
        Log.i(TAG, "Connected");
        _this = this;
        sem.release();
    }

    @Override
    public void onListenerDisconnected() {
        Log.i(TAG, "Disconnected");
        sem.acquireUninterruptibly();
        _this = null;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
    }

//    @Override
//    public void onNotificationPosted(StatusBarNotification sbn) {
//        Log.i("xiaolong", "open" +  "-----" +  sbn.getPackageName());
//        Log.i("xiaolong", "open"  + "------" +  sbn.getNotification().tickerText);
//        Log.i("xiaolong", "open"  + "-----" +  sbn.getNotification().extras.get("android.title"));
//        Log.i("xiaolong", "open"  + "-----" +  sbn.getNotification().extras.get("android.text"));
//    }
//    @Override
//    public void onNotificationRemoved(StatusBarNotification sbn) {
//        Log.i("xiaolong", "remove" +  "-----" +  sbn.getPackageName());
//    }

}