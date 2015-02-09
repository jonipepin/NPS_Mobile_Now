/** Scheduler 
 * 
 *  Author: 		Joni Pepin, jpepin@nps.edu
 *  Date:			22 Dec 2011
 *  Description: 	Class for managing scheduling of alarms. 
 */

package com.pepinonline.mc.muster;

import java.util.Arrays;
import java.util.Calendar;

import com.pepinonline.mc.Logger;
import com.pepinonline.mc.R;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class Scheduler {
	
	private Context context;
	public static final int PENDING_INTENT_REQUEST_CODE = 192837465;
	private int[] holidays;
	
	public Scheduler (Context context) {
		// Instantiate the Scheduler
		this.context = context;	
	}

	/**
	 * Schedule alarm for the input number of minutes in the future.
	 * 
	 * @param minutes
	 */
	public void scheduleAlarmInMinutes(int minutes) {
		if (minutes > 0) {
			// get a Calendar object with current time
			Calendar cal = Calendar.getInstance();
			// add input minutes to the calendar object
			cal.add(Calendar.MINUTE, minutes);
			
			setAlarm(cal);
			Logger.i("Alarm scheduled for " + minutes + " minutes from now ");
		}
	}
	
	/**
	 * Schedule alarm for next occurrence of the input hour and minute,
	 * skipping all weekends and holidays.
	 * 
	 * @param hour
	 * @param minute
	 */
	public void scheduleAlarmAtTime(int hour, int minute) {
		// get a calendar object with current time
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, minute);
		cal.set(Calendar.SECOND, 00);
		cal.set(Calendar.MILLISECOND, 000);
		if (hasAlreadyOccuredToday(cal) || isWeekend(cal)){
			cal.add(Calendar.DAY_OF_YEAR, daysUntilNextWeekday(cal));
		}
		
		// check to make sure the new day isn't a holiday
		getHolidays();
		while (dateIsHoliday(cal)) {
			// if it is a holiday, go to the next weekday and try again
			cal.add(Calendar.DAY_OF_YEAR, daysUntilNextWeekday(cal));
		}
		
		setAlarm(cal);
	}
	
	/**
	 * Determines if the input calendar time has already occurred.
	 * Checks to the millisecond.
	 * 
	 * @param cal
	 * @return true or false
	 */
	private boolean hasAlreadyOccuredToday(Calendar cal){
		//returns true if the input time/date is before the current time/date
		return cal.before(Calendar.getInstance());
	}
	
	private int daysUntilNextWeekday(Calendar today){
		// determine how many days until next weekday
		switch (today.get(Calendar.DAY_OF_WEEK)) {
			case 6:	return 3;
			case 7: return 2;
		}
		return 1;
	}
	
	private boolean isWeekend(Calendar today){
		// determine how many days until next weekday
		switch (today.get(Calendar.DAY_OF_WEEK)) {
			case 1:	
			case 7: return true;
		}
		return false;
	}
	
	private void getHolidays() {
		// retrieves sorted integer array from resources 
		// representing each holiday's day of year integer value
		// in the format YYDDD i.e. 12001 == 1 Jan 2012
		holidays = context.getResources().getIntArray(R.array.holidays);
	}
	
	private boolean dateIsHoliday(Calendar date) {
		int year = date.get(Calendar.YEAR);
		int doy = date.get(Calendar.DAY_OF_YEAR);
		int arrayDate = (year - 2000)*1000 + doy;
		Logger.i("Checking Date: " + arrayDate);
		// search sorted holidays integer array for a match to day of year value
		if (Arrays.binarySearch(holidays, arrayDate) >= 0) {
			Logger.i(arrayDate + " is a holiday");
			return true;
		}
		return false;
	}
	
	public void setAlarm(Calendar cal) {
		
		// create intent
		Intent intent = new Intent(context, AlarmReceiver.class);
		PendingIntent sender = PendingIntent.getBroadcast(context, PENDING_INTENT_REQUEST_CODE, 
				intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		// set alarm
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);
		Logger.i("Alarm scheduled for " + cal.getTime());
		
		// save in case of reboot
		saveNextAlarmDate(cal.getTimeInMillis());
	}
	
	public void cancelAlarm() {
		
		// create intent
		Intent intent = new Intent(context, AlarmReceiver.class);
		PendingIntent sender = PendingIntent.getBroadcast(context, PENDING_INTENT_REQUEST_CODE, 
				intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		// cancel alarm
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.cancel(sender);
		
		// set next alarm to none
		saveNextAlarmDate(0l);
	}
	
	private void saveNextAlarmDate(long nextAlarm) {
    	try {
    		Editor e = PreferenceManager.getDefaultSharedPreferences(context).edit();
	        e.putLong("nextAlarm", nextAlarm);
	    	e.commit();

        } catch (Exception e) {//Catch exception if any
        	Logger.e("Save nextMuster Exception: " + e.getMessage());
        }
    	
    }
}
