package com.zzzlarry.reminder;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class AppUsageLauncher extends Activity {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent activityIntent = new Intent();
        activityIntent.setComponent(new ComponentName("com.a0soft.gphone.uninstaller", "com.a0soft.gphone.uninstaller.wnd.MainWnd"));
        startActivity(activityIntent);
    }

}
