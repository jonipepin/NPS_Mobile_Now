/** Notifier 
 *
 *  Author: 		Joni Pepin, jpepin@nps.edu
 *  Date:			22 Dec 2011
 *  Description: 	Class for managing device notifications. 
 */

package com.pepinonline.mc.muster;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.pepinonline.mc.MainActivity;
import com.pepinonline.mc.R;

public class Notifier {

    private final int USER_NOTIFIER_ID = 1;
    private Context context;
    private Notification notification;
    private NotificationManager mNotificationManager;

    Notifier(Context context) {
        // Instantiate the Notifier
        int icon = R.drawable.icon;
        CharSequence tickerText = "Muster";
        long when = System.currentTimeMillis();

        this.context = context;
        this.notification = new Notification(icon, tickerText, when);
    }

    public void getNotificationManagerReference() {
        // Get a reference to the NotificationManager
        String ns = Context.NOTIFICATION_SERVICE;
        this.mNotificationManager = (NotificationManager) context.getSystemService(ns);
    }

    public void setNotificationProperties(CharSequence notificationText) {
        // Define the notification's message and
        // PendingIntent (what activity is called when notification is clicked)
        CharSequence contentTitle = "Muster Reminder";
        CharSequence contentText = notificationText;
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
        // cancel notification when selected
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        // add sound and vibration to notification

        SharedPreferences mcPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String tone = mcPrefs.getString("ringTone", null);
        if (tone != null) {
            notification.sound = Uri.parse(tone);
        } else {
            notification.defaults |= Notification.DEFAULT_SOUND;
        }

        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.defaults |= Notification.DEFAULT_LIGHTS;
    }

    public void passNotificationToManager() {
        // Pass the Notification to the NotificationManager
        mNotificationManager.notify(USER_NOTIFIER_ID, notification);
    }

}
