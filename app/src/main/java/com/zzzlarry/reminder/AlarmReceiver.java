/**************************************************************************
 *
 * Copyright (C) 2012-2015 Alex Taradov <alex@taradov.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *************************************************************************/

package com.zzzlarry.reminder;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

public class AlarmReceiver extends BroadcastReceiver {

    public static Ringtone mRingtone;
    public static Vibrator mVibrator;
    private final long[] mVibratePattern = {0, 500, 5000};
    private boolean mVibrate;
    private Uri mAlarmSound;
    private long mPlayTime;

    @Override
    public void onReceive(Context context, Intent intent) {
        int notification_id = 111;
        String alarmNotiChannelId = MainActivity.alarmNotiChannelId;

        Date dNow = new Date();
        @SuppressLint("SimpleDateFormat") SimpleDateFormat ft = new SimpleDateFormat("HH");
        String now_time = ft.format(dNow);
        Log.d("where", now_time);

        Intent newIntent;

        if ("03".equals(now_time)) {
            newIntent = new Intent(context, MainActivity.class);
            Alarm alarm = new Alarm(context);
            alarm.fromIntent(intent);
            alarm.toIntent(newIntent);
            newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            String TAG = "AlarmMe";
            Log.i(TAG, "AlarmReceiver.onReceive('" + alarm.getTitle() + "')");
        } else {
            //daily notification
            newIntent = new Intent(context, AlarmNotification.class);
            Alarm alarm = new Alarm(context);

            alarm.fromIntent(intent);
            alarm.toIntent(newIntent);
            newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            String TAG = "AlarmMe";
            Log.i(TAG, "AlarmReceiver.onReceive('" + alarm.getTitle() + "')");

            readPreferences(context);
            mRingtone = RingtoneManager.getRingtone(context, mAlarmSound);
            mRingtone.play();

            if (mVibrate)
                mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            mVibrator.vibrate(mVibratePattern, -1);
        }

        PendingIntent pi = PendingIntent.getActivity(context, 0, newIntent, 0);
        Notification notification = new NotificationCompat.Builder(context, alarmNotiChannelId)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Fiszy your Alarm is on")
                .setContentText("Click Me")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .build();
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManagerCompat.notify(notification_id, notification);
    }

    private void readPreferences(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        mAlarmSound = Uri.parse(prefs.getString("alarm_sound_pref", "DEFAULT_RINGTONE_URI"));
        mVibrate = prefs.getBoolean("vibrate_pref", true);
        mPlayTime = (long) Integer.parseInt(prefs.getString("alarm_play_time_pref", "30")) * 1000;
    }

    public static void stop() {
        mRingtone.stop();
        mVibrator.cancel();
    }

}

