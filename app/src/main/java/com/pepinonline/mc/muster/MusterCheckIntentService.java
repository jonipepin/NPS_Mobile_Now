/** MusterCheckIntentService 
 *
 *  Author: 		Joni Pepin, jpepin@nps.edu
 *  Date:			22 Dec 2011
 *  Description: 	This service checks to see if the specified user has 
 *  				mustered for the day.  If on the NPS network, it checks
 *  				the student check-in page.  If not, it checks the user's
 *  				email folders to see if a muster confirmation email was 
 *  				received for the day.  IntentService is used so that it 
 *  				runs on its own thread.  		
 *
 *  Reference:
 * 		http://developer.android.com/guide/topics/fundamentals/services.html
 */

package com.pepinonline.mc.muster;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.pepinonline.mc.Logger;
import com.pepinonline.mc.Util;

import java.util.Date;

public class MusterCheckIntentService extends IntentService {

    public static SharedPreferences mcPrefs;
    public static Date musterDate;
    public static String savedMusterDate;
    public static int mustered;
    public static String verifiedBy;
    public static int scheduledHour;
    public static int scheduledMin;

    public MusterCheckIntentService() {
        super("UserNotifierIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Logger.i("Starting Muster Check Service");

        // get shared preferences
        mcPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // get info from most recent saved check
        getMusterInfo();

        // check to see if called manually
        boolean startedManually = intent.getBooleanExtra("manual", false);

        // get muster status
        getMusterStatus(startedManually);

        // check to see if the user has enabled the scheduler
        boolean schedulerEnabled = mcPrefs.getBoolean("schedulerEnabled", false);
        Scheduler s = new Scheduler(getBaseContext());
        boolean nextAlarmSet = false;

        if (mustered != 0 && !startedManually) {
            // notify user if not mustered and not checked manually
            sendUserAlert();
            // snooze for the user-set amount of time
            nextAlarmSet = setSnooze();
        }

        if (!nextAlarmSet && schedulerEnabled && timeScheduled()) {
            // schedule for the next day if scheduler enabled, and
            // a scheduled time has been set.
            s.scheduleAlarmAtTime(scheduledHour, scheduledMin);
        }

        // save the results of the check
        saveMusterInfo();

        // if started manually, notify the Main UI that the
        // service has finished getting the muster status.
//		if (startedManually){
        broadcastCompletion();
//		}

        // stop the thread
        Logger.i("Stopping Muster Check Service");
        stopSelf();
    }

    /**
     * Get muster status for the user.
     *
     * @param manual Was the service started manually?
     */
    private void getMusterStatus(boolean manual) {
        if (manual) {
            /* if called manually, just check status;
			 no notification or scheduled future check */
            Logger.i("Service started manually");
            mustered = checkMuster();
        } else {
			/* if started automatically, check if already 
			 confirmed for the day.  If so, there's no need 
			 to check again */
            Logger.i("Service started automatically");
            if (!alreadyMusteredToday()) {
                mustered = checkMuster();
                Logger.i("Not already mustered today");
            }
        }
    }

    /**
     * Checks to see if the user has mustered using either the student
     * check-in page, or the users email folders, depending on whether
     * or not the device is on the NPS network.
     *
     * @return
     */
    private int checkMuster() {
        try {
            String username = mcPrefs.getString("username", null);
            String password = mcPrefs.getString("password", null);
            if (Util.onNpsNetwork()) {
                WebChecker wc = new WebChecker(username, password);
                int i = wc.musterCheck();
                musterDate = wc.musterDate;
                verifiedBy = "Website";
                return i;
            } else {
                String folder = mcPrefs.getString("emailFolder", null);
                EmailChecker ec = new EmailChecker(getBaseContext(), username, password);
                int j = ec.musterCheck(folder);
                musterDate = ec.musterDate;
                verifiedBy = "Email";
                return j;
            }
        } catch (Exception e) {
            Logger.e(e.toString());
            return 3;
        }
    }

    /**
     * Create notification in the device's status bar.
     */
    private void sendUserAlert() {
        Notifier n = new Notifier(this);
        n.getNotificationManagerReference();
        CharSequence message;
        switch (mustered) {
            case 1:
                message = "Don't forget to Muster !!";
                break;
            case 2:
                message = "Unable to check muster status.  Please verify user info";
                break;
            default:
                message = "Muster status unknown";
                break;
        }
        n.setNotificationProperties(message);
        n.passNotificationToManager();
    }

    /**
     * Save the muster check results to default shared preferences.
     */
    private void saveMusterInfo() {
        try {
            String date = new Date().toString();
            Editor e = mcPrefs.edit();
            e.putString("lastchecked", date);
            e.putInt("musterStatus", mustered);
            if (musterDate != null) {
                e.putString("mustertime", musterDate.toString());
            }
            e.commit();
            Logger.i("Saved Muster Info");
        } catch (Exception e) {//Catch exception if any
            Logger.e("Save Muster Info Exception: " + e.getMessage());
        }

    }

    /**
     * Get muster info, for use in determining if the service needs to run again.
     */
    private void getMusterInfo() {
        try {
            savedMusterDate = mcPrefs.getString("mustertime", "Time Not Saved");
            mustered = mcPrefs.getInt("musterStatus", 3);
            musterDate = Util.convertStringToDate(savedMusterDate);
        } catch (Exception e) {//Catch exception if any
            Logger.e("Get Muster Info Exception: " + e.getMessage());
        }
    }

    /**
     * Send broadcast that the service is done.
     */
    private void broadcastCompletion() {
        Intent broadcastIntent = new Intent();
        broadcastIntent.putExtra("verifyMethod", verifiedBy);
        broadcastIntent.setAction("MUSTER_CHECK_COMPLETE");
        sendBroadcast(broadcastIntent);
    }

    /**
     * Check to see if there is already a saved verification of mustering
     *
     * @return
     */
    private boolean alreadyMusteredToday() {
        return (mustered == 0 && Util.dateIsToday(musterDate));
    }

    /**
     * Checks to see if there is a scheduled alarm time already set.
     *
     * @return true if set, false if not
     */
    private boolean timeScheduled() {
        String time = mcPrefs.getString("scheduledTime", null);
        if (time != null) {
            String[] pieces = time.split(":");
            scheduledHour = Integer.parseInt(pieces[0]);
            scheduledMin = Integer.parseInt(pieces[1]);
            return true;
        }
        return false;
    }

    /**
     * Retrieve the saved length of time to snooze.
     *
     * @return Integer value representing number of minutes.
     */
    private int getSnoozeTime() {
        String time = mcPrefs.getString("snoozeTime", null);
        if (time != null) {
            String[] pieces = time.split(":");
            int hours = Integer.parseInt(pieces[0]);
            int minutes = Integer.parseInt(pieces[1]);
            return hours * 60 + minutes;
        }
        return 0;
    }

    /**
     * Schedule the alarm to run after the set number of minutes.
     *
     * @return
     */
    private boolean setSnooze() {
        int snoozetime = getSnoozeTime();
        if (snoozetime != 0) {
            Scheduler s = new Scheduler(getBaseContext());
            s.scheduleAlarmInMinutes(snoozetime);
            return true;
        }
        return false;
    }
}
