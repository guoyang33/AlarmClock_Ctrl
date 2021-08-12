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
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.PowerManager;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Date dNow = new Date();
        @SuppressLint("SimpleDateFormat") SimpleDateFormat ft = new SimpleDateFormat("HH");
        String now_time = ft.format(dNow);
        Log.d("where", now_time);
        if ("03".equals(now_time)) {
            Intent newIntent = new Intent(context, MainActivity.class);
            Alarm alarm = new Alarm(context);
            alarm.fromIntent(intent);
            alarm.toIntent(newIntent);
            newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            String TAG = "AlarmMe";
            Log.i(TAG, "AlarmReceiver.onReceive('" + alarm.getTitle() + "')");
            context.startActivity(newIntent);
        } else {
            //daily notification
            Intent newIntent = new Intent(context, AlarmNotification.class);
            Alarm alarm = new Alarm(context);
            alarm.fromIntent(intent);
            alarm.toIntent(newIntent);
            newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            String TAG = "AlarmMe";
            Log.i(TAG, "AlarmReceiver.onReceive('" + alarm.getTitle() + "')");
            context.startActivity(newIntent);
        }
    }
}

