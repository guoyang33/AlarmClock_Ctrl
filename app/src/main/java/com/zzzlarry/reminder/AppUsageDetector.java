package com.zzzlarry.reminder;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import static android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS;

public class AppUsageDetector extends Activity {

//    private final String TAG = "AUD";
//
//    @Override
//    protected void onCreate(Bundle bundle) {
//        super.onCreate(bundle);
//    }
//
//    public void showNotifications() {
//        if (isNotificationServiceEnabled()) {
//            Log.i(TAG, "Notification enabled -- trying to fetch it");
//            getNotifications();
//        } else {
//            Log.i(TAG, "Notification disabled -- Opening settings");
//            startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
//        }
//    }
//
//    private void getNotifications() {
//        Log.i(TAG, "Waiting for NotificationCollectorService");
//        NotificationCollectorService notificationCollectorService = NotificationCollectorService.get();
//        Log.i(TAG, "Active Notifications: [");
//        for (StatusBarNotification notification : notificationCollectorService.getActiveNotifications()) {
//            Log.i(TAG, "    " + notification.toString());
//            Log.i(TAG, "    " + notification.getPackageName() + " / " + notification.getTag() + " / " + notification.getId());
//        }
//        Log.i(TAG, "]");
//    }
//
//    private boolean isNotificationServiceEnabled() {
//        String pkgName = getPackageName();
//        final String allNames = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
//        if (allNames != null && !allNames.isEmpty()) {
//            for (String name : allNames.split(":")) {
//                if (getPackageName().equals(ComponentName.unflattenFromString(name).getPackageName())) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
}
