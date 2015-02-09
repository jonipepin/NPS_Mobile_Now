/** AcademicCalendarMain 
 * 
 *  Author: 		Joni Pepin, jpepin@nps.edu
 *  Date:			22 Dec 2011
 *  Description: 	This activity accesses the public NPS site and retrieves the 
 *  				latest version of the academic calendar.  		
 *  
 *  Library Requirements:
 * 		JSoup - http://jsoup.org/download
 */

package com.pepinonline.mc;

import java.util.Timer;
import java.util.TimerTask;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.pepinonline.mc.R;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class AcademicCalendarMain extends SherlockActivity {

	private WebView cal;
	
//	private String CALENDAR_URL = "file:///android_asset/calendar/academic_calendar.html#current";
	private String CALENDAR_URL = "file:///android_asset/calendar/academic_calendar.html";


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.calendar);
		
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		cal = (WebView) findViewById(R.id.calendar);
		cal.setScrollbarFadingEnabled(true);
		cal.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		
		// need a slight delay so that the in-page link will load correctly
//		Timer timer = new Timer();
//		timer.schedule(new TimerTask() {
//		    @Override
//		    public void run() {
//		    	cal.loadUrl(CALENDAR_URL);
//		    }
//		}, 400);
				
		cal.loadUrl(CALENDAR_URL);
	}
	
	// ----------------- Event Handlers ----------------- //
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	  super.onConfigurationChanged(newConfig);
	  // do nothing on changes to orientation or slide-out keyboard
	  //Logger.i"Main configuration change detected");
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// app icon in action bar clicked; go home
            Intent intentHome = new Intent(this, MainActivity.class);
            intentHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intentHome);
	        break;
		}
		return true;
	}
			
}
