/** AlarmReceiver 
 * 
 *  Author: 		Joni Pepin, jpepin@nps.edu
 *  Date:			22 Dec 2011
 *  Description: 	Broadcast receiver that runs MusterCheckIntentService
 *  				when the alarm is triggered.
 */

package com.pepinonline.mc.muster;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import com.pepinonline.mc.Logger;
import com.pepinonline.mc.LunchCheckIntentService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			Logger.i("Muster Alarm Triggered");

			// start the muster checker service
			Intent musterCheckIntent = new Intent(context,
					MusterCheckIntentService.class);
			context.startService(musterCheckIntent);
			
			// Check to see if lunch menu is up to date
			SharedPreferences mcPrefs = PreferenceManager.getDefaultSharedPreferences(context);
			String specialDate = mcPrefs.getString("special_date", null);
			String lunchSpecial = mcPrefs.getString("lunch_special", null);
			
			if (lunchSpecial == null || !isToday(specialDate)) {
				// may as well run the lunch checker service as well
				// if the lunch menu hasn't been retrieved
				Intent lunchCheckIntent = new Intent(context,
						LunchCheckIntentService.class);
				context.startService(lunchCheckIntent);
			}
			
		} catch (Exception e) {
			Logger.e("An alarm was received, but an error occurred");
			e.printStackTrace();
		}
	}

	private boolean isToday(String dateString) {
		Calendar today = Calendar.getInstance();
		try {
			Calendar testCal = convertStringToDate(dateString);
			if (today.get(Calendar.DATE) == testCal.get(Calendar.DATE)
					&& today.get(Calendar.MONTH) == testCal.get(Calendar.MONTH)
					&& today.get(Calendar.YEAR) == testCal.get(Calendar.YEAR)) {
				return true;
			}
		} catch (Exception e) {
			Logger.e("isToday() exception: " + e.toString());
		}
		
		return false;
	}
	
	private Calendar convertStringToDate(String dateString) {
		String pattern = "MMM dd, yyyy";
		Calendar c = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);

		try {
			Date date = sdf.parse(dateString);
			c.setTime(date);
		} catch (ParseException e) {
			Logger.e(e.getMessage());
		}
		return c;
	}
}