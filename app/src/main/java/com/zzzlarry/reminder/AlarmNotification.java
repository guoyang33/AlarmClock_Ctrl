package com.zzzlarry.reminder;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Timer;
import java.util.TimerTask;

import cz.msebera.android.httpclient.Header;

import static android.widget.Toast.LENGTH_LONG;

public class AlarmNotification extends Activity {

    private final String serverAddr = MainActivity.serverAddr;
    private final String user_id = MainActivity.userId;

    private final String TAG = "AlarmMe";
    private Ringtone mRingtone;
    private Vibrator mVibrator;
    private final long[] mVibratePattern = {0, 500, 5000};
    private boolean mVibrate;
    private Uri mAlarmSound;
    private long mPlayTime;
    private Timer mTimer = null;
    private Alarm mAlarm;
    private DateTime mDateTime;
    private TextView mTextView;
    private PlayTimerTask mTimerTask;


    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.notification);

        AlarmReceiver.stop();

        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        mDateTime = new DateTime(this);
        mTextView = findViewById(R.id.alarm_title_text);
        //set up notification manager
        NotificationManager notify_manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        readPreferences();

        mRingtone = RingtoneManager.getRingtone(getApplicationContext(), mAlarmSound);
        if (mVibrate)
            mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        assert notify_manager != null;
//        notify_manager.notify(0, notification_popup);
        start(getIntent());
        WebView tota = findViewById(R.id.total);
        String url = serverAddr + "/App_2nd/daily/redirect.php?id=" + user_id;
        tota.loadUrl(url);
        GetDate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "AlarmNotification.onDestroy()");

        stop();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.i(TAG, "AlarmNotification.onNewIntent()");

        addNotification(mAlarm);

        stop();
        start(intent);
    }

    private void start(Intent intent) {
        mAlarm = new Alarm(this);
        mAlarm.fromIntent(intent);

        Log.i(TAG, "AlarmNotification.start('" + mAlarm.getTitle() + "')");

        mTextView.setText(mAlarm.getTitle());

        mTimerTask = new PlayTimerTask();
        mTimer = new Timer();
        mTimer.schedule(mTimerTask, mPlayTime);
//        mRingtone.play();
//        if (mVibrate)
//            mVibrator.vibrate(mVibratePattern, -1);
    }

    private void stop() {
        Log.i(TAG, "AlarmNotification.stop()");

        mTimer.cancel();
        mRingtone.stop();
        if (mVibrate)
            mVibrator.cancel();
    }

    public void onDismissClick(View view) {
        Intent intent = new Intent(this, AlarmNotification.class);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 1, intent, PendingIntent.FLAG_UPDATE_CURRENT | Intent.FILL_IN_DATA);
        alarmManager.cancel(pendingIntent);
        finish();
    }

    private void readPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        mAlarmSound = Uri.parse(prefs.getString("alarm_sound_pref", "DEFAULT_RINGTONE_URI"));
        mVibrate = prefs.getBoolean("vibrate_pref", true);
        mPlayTime = (long) Integer.parseInt(prefs.getString("alarm_play_time_pref", "30")) * 1000;
    }

    private void addNotification(Alarm alarm) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder notification;
        PendingIntent activity;
        Intent intent;

        Log.i(TAG, "AlarmNotification.addNotification(" + alarm.getId() + ", '" + alarm.getTitle() + "', '" + mDateTime.formatDetails(alarm) + "')");

        intent = new Intent(this.getApplicationContext(), MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

//        activity = PendingIntent.getActivity(this, (int) alarm.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "zzzlarry");
//        notification = builder
//                .setContentIntent(activity)
//                .setSmallIcon(R.drawable.ic_notification)
//                .setAutoCancel(true)
//                .setContentTitle("Missed alarm: " + alarm.getTitle())
//                .setContentText(mDateTime.formatDetails(alarm));
//
//        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
//        notificationManagerCompat.notify(123, notification.build());
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private class PlayTimerTask extends TimerTask {
        @Override
        public void run() {
            Log.i(TAG, "AlarmNotification.PalyTimerTask.run()");
            addNotification(mAlarm);
//            finish();
        }
    }

    private void doFileUpload(String date) throws FileNotFoundException {
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
                    String url = serverAddr + "/App_2nd/receive_file_finish.php?id=" + user_id;
                    client.post(this, url, params, new AsyncHttpResponseHandler() {
                        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                            Log.d("where", s + " 傳送成功");
                            String content = new String(responseBody);
                            Toast.makeText(AlarmNotification.this, "今日資料已上傳" + content, LENGTH_LONG).show();
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                            Log.d("where", s + " 傳送失敗");
                            String content = new String(responseBody);
                            Toast.makeText(AlarmNotification.this, "上傳失敗!" + content, LENGTH_LONG).show();
                        }

                    });
                }

            }
        } else {
            Log.d("where", "無此資料夾");
        }

    }


    private void GetDate() {
        Log.d(TAG, "GetDate()");
        AsyncHttpClient client = new AsyncHttpClient();
        String url = serverAddr + "/App_2nd/daily/data_makeup.php?id=" + user_id;
        client.get(url, new TextHttpResponseHandler() {
            @Override
            public void onStart() {
                // called before request is started
            }

            @Override
            public void onFailure(int i, Header[] headers, String s, Throwable throwable) {
                Log.d("where", String.valueOf(throwable));
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String response) {
                // called when response HTTP status is "200 OK"
                if (!"".equals(response)) {
                    String[] Date = response.split("<br>");
                    for (String s : Date) {
                        try {
                            doFileUpload(s);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    Toast.makeText(AlarmNotification.this, "資料已補全!", LENGTH_LONG).show();
                }
            }

            @Override
            public void onRetry(int retryNo) {
                // called when request is retried
            }
        });
    }
}
