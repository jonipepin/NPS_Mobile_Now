/** BootReceiver 
 *
 *  Author: 		Joni Pepin, jpepin@nps.edu
 *  Date:			22 Dec 2011
 *  Description: 	Since all alarms are erased on device reboot, this broadcast 
 *  				receiver specifically detects reboot and reschedules the 
 *  				last alarm.
 */

package com.pepinonline.mc.muster;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import com.pepinonline.mc.Logger;

import java.util.Date;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            // create intent
            Intent mIntent = new Intent(context, AlarmReceiver.class);
            PendingIntent sender = PendingIntent.getBroadcast(context,
                    Scheduler.PENDING_INTENT_REQUEST_CODE,
                    mIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            // get next set alarm calendar date
            long nextAlarm = getNextAlarm(context);
            if (nextAlarm != 0l) {
                Date d = new Date();
                d.setTime(nextAlarm);

                // reset alarm
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                am.set(AlarmManager.RTC_WAKEUP, nextAlarm, sender);
//				Toast.makeText(context, "Next Muster Check:\n" + d.toString(), Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieve the last saved alarm time and date from the default shared preferences.
     *
     * @param context
     * @return
     */
    private long getNextAlarm(Context context) {
        try {
            return PreferenceManager.getDefaultSharedPreferences(context).getLong("nextAlarm", 0l);
        } catch (Exception e) {
            Logger.e(e.getMessage());
        }
        return 0l;
    }
}