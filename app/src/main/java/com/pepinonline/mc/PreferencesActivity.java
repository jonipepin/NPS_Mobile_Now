/** McPreferences 
 * 
 *  Author: 		Joni Pepin, jpepin@nps.edu
 *  Date:			22 Dec 2011
 *  Description: 	Preference Activity for modifying all default
 *  				shared preferences.
 */

package com.pepinonline.mc;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.pepinonline.mc.muster.EmailChecker;
import com.pepinonline.mc.muster.Scheduler;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.preference.TimePreference;
import android.view.Gravity;
import android.widget.Toast;

public class PreferencesActivity extends SherlockPreferenceActivity implements
		OnSharedPreferenceChangeListener, OnPreferenceChangeListener {
	
	public static String[] emailFolderNames;
	PreferenceScreen ps;
	SharedPreferences sp;
	ListPreference emailPrefs;
	TimePreference scheduledTime;
	TimePreference snoozeTime;
	CheckBoxPreference schedEnabled;
	RingtonePreference ringtone;
	Preference updateEmailFolders;
	ProgressDialog progressDlg;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		addPreferencesFromResource(R.xml.preferences_activity);

		// initialize all Preferences
		initializePreferences(); 
		
		// configure email folder preferences
		configEmailFolderPref();
		configUpdateEmailFoldersPref();
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
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	  super.onConfigurationChanged(newConfig);
	  // do nothing on changes to orientation or slide-out keyboard
	  //Logger.i"Preferences configuration change detected");
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Preference pref = findPreference(key);
		if(pref == null) return;
		
		Logger.i("Shared Preference Changed: " + pref.getKey());
		if (pref instanceof EditTextPreference) {
			EditTextPreference etp = (EditTextPreference) pref;
			if (pref.getKey().equals("password")) {
				pref.setSummary(etp.getText().replaceAll(".", "*"));
			} else {
				pref.setSummary(etp.getText());
			}
		} else if (pref instanceof ListPreference) {
			ListPreference lp = (ListPreference) pref;
			pref.setSummary(lp.getValue());
		} else if (pref instanceof TimePreference) {
			if (pref == scheduledTime){
				updateScheduledTime();
			} else if (pref == snoozeTime) {
				updateSnoozeTimeDisplay();
			}
		} else if (pref instanceof CheckBoxPreference) {
			Scheduler s = new Scheduler(getBaseContext());
			if (!schedEnabled.isChecked()){
				s.cancelAlarm();
				Logger.i("Scheduled Check Disabled");
				Toast.makeText(this, "Scheduled Check Disabled",
						Toast.LENGTH_SHORT).show();
			} else {
				s.scheduleAlarmAtTime(scheduledTime.getHour(), scheduledTime.getMinute());
			}
		} else if (pref instanceof RingtonePreference) {
			Logger.i("Ringtone set: " + pref.getKey() + " | " + sp.getString("ringTone", "Default"));
		} 
	}

	@Override
	protected void onPause() {
//		getPreferenceScreen().getSharedPreferences()
//				.unregisterOnSharedPreferenceChangeListener(this);
//		Logger.i("PreferencesActivity onStop() called");
		sp.unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		finish();
	}

	@Override
	protected void onResume() {
		super.onResume();
//		getPreferenceScreen().getSharedPreferences()
//				.registerOnSharedPreferenceChangeListener(this);
		sp.registerOnSharedPreferenceChangeListener(this);
		
		// A patch to overcome OnSharedPreferenceChange not being called by RingtonePreference bug 
	    RingtonePreference pref = (RingtonePreference) findPreference("ringTone");
	    pref.setOnPreferenceChangeListener(this);
	    
	}
	
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
	    updateRingtoneSummary((RingtonePreference) preference, Uri.parse((String) newValue));
	    Logger.i("onPreferenceChange called");
	    return true;
	}
	
//	@Override
//	protected void onActivityResult(int requestCode, int resultCode, Intent data){
//		Logger.i("onActivityResult Fired");
//		super.onActivityResult(requestCode, resultCode, data);
//	}

	private void updateRingtoneSummary(RingtonePreference preference, Uri ringtoneUri) {
	    Ringtone ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
	    if (ringtone != null)
	        preference.setSummary(ringtone.getTitle(this));
	    else
	        preference.setSummary("Default");
	}

	private void initializePreferences() {
//		ps = getPreferenceScreen();
//		sp = ps.getSharedPreferences();
		sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
//		Map<String, ?> keys = sp.getAll();
//		Set<String> k = keys.keySet();
//		for (String s: k) {
//			Logger.i("Key name: " + s);
//		}
		
		Preference name = (Preference) findPreference("username");
		name.setSummary(sp.getString("username", "not set"));

		Preference password = (Preference) findPreference("password");
		password.setSummary(sp.getString("password", "").replaceAll(".", "*"));
		
		schedEnabled = (CheckBoxPreference) findPreference("schedulerEnabled");
		
		scheduledTime = (TimePreference) findPreference("scheduledTime");
		scheduledTime.setSummary(sp.getString("scheduledTime", "not set"));
		
		snoozeTime = (TimePreference) findPreference("snoozeTime");
		String snooze = sp.getString("snoozeTime", "disabled");
		if (!snooze.equals("00:00")) {
			snoozeTime.setSummary(snooze);
		}
		
		ringtone = (RingtonePreference) findPreference("ringTone");
		String tone = sp.getString("ringTone", "Default");
		updateRingtoneSummary(ringtone, Uri.parse((String) tone));
			
		emailPrefs = (ListPreference) findPreference("emailFolder");
		emailPrefs.setSummary(sp.getString("emailFolder", "none selected"));
		
		updateEmailFolders = (Preference) findPreference("updateEmailFolders");
	}
	
	private void updateScheduledTime() {
		scheduledTime.setSummary(scheduledTime.getTime());
		if (schedEnabled.isChecked()) {
			Scheduler s = new Scheduler(getBaseContext());
			s.scheduleAlarmAtTime(scheduledTime.getHour(), scheduledTime.getMinute());
		}
	}
	
	private void configEmailFolderPref() {
		if ((emailFolderNames = getEmailFolderNamesFromFile()) != null) {
			updateEmailFolderPref();
		} else {
			emailPrefs.setSummary("Update Folder List First");
		}
	}
	
	private void configUpdateEmailFoldersPref() {
		updateEmailFolders.setOnPreferenceClickListener(new OnPreferenceClickListener() {
		    public boolean onPreferenceClick(Preference preference) {
		    	new UpdateEmailFoldersTask().execute();
		    	return true;
		    }});
	}
	
	private void updateEmailFolderPref() {
		emailPrefs.setEntries(emailFolderNames);
    	emailPrefs.setEntryValues(emailFolderNames);
    	emailPrefs.setSelectable(true);
    	emailPrefs.setSummary(sp.getString("emailFolder", "none selected"));
	}
	
	private void updateSnoozeTimeDisplay() {
		int hour = snoozeTime.getHour();
		int minutes = snoozeTime.getMinute();
		String time = null;
		if (hour == 0) {
			time = Integer.toString(minutes) + " minutes";
		} else {
			time = Integer.toString(hour) + " hours, "+ Integer.toString(minutes) + " minutes";
		}
		if (hour + minutes == 0){
			snoozeTime.setSummary("No Recheck Time Set");
		} else {
			snoozeTime.setSummary("Recheck every " + time + " until mustered.");
		}
	}
	
	public String[] getEmailFolderNamesFromFile() {
    	
		String[] folders = null;
    	try {
    		
    		FileInputStream fis = openFileInput("EmailFolderNames");
    		ObjectInputStream ois = new ObjectInputStream(fis);
    		
    		folders = (String[]) ois.readObject();
    		
    		ois.close();
    		Logger.i("Email Folders retrieved from file");
    	} catch (Exception e) {
    		Logger.e("Exception getting folder list: " +  e.getMessage());
    		
    	}     	
    	return folders;
    }
	
	public void saveEmailFolderNamesFile(String[] folders) {
        if (folders != null){
			try {
		    	FileOutputStream fos = openFileOutput("EmailFolderNames", MODE_PRIVATE);
		    	ObjectOutputStream oos = new ObjectOutputStream(fos);
		    	
		    	oos.writeObject(folders); 
		    	oos.close();
		    	
		    	Logger.i("Email Folder List Saved");
		    	
	    	} catch (IOException e) {
	    		Logger.e(e.getMessage());
	    	}
        }
    }
	
	//------------------------ Sub-classes -----------------------------//
	
	/* AsyncTask subclass for updating email folders in the background.
	 * AsyncTask must be a subclass. 
	 * 
	 */
	private class UpdateEmailFoldersTask extends AsyncTask<Long, Integer, Integer> {
		
		private String user;
		private String pwd;
		private boolean successful = false;
		
	    protected Integer doInBackground(Long... params) {
	    	if (getLoginDetails()) {
		    	EmailChecker ec = new EmailChecker(getBaseContext(), user, pwd);
		     	if ((emailFolderNames = ec.getEmailFolderNames()) != null){
		 			saveEmailFolderNamesFile(emailFolderNames);	 
		 			successful = true;
		     	} 
	    	}
	        return 0;
	     }
	     
	     private boolean getLoginDetails() {
		    user = sp.getString("username", null);
	        pwd = sp.getString("password", null);
	        if (user == null || pwd == null){
	        	return false;
	        }
	        return true;
	     }

	     protected void onPreExecute() {
	    	 showProgressDialog();
	     }
	     
	     protected void onPostExecute(Integer result) {
	    	 progressDlg.cancel();
	    	 if (successful) {
	    		 updateEmailFolderPref();
	    		 Toast.makeText(PreferencesActivity.this, "Email Folder List Updated",
			    			Toast.LENGTH_SHORT).show();
	    	 } else {
		    	 Toast.makeText(PreferencesActivity.this, "Unable to retrieve email folders",
			    			Toast.LENGTH_LONG).show(); 
	    	 }
	     }
	     
	     public void showProgressDialog(){
	 		//progressDlg = ProgressDialog.show(this, "", "Please Wait...", true);
	 		// this progress dialog can be canceled:
	 		progressDlg = new ProgressDialog(PreferencesActivity.this);
	 		progressDlg.setMessage("Fetching Email Folder Names...");
	 		progressDlg.getWindow().setGravity(Gravity.BOTTOM);
	 		progressDlg.setCancelable(true);
	 		progressDlg.show();
	 	}
	 }

}
