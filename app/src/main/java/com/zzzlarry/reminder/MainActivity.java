package com.zzzlarry.reminder;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.content.Context;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Calendar;
import java.util.TimeZone;

import cz.msebera.android.httpclient.Header;

import static android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS;
import static android.widget.Toast.LENGTH_LONG;


public class MainActivity extends AppCompatActivity {
    static final String serverAddr = "http://120.108.111.131";
    static final String userId = "A1102299"; // 編號規則：{Ios / Android} {year} {1:成癮 | 2:非成癮} {目標 / 信息} {流水號}

    private String introduction;

    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    Button btn_rank;
    Button btn_checklack;
    Button btn_alltime;
    private final String TAG = "AlarmMe";
    public static String alarmNotiChannelId = "notiChannelAlarm";
    public static String silenceNotiChannelId = "notiChannelSilence";
    public ListView mAlarmList;
    private AlarmListAdapter mAlarmListAdapter;
    private Alarm mCurrentAlarm;
    public final int NEW_ALARM_ACTIVITY = 0;
    public final int EDIT_ALARM_ACTIVITY = 1;
    public final static int PREFERENCES_ACTIVITY = 2;
    private final int CONTEXT_MENU_EDIT = 0;
    private final int CONTEXT_MENU_DELETE = 1;
    private ActionMode mActionMode;
    ImageView imageView;
    private SQLiteDatabase db;
    private DBHelper DH = null;

    public class DBHelper extends SQLiteOpenHelper {
        private static final String DB_NAME = "my.db";
        private static final int DB_VERSION = 1;
        private final static String _TableName = "Dialog_Show";

        public DBHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            final String SQL = "CREATE TABLE IF NOT EXISTS " + _TableName + "(" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "_TITLE VARCHAR(50)," +
                    "_SHOW INTEGER)";
            db.execSQL(SQL);

            String insertsql = "INSERT INTO `Dialog_Show` (`_TITLE`,`_SHOW`) VALUES ('dialog',1)";
            db.execSQL(insertsql);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            final String SQL = "DROP TABLE " + _TableName;
            db.execSQL(SQL);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);

        createNotificationChannel();

        Log.d(TAG, "ctrl type: " + userId.substring(5, 6).equals("1"));
        if (userId.substring(5, 6).equals("1")) {
            introduction = "自控力是個人未來生涯成功指標,這套「健康上網APP」可訓練自控力,並透過逐步減量手機使用時間,預防過度3C使用對身心健康的危害,請善加利用。請依照APP手冊安裝與設定程式,並每日確認程式維持開啟狀態。訓練期共8週,追蹤期共16周,各週重點如下:\n" +
                    "\n" +
                    "第0週 預備週（Week 0）\n" +
                    "1.「健康上網APP」會蒐集您的每日手機使用時間的基礎值，於1週後提供四類APP每日平均使用時間給您參考。\n" +
                    "2.本週暫時不會回饋您的手機使用時間，請您按照正常習慣使用手機。\n" +
                    "3.第一週蒐集資料目的在於建立基礎值，讓您知道您的每日平均手機使用時間，也作為您未來手機使用時間的參照基準。\n" +
                    "4.填寫線上數位探索調查問卷。\n" +
                    "\n" +
                    "第1週 訓練期開始（Week 1）\n" +
                    "1.系統會自動呈現第一週每日平均使用時間及與社群同儕比較的個人排名（名次是由使用時間少到多排列）。\n" +
                    "2.請您參考第一週的每日平均使用時間設定目標（設定未來一週的日平均使用時間），請逐一照5類APP使用時間設定目標，請您設定減少3~10%的使用時間。設定一個小目標或小計畫，有助於您達成目標並獲得成功經驗，更有動力往前邁進。\n" +
                    "3.若每日達成設定之目標可獲得10分點數，若連續7天達到減量目標，則可獲得70分點數，另會再獲得30分點數作為獎勵。\n" +
                    "4.若每日目標未達成者，雖未獲得點數，也請您繼續努力，下週有機會可再補回點數。\n" +
                    "5.每7天會結算一次分數，若獲得滿100分則給予獎勵拼圖卡一張。\n" +
                    "6.請您填寫每週線上回饋單，填寫完成同樣可獲得獎勵分數和拼圖卡。\n" +
                    "7.請您每天抽空開啟APP確認跳出「今日資料已上傳」訊息，並確認每天的排名與使用時間，否則以Line聯絡研究人員。\n" +
                    "\n" +
                    "第2週到第7週 訓練期（Week 2 to week 7 ）\n" +
                    "1.請參考前一週您是否達標來設定本周目標，達成者請繼續設定減量目標。\n" +
                    "2.若前一週目標未達成者，雖未獲得點數，您可再次設定目標，使用時間較前一週減少3~10%，繼續努力。\n" +
                    "3.計分方式如同前一週，除了達成每日目標，若前一週未達標，本週再減少使用前一週未達成的時間可補領獎勵點數。\n" +
                    "4.每7天結算一次分數，每獲得滿100分可另獲得獎勵拼圖卡一張。\n" +
                    "5.同上週所列第6點和第7點。\n" +
                    "\n" +
                    "第8週 訓練期結束 (Week 8)\n" +
                    "1.訓練的最後一週，重複上週程序。\n" +
                    "2.當訓練結束時，系統會呈現總結畫面，回饋您的使用時間（總時間及日平均）、總獎勵積分、個人排名、進步程度（日平均與基礎值相比）、每週蒐集到的拼圖卡所完成的主題拼圖。\n" +
                    "3.完成主題拼圖（集滿八張拼圖卡）則頒發「3C自控達人」獎狀一張。\n" +
                    "4.請您填寫每週線上回饋單。\n" +
                    "5.請您每天抽空開啟APP確認跳出「今日資料已上傳」訊息，否則以Line聯絡研究人員。\n" +
                    "\n" +
                    "第9週到第24週 追蹤期 (Week 9 to week 24)\n" +
                    "1.8週的自控訓練已結束，接下來系統不會再有目標設定與獎勵分數，請您將這8週的自控策略應用到未來的生活中，研究計畫仍會持續追蹤4個月，APP仍會呈現每日平均使用時間及與社群同儕比較的個人排名。\n" +
                    "2.同上週所列第4點和第5點。\n" +
                    "3.第9週(Week 9)、第13週(Week 13)、第25週(Week 25) ：需填寫線上數位探索調查問卷。\n" +
                    "4.計畫完成前須繳交服務學習心得報告，在滿足其他必須條件後，符合並領取AI計畫研究型服務學習時數30小時證明書。\n" +
                    "\n" +
                    "若您手機、手環等設備損壞、遺失或更換，請立即聯絡研究人員。\n" +
                    "若需協助可用Line回報或是聯絡 (04)2332-3456轉分機3606高助理或分機1021。";
        } else {
            introduction = "自控力是個人未來生涯成功指標,這套「健康上網APP」可訓練自控力,並透過逐步減量手機使用時間,預防過度3C使用對身心健康的危害,請善加利用。請依照APP手冊安裝與設定程式,並每日確認程式維持開啟狀態。訓練期共8週,追蹤期共16周,各週重點如下:\n" +
                    "\n" +
                    "第0週 預備週（Week 0）\n" +
                    "1.「健康上網APP」會蒐集您的每日手機使用時間的基礎值，於1週後提供四類APP每日平均使用時間給您參考。\n" +
                    "2.本週暫時不會回饋您的手機使用時間，請您按照正常習慣使用手機。\n" +
                    "3.第一週蒐集資料目的在於建立基礎值，讓您知道您的每日平均手機使用時間，也作為您未來手機使用時間的參照基準。\n" +
                    "4.填寫線上數位探索調查問卷。\n" +
                    "\n" +
                    "第1週 訓練期（Week 1）\n" +
                    "1.系統會自動呈現第一週每日平均使用時間及與社群同儕比較的個人排名（名次是由使用時間少到多排列）。\n" +
                    "2.請您參考第一週的每日平均使用時間，經由APP了解您的每日手機使用時間，並進行健康上網使用規劃。\n" +
                    "3.請您填寫每週線上回饋單，填寫完成可獲得獎勵分數和拼圖卡。\n" +
                    "4.請您每天抽空開啟APP確認跳出「今日資料已上傳」訊息，並確認每天的排名與使用時間，否則以Line聯絡研究人員。\n" +
                    "\n" +
                    "第2週 訓練期（Week 2）\n" +
                    "1.請您參考前一週的每日平均使用時間、前一天的使用時間及與社群同儕比較的個人排名，經由APP了解您的每日手機使用時間，並進行健康上網使用規劃。\n" +
                    "2.請您填寫每週線上回饋單，填寫完成可獲得獎勵分數和拼圖卡。\n" +
                    "3.請您每天抽空開啟APP確認跳出「今日資料已上傳」訊息，並確認每天的排名與使用時間，否則以Line聯絡研究人員。\n" +
                    "\n" +
                    "第3週到第7週 訓練期 (Week 3 to week 7)\n" +
                    "1.重複上週程序。\n" +
                    "\n" +
                    "第8週 訓練期 (Week 8)\n" +
                    "1.訓練的最後一週，重複上週程序。\n" +
                    "2.當訓練結束時，系統會呈現總結畫面，回饋您的使用時間（總時間及日平均）、個人排名、進步程度（日平均與基礎值相比）。\n" +
                    "3.請您填寫每週線上回饋單。\n" +
                    "4.請您每天抽空開啟APP確認跳出「今日資料已上傳」訊息，並確認每天的排名與使用時間，否則以Line聯絡研究人員。\n" +
                    "\n" +
                    "第9週到第24週 追蹤期 (Week 9 to week 24)\n" +
                    "1.8週的訓練已結束，接下來持續使用APP，研究計畫仍會持續追蹤4個月，APP仍會呈現每日平均使用時間及與社群同儕比較的個人排名。\n" +
                    "2.請您填寫每週線上回饋單。\n" +
                    "3.第9週(Week 9)、第13週(Week 13)、第25週(Week 25) ：需填寫線上數位探索調查問卷。\n" +
                    "4.計畫完成前須繳交服務學習心得報告，在滿足其他必須條件後，符合並領取AI計畫研究型服務學習時數30小時證明書。\n" +
                    "\n" +
                    "若您手機、手環等設備損壞、遺失或更換，請立即聯絡研究人員。\n" +
                    "若需協助可用Line回報或是聯絡 (04)2332-3456轉分機3606高助理或分機1021。";
        }
        
        btn_rank = findViewById(R.id.button_rank);
        btn_alltime = findViewById(R.id.button_alltime);
        mAlarmList = (ListView) findViewById(R.id.alarm_list);
        mAlarmListAdapter = new AlarmListAdapter(this);
        mAlarmList.setAdapter(mAlarmListAdapter);
        // mAlarmList.setOnItemClickListener(mListOnItemClickListener);
        registerForContextMenu(mAlarmList);
        mCurrentAlarm = null;
        //mAlarmList.setMultiChoiceModeListener(AbsListView.CHOICE_MODE_MULTIPLE);
        imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setOnLongClickListener(new View.OnLongClickListener() {
            // Called when the user long-clicks on someView
            public boolean onLongClick(View view) {
                if (mActionMode != null) {
                    return false;
                }

                // Start the CAB (Context Action Bar) using the ActionMode.Callback defined above
                mActionMode = MainActivity.this.startActionMode(mActionModeCallback);
                view.setSelected(true);
                return true;
            }
        });
        mAlarmList.setOnLongClickListener(new View.OnLongClickListener() {
            // Called when the user long-clicks on someView
            public boolean onLongClick(View view) {
                if (mActionMode != null) {
                    return false;
                }

                // Start the CAB (Context Action Bar) using the ActionMode.Callback defined above
                mActionMode = MainActivity.this.startActionMode(mActionModeCallback);
                //  view.setSelected(true);
                return true;
            }
        });

        // 監聽通知權限
        String string = Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners");
        if (!string.contains(NotificationCollectorService.class.getName())) {
            startActivity(new Intent(
                    "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
        }

        //檢查是否取得權限
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        //沒有權限時
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);
        } else {
            startAppUsageUploader();
            startAppUsageDetect();
            startCheckQuesLack();
            GetDate();
        }
        DH = new DBHelper(this);
        db = DH.getWritableDatabase();
        db = openOrCreateDatabase("my.db", Context.MODE_PRIVATE, null);
        Cursor cursor = db.rawQuery("SELECT * FROM `Dialog_Show` WHERE 1", null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    int bool = cursor.getInt(cursor.getColumnIndex("_SHOW"));
                    String title = cursor.getString(cursor.getColumnIndex("_TITLE"));
                    if (bool == 1) {
                        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                        dialog.setTitle("健康上網APP使用規則");
                        dialog.setMessage(introduction);
                        dialog.setNeutralButton("我已了解不再顯示", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                // TODO Auto-generated method stub
                                Toast.makeText(MainActivity.this, "不再顯示", Toast.LENGTH_SHORT).show();
                                db.execSQL("Update `Dialog_Show` set `_SHOW` = 0 where `_TITLE` = 'dialog'");
                            }

                        });
                        dialog.setPositiveButton("關閉", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                // TODO Auto-generated method stub
                            }

                        });
                        dialog.show();
                    }
                } while (cursor.moveToNext());
            }
        }
        assert cursor != null;
        cursor.close();

    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            CharSequence name  = "zzzlarryReminderChannel";
            String description = "Channel For Alarm Manager";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(alarmNotiChannelId, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            CharSequence name2 = "silenceNotiChannel";
            String description2 = "Channel for silence notification";
            int importance2 = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel2 = new NotificationChannel(silenceNotiChannelId, name2, importance2);
            channel.setDescription(description);

            NotificationManager notificationManager2 = getSystemService(NotificationManager.class);
            notificationManager2.createNotificationChannel(channel2);
        }
    }

    public void rank(View v) {
        Uri uri = Uri.parse(serverAddr + "/App_2nd/daily/redirect.php?id=" + userId);
        Intent i = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(i);
    }

    public void rule(View v) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        dialog.setTitle("健康上網APP使用規則");
        dialog.setMessage(introduction);
        dialog.setPositiveButton("關閉", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                // TODO Auto-generated method stub
            }

        });
        dialog.show();
    }

    // 上傳csv至伺服器，每日匯出的csv儲存在/storage/emulated/0/AppUsage/export/daily/<日期>/<檔名>.csv
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
                    String url = serverAddr + "/App_2nd/receive_file_finish.php?id=" + userId;
                    client.post(this, url, params, new AsyncHttpResponseHandler() {
                        @RequiresApi(api = Build.VERSION_CODES.P)
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                            Log.d("where", s + " 傳送成功");
                            String content = new String(responseBody);
                            Toast.makeText(MainActivity.this, "今日資料已上傳" + content, LENGTH_LONG).show();
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                            Log.d("where", s + " 傳送失敗");
                            String content = new String(responseBody);
                            Toast.makeText(MainActivity.this, "上傳失敗!" + content, LENGTH_LONG).show();
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
        String url = serverAddr + "/App_2nd/daily/data_makeup.php?id=" + userId;
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
                Log.d("getDate()", "status:" + statusCode);
                Log.d("getDate()", response);
                if (!"".equals(response)) {
                    String[] Date = response.split("<br>");
                    for (String s : Date) {
                        try {
                            Log.d(TAG, "doFileUpload " + s);
                            doFileUpload(s);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    Toast.makeText(MainActivity.this, "資料已無任何缺失!", LENGTH_LONG).show();
                }
            }

            @Override
            public void onRetry(int retryNo) {
                // called when request is retried
            }
        });
    }

    public void alltime(View v) {
        Intent activityIntent = new Intent();
        activityIntent.setComponent(new ComponentName("com.a0soft.gphone.uninstaller", "com.a0soft.gphone.uninstaller.wnd.MainWnd"));
        startActivity(activityIntent);
    }

    private void startAppUsageUploader() {
        Log.d(TAG, "call startAppUsageUploader()");
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) + 3);
//        calendar.set(Calendar.HOUR, calendar.get(Calendar.HOUR) + 12);
        Log.d("startAppUsageUploader()", "time: " + calendar.getTimeInMillis());
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AppUsageUploaderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
//        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 12*60*60*1000, pendingIntent); // 12Hours
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 3*60*1000, pendingIntent);
    }

    private void startAppUsageDetect() {
        Log.d(TAG, "call startAppUsageDetect()");
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) + 5);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AppUsageDetectReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 60000, pendingIntent);
    }

    public void startCheckQuesLack() {
        Log.d(TAG, "call startCheckQuesLack()");
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) + 10);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, CheckQuesLackReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 60000, pendingIntent); // 測試、間隔待調
//        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 3600000, pendingIntent);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "AlarmMe.onResume()");
        mAlarmListAdapter.updateAlarms();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "AlarmMe.onDestroy()");
    }

    public void onAddAlarmClick(View view) {
        Intent intent = new Intent(getBaseContext(), Editt.class);

        mCurrentAlarm = new Alarm(this);
        mCurrentAlarm.toIntent(intent);

        MainActivity.this.startActivityForResult(intent, NEW_ALARM_ACTIVITY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == NEW_ALARM_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                mCurrentAlarm.fromIntent(data);
                mAlarmListAdapter.add(mCurrentAlarm);
            }
            mCurrentAlarm = null;
        } else if (requestCode == EDIT_ALARM_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                mCurrentAlarm.fromIntent(data);
                mAlarmListAdapter.update(mCurrentAlarm);
            }
            mCurrentAlarm = null;
        } else if (requestCode == PREFERENCES_ACTIVITY) {
            mAlarmListAdapter.onSettingsUpdated();

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //  MenuInflater inflater = getMenuInflater();

        //inflater.inflate(R.menu.menu, menu);
        getMenuInflater().inflate(R.menu.menu_new, menu);
        // menu.findItem(R.id.menu_item_save).setVisible(true);
        // menu.findItem(R.id.menu_item_delete).setVisible(true);
        return super.onCreateOptionsMenu(menu);

    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_new:
                // Toast.makeText(this, "HELLO", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(getBaseContext(), Editt.class);

                mCurrentAlarm = new Alarm(this);
                mCurrentAlarm.toIntent(intent);

                MainActivity.this.startActivityForResult(intent, NEW_ALARM_ACTIVITY);
                return true;
            case R.id.menu_about:
                Intent intent1 = new Intent(getBaseContext(), About.class);
                startActivity(intent1);
                return true;
            case R.id.menu_settings:
                Intent intent2 = new Intent(getBaseContext(), Preferences.class);
                startActivityForResult(intent2, PREFERENCES_ACTIVITY);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.alarm_list) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

            menu.setHeaderTitle(mAlarmListAdapter.getItem(info.position).getTitle());
            menu.add(Menu.NONE, CONTEXT_MENU_EDIT, Menu.NONE, "Modify");
            menu.add(Menu.NONE, CONTEXT_MENU_DELETE, Menu.NONE, "Remove");

        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int index = item.getItemId();

        if (index == CONTEXT_MENU_EDIT) {
            Intent intent = new Intent(getBaseContext(), Editt.class);

            mCurrentAlarm = mAlarmListAdapter.getItem(info.position);
            mCurrentAlarm.toIntent(intent);
            startActivityForResult(intent, EDIT_ALARM_ACTIVITY);
        } else if (index == CONTEXT_MENU_DELETE) {
            mAlarmListAdapter.delete(info.position);
        }


        return true;
    }


    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            actionMode.setTitle("Selected");
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.context_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            mActionMode = null;
        }
    };

}

