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

import cz.msebera.android.httpclient.Header;

import static android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS;
import static android.widget.Toast.LENGTH_LONG;


public class MainActivity extends AppCompatActivity {
    static final String user_id = "user1092250";//user001-user100非網癮
    static final String yearNo = "110"; // 計畫實施學年

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
        
        btn_rank = findViewById(R.id.button_rank);
        btn_checklack = findViewById(R.id.button_checklack);
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
        startAppUsageDetect();
        startCheckQuesLack();

        //檢查是否取得權限
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        //沒有權限時
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);
        } else {
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
                        dialog.setMessage("自控力是個人未來生涯成功指標，這套「健康上網自控APP」可訓練自控力，並透過逐步減量手機使用時間，預防過度3C使用對身心健康的危害，請善加利用。\n" +
                                "\n" +
                                "請依照APP手冊安裝與設定程式，並每日確認程式維持開啟狀態。訓練期共8週，追蹤期共16周，各週重點如下：\n" +
                                "\n" +
                                "第零週(Week 0)：程式會蒐集您的手機使用時間，目的是建立基礎值，於一週後提供統計結果給您。(請您按照正常習慣使用手機)\n\n" +
                                "第一週(Week 1)：請您先填寫每週自我評估單。再依順序參考前一週每日平均使用時間來設定目標（未來一週的日平均使用時間），目標請您設定減少3~10%。若每日達標可獲得10分點數，若連續7天達到減量目標，則共可獲得70分點數，另再獲得30分點數作為獎勵。每七天結算一次分數，若滿100分點數可另獲得獎勵拼圖卡一張。若未達標，雖未獲得點數，也請繼續努力，下週有機會可再補回點數（搶救100分）。\n\n" +
                                "第二週(Week 2)到第八週(Week 8)：請您先填寫每週自我評估單。再參考前一週是否達標來設定目標，達成者請繼續設定減量目標。若前一週目標未達成者，雖未獲得點數，您可重新調降減量目標（最低仍是3%），繼續努力，或是您可選擇搶救上週失分的機會，即本週除了繼續設定減量目標外，且本週還要多減少上週未達標而多用的時間，就可多贏得一張上週未得到的獎勵卡。\n\n" +
                                "第九周(Week 9)：訓練期結束，完成主題拼圖（集滿八張拼圖卡）則頒發「3C自控達人」獎狀一張。\n\n" +
                                "第5週、第9週、第13週、第25週：需填寫線上追蹤問卷。\n" +
                                "\n" +
                                "說明：\n" +
                                "1.\t各類APP使用時間的計算方式，是以上週的每日平均使用時間來計算，例如：上週每日平均使用時間8小時，設定減少10%，則本週每日平均使用時間為7.2小時。每週可重新設定目標一次。\n" +
                                "2.\t請每天抽空開啟APP確認「今日資料已上傳」，否則立即以Line聯絡研究人員。\n" +
                                "3.\t每週必須填寫自我評估單（線上回饋問卷心得單）。\n" +
                                "4.\t計畫結束時（第25週）須繳交服務學習心得報告才得以抵免服務學習時數。\n" +
                                "5.\t若您手機、手環等設備損壞、遺失或更換，請立即聯絡研究人員。\n" +
                                "6.\t若需協助可用Line回報或是聯絡 (04)2332-3456轉分機3606高助理或分機1021林助理。");
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
        Uri uri = Uri.parse("http://120.108.111.131/App_2nd/Ctrl_daily/redirect.php?id=" + user_id);
        Intent i = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(i);
    }

    public void rule(View v) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        dialog.setTitle("健康上網APP使用規則");
        dialog.setMessage("自控力是個人未來生涯成功指標，這套「健康上網自控APP」可訓練自控力，並透過逐步減量手機使用時間，預防過度3C使用對身心健康的危害，請善加利用。\n" +
                "\n" +
                "請依照APP手冊安裝與設定程式，並每日確認程式維持開啟狀態。訓練期共8週，追蹤期共16周，各週重點如下：\n" +
                "\n" +
                "第零週(Week 0)：程式會蒐集您的手機使用時間，目的是建立基礎值，於一週後提供統計結果給您。(請您按照正常習慣使用手機)\n\n" +
                "第一週(Week 1)：請您先填寫每週自我評估單。再依順序參考前一週每日平均使用時間來設定目標（未來一週的日平均使用時間），目標請您設定減少3~10%。若每日達標可獲得10分點數，若連續7天達到減量目標，則共可獲得70分點數，另再獲得30分點數作為獎勵。每七天結算一次分數，若滿100分點數可另獲得獎勵拼圖卡一張。若未達標，雖未獲得點數，也請繼續努力，下週有機會可再補回點數（搶救100分）。\n\n" +
                "第二週(Week 2)到第八週(Week 8)：請您先填寫每週自我評估單。再參考前一週是否達標來設定目標，達成者請繼續設定減量目標。若前一週目標未達成者，雖未獲得點數，您可重新調降減量目標（最低仍是3%），繼續努力，或是您可選擇搶救上週失分的機會，即本週除了繼續設定減量目標外，且本週還要多減少上週未達標而多用的時間，就可多贏得一張上週未得到的獎勵卡。\n\n" +
                "第九周(Week 9)：訓練期結束，完成主題拼圖（集滿八張拼圖卡）則頒發「3C自控達人」獎狀一張。\n\n" +
                "第5週、第9週、第13週、第25週：需填寫線上追蹤問卷。\n" +
                "\n" +
                "說明：\n" +
                "1.\t各類APP使用時間的計算方式，是以上週的每日平均使用時間來計算，例如：上週每日平均使用時間8小時，設定減少10%，則本週每日平均使用時間為7.2小時。每週可重新設定目標一次。\n" +
                "2.\t請每天抽空開啟APP確認「今日資料已上傳」，否則立即以Line聯絡研究人員。\n" +
                "3.\t每週必須填寫自我評估單（線上回饋問卷心得單）。\n" +
                "4.\t計畫結束時（第25週）須繳交服務學習心得報告才得以抵免服務學習時數。\n" +
                "5.\t若您手機、手環等設備損壞、遺失或更換，請立即聯絡研究人員。\n" +
                "6.\t若需協助可用Line回報或是聯絡 (04)2332-3456轉分機3606高助理或分機1021林助理。");
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
                    client.post(this, "http://120.108.111.131/App_2nd/receive_file_finish.php?id=" + user_id, params, new AsyncHttpResponseHandler() {
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
        client.get("http://120.108.111.131/App_2nd/Data_MakeUp.php?id=" + user_id + "&yearno=" + yearNo, new TextHttpResponseHandler() {
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
                    Toast.makeText(MainActivity.this, "資料已無任何缺失!", LENGTH_LONG).show();
                }
            }

            @Override
            public void onRetry(int retryNo) {
                // called when request is retried
            }
        });
    }

    public void checklack(View v) {
        Uri uri = Uri.parse("http://120.108.111.131/App_2nd/Data_MakeUp1.php?id=" + user_id);
        Intent i = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(i);
    }

    public void alltime(View v) {
        Intent activityIntent = new Intent();
        activityIntent.setComponent(new ComponentName("com.a0soft.gphone.uninstaller", "com.a0soft.gphone.uninstaller.wnd.MainWnd"));
        startActivity(activityIntent);
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

