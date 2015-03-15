/** Main 
 *
 *  Author: 		Joni Pepin, jpepin@nps.edu
 *  Date:			22 Dec 2011
 *  Description: 	This activity provides the main user interface for 
 *  				the NPS Muster Checker application.  		
 *
 *  Library Requirements for Application:
 * 		Javamail - http://www.oracle.com/technetwork/java/index-138643.html
 * 		JSoup - http://jsoup.org/download
 *
 *  Changes:
 *    v4: 0.5.0 - added ability to check Club Del Monte Lunch specials when not connected to the NPS wireless network.  
 *    v5: 0.6.0 - fixed bug in LunchMain getLinkToSpecial(); changed selectText search value
 *    				from "a:contains(Club Del Monte Lunch Specials)" to "a:contains(Club Del Monte Lunch)"
 *    			- added color-coded backgrounds capability
 *    			- added main_v2 layout and set as default
 *    			- commented out musterPage button
 *    v6: 0.6.1 - changed events activity to pull items from MWR page
 *    			- created EventRetriever class
 *    			- changed format of news_item layout to add subtitle section
 *    			- updated Article class to add subtitle field
 *    			- updated ArticleAdapter to display Article subtitle if not null
 *    v7: 0.7.0 - complete decor overhaul with new theme 
 *    			- all new dashboard interface
 *    			- incorporated student handbook and DKL mobile
 *    v8: 0.7.1 - changed dashboard icon colors from yellow to silver
 *    v9: 0.7.2 - fixed library webview mailto: and tel: link actions
 *    			- disabled library webview page caching 
 *    			- fixed bug that caused icon to not display properly on animation stop
 *    			- changed news and events activities to only run automatically if not updated
 *    			  in the last 8 hours.
 *    v10 0.8.0 - Changed holiday array format to denote year in addition to day of year
 *    			  and updated Scheduler.dateIsHoliday() method accordingly.
 *    			- Set academic calendar to automatically go to Summer 2012 by moving
 *    			  "current" anchor in academic_calendar_old.html.
 *    			- Changed events source back to NPS b/c of MWR page changes
 *    v11 1.0.0 - All new Card interface
 *    			- fetch MWR events 
 *    			- set notification ring-tone
 *    v12 1.0.1 - Fixed calendar activity webview not loading webpage for some users
 *    			  due to known bug with in-page anchors by removing anchor and just 
 *    			  truncating HTML page to have current semester on top
 *    v13 1.0.2 - Updated academic calendar through Fall 2015 quarter
 *    			- Hid library computer status display (availableComputersBlock)
 *    			  b/c dkl is not maintaining it.
 *    			- Updated Library URL
 *    v14 1.1.0 - Fixed bug causing events to not display chronologically.
 *    v15 1.2.0 - Updated academic calendar through Jan 2016
 *              - Modified MainActivity layout so News and Events cards contain LinearLayout
 *                that has clickable TextViews for each item.  Click goes to proper web page.
 *              - Disabled WebChecker until I can get a version of the updated page; uses the
 *                email checker by default now.
 *    v16 1.2.1 - Updated library hours.
 *
 */

package com.pepinonline.mc;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.pepinonline.mc.muster.MusterCheckIntentService;
import com.pepinonline.mc.news.Article;
import com.pepinonline.mc.news.Article.Type;
import com.pepinonline.mc.news.ArticleRetriever;
import com.pepinonline.mc.news.EventsMain;
import com.pepinonline.mc.news.NewsMain;
import com.pepinonline.mc.news.NpsEventRetriever;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class MainActivity extends SherlockActivity implements OnClickListener,
        DialogInterface.OnClickListener {

    // comparator for sorting articles
    static final Comparator<Article> EVENT_ORDER = new Comparator<Article>() {
        public int compare(Article a1, Article a2) {
            if (a1.getDate() == null) {
                Calendar c = Calendar.getInstance();
                c.set(Calendar.YEAR, 2020);
                a1.setDate(c);
            }
            if (a2.getDate() == null) {
                Calendar c = Calendar.getInstance();
                c.set(Calendar.YEAR, 2020);
                a2.setDate(c);
            }
            return a1.getDate().compareTo(a2.getDate());
        }
    };
    public static SharedPreferences mcPrefs;
    public static String lastChecked;
    public static String verifiedBy;
    public static String savedMusterDate;
    public static String localUrl = "https://intranet.nps.edu/studentmuster/studentpages/studentcheckin.aspx";
    public static String[] emailFolderNames;
    public static int mustered = 3;
    public static int selectedItem = -1;
    public static boolean userInfoSet = false;
    private static boolean appIsActive = false;
    private static int currentVersion;
    private static String sUsername;
    private static String sPassword;
    private static EditText usernameInput, pwdInput;
    private static AlertDialog userInfoDlg, updateDialog;
    private static CheckBox showPwdCheckBox, ignoreUpdateCheckbox;
    private static ImageView npsIcon, lunchIcon, newsIcon, eventsIcon;
    // private static Button musterPageButton;
    private static TextView lastMusterCheckText, nextMusterCheckText,
            musterStatusText, checkMusterAgainText;
    public final long EIGHT_HOURS = 28800000; // 8 hours in milliseconds
    private final String MUSTER_ACTION_COMPLETE = "MUSTER_CHECK_COMPLETE";
    private final String LUNCH_SPECIAL_ACTION_COMPLETE = "LUNCH_SPECIAL_CHECK_COMPLETE";
    private final String NEWS_ACTION_COMPLETE = "NEWS_CHECK_COMPLETE";
    private final String EVENTS_ACTION_COMPLETE = "EVENTS_CHECK_COMPLETE";
    
    /**
     * Broadcast receiver to receive completion broadcast from
     * MusterCheckIntentService, if it has been started manually.
     */
    private BroadcastReceiver intentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MUSTER_ACTION_COMPLETE)) {
                verifiedBy = intent.getStringExtra("verifyMethod");
                postMusterCheck();
            } else if (action.equals(LUNCH_SPECIAL_ACTION_COMPLETE)) {
                // progressDlg.cancel();
                getSavedLunchSpecial();
                Util.stopSpinningIcon(lunchIcon);
                // if (lunchSpecial == null) {
                // Toast.makeText(MainActivity.this, "Please Try Again",
                // Toast.LENGTH_SHORT).show();
                // } else {
                updateLunchCardDisplay();
                // }
            } else if (action.equals(NEWS_ACTION_COMPLETE)) {
                getNewsArticlesFromFile();
                Util.stopSpinningIcon(newsIcon);
            } else if (action.equals(EVENTS_ACTION_COMPLETE)) {
                getEventsFromFile();
                Util.stopSpinningIcon(eventsIcon);
            }
        }
    };
    private final String NEWS_ITEM_FILE = "NPS_News_Articles";
    private final String EVENTS_ITEM_FILE = "NPS_Events";
    private final String APPURL = "https://play.google.com/store/apps/details?id=com.pepinonline.mc";
    
    // private static LinearLayout mainView;
    TextView checkMusterAgain;
    boolean musterCheckInProgress = false;
    private IntentFilter intentFilter;
    // Lunch Card variables
    private LinearLayout lunchCard;
    private TextView specialDateView, specialMenuView;
    private String lunchSpecial;
    private String specialDate;
    private Calendar today;
    // News Card
    private TextView moreNews;
    // Events Card
    private TextView moreEvents;
    // Library
    private LinearLayout availableComputersBlock;
    private TextView libraryStatus, librarySubStatus, availableComputers, unavailableComputers, publicComputers;
    private boolean libraryIsOpen = false;
    // Calendar Card
    private LinearLayout calendarCard, mapCard;
    // Contact Card
    private LinearLayout contactCard;

    /**
     * Retrieves the saved value of the next scheduled alarm.
     *
     * @return String value of next alarm date and time.
     */
    private static String getNextAlarmDateString() {
        // get next alarm date info as string
        String date = "None Scheduled";
        long nextDate = mcPrefs.getLong("nextAlarm", 0l);
        if (nextDate != 0l) {
            Date d = new Date();
            d.setTime(nextDate);
            date = d.toString();
        }
        return date;
    }

    // ***************** PRIVATE METHODS ******************//

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        createActivity();
        appIsActive = true;
    }

    @Override
    public void onStart() {
        super.onStart();
        startActivity();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // unregister the broadcast receiver
        unregisterReceiver(intentReceiver);
        appIsActive = false;
    }

    /**
     * Actions to be performed when onCreate() is called. Separated from
     * onCreate() so that the actions in this method can also be called by
     * onConfigurationChanged.
     */
    private void createActivity() {

        setupViews();

        setupListeners();

        // what day is today?
        today = Calendar.getInstance();

        PackageInfo pInfo;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            currentVersion = pInfo.versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        // get shared preferences
        mcPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // intent to filter for muster check service completion
        intentFilter = new IntentFilter();
        intentFilter.addAction(MUSTER_ACTION_COMPLETE);
        intentFilter.addAction(LUNCH_SPECIAL_ACTION_COMPLETE); // added by Joni
        intentFilter.addAction(NEWS_ACTION_COMPLETE);
        intentFilter.addAction(EVENTS_ACTION_COMPLETE);

        // register the broadcast receiver
        registerReceiver(intentReceiver, intentFilter);

        // get updated news articles if necessary
        if (!newsUpdatedRecently()) {
            Logger.i("News NOT updated recently");
            runNewsService();
        }

        // get updated events if necessary
        if (!eventsUpdatedRecently()) {
            Logger.i("Events NOT updated recently");
            runEventsService();
        }

        // Lunch Card
        int dayOfWeek = today.get(Calendar.DAY_OF_WEEK);
        // if (getConfigInfo() && dayOfWeek != Calendar.SATURDAY
        // && dayOfWeek != Calendar.SUNDAY) {
        if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {
            getSavedLunchSpecial();
            if (lunchSpecial != null && isToday(specialDate)) {
                updateLunchCardDisplay();
            } else {
                runLunchSpecialService();
            }
        }

        // Check for app updates
        int ignore = mcPrefs.getInt("ignoreUpdate", -1);
        int lastUpdateCheck = mcPrefs.getInt("lastUpdateCheck", -1);
        if (ignore < currentVersion
                && lastUpdateCheck != today.get(Calendar.DAY_OF_YEAR)) {
            new CheckForAppUpdate().execute();
        }
    }

    /**
     * Actions to be performed when onStart() is called. Like createActivity(),
     * this method was separated from onStart() so that the actions in this
     * method can also be called by onConfigurationChanged. Checking if on NPS
     * network is done here as opposed to in onCreate() so that if the Main
     * activity is in the background when the user connects to the NPS network,
     * the proper UI changes are made when the activity is brought to the
     * foreground.
     */
    private void startActivity() {

        // get saved info from most recent check
        getMusterInfo();

        // set text displayed in UI
        setMusterCardText();

        // show/hide lunch card
        lunchCard = (LinearLayout) findViewById(R.id.lunchCard);
        int dayOfWeek = today.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            lunchCard.setVisibility(View.GONE);
        } else {
            lunchCard.setVisibility(View.VISIBLE);
        }

        // get saved user info and prompt for their input if non-existent
        if (!getConfigInfo()) {
            userInfoSet = false;
            // Toast.makeText(this, "Press menu button to enter NPS user info",
            // Toast.LENGTH_SHORT).show();
            checkMusterAgainText.setText("Set NPS User Details");
        } else {
            userInfoSet = true;
            // lunchCard.setVisibility(View.VISIBLE);
            checkMusterAgainText.setText("Check Now");
        }

        // get news items
        getNewsArticlesFromFile();

        // get event items
        getEventsFromFile();
    }

    /**
     * Initialize all Views used in the Main UI.
     */
    private void setupViews() {
        // Muster Card Views
        musterStatusText = (TextView) findViewById(R.id.musterStatusText);
        lastMusterCheckText = (TextView) findViewById(R.id.lastMusterCheckText);
        nextMusterCheckText = (TextView) findViewById(R.id.nextMusterCheckText);
        checkMusterAgainText = (TextView) findViewById(R.id.checkAgainTextView);
        npsIcon = (ImageView) findViewById(R.id.npsIcon);

        // Lunch Card Views
        lunchCard = (LinearLayout) findViewById(R.id.lunchCard);
        lunchIcon = (ImageView) findViewById(R.id.lunchIcon);
        specialDateView = (TextView) findViewById(R.id.lunch_special_date);
        specialMenuView = (TextView) findViewById(R.id.lunch_special_menu);

        // News Card Views
        newsIcon = (ImageView) findViewById(R.id.newsIcon);
        moreNews = (TextView) findViewById(R.id.moreNews);

        // Events Card Views
        eventsIcon = (ImageView) findViewById(R.id.eventsIcon);
        moreEvents = (TextView) findViewById(R.id.moreEvents);

        // Library Available Terminal Card
        libraryStatus = (TextView) findViewById(R.id.libraryStatus);

        librarySubStatus = (TextView) findViewById(R.id.librarySubStatus);
        availableComputersBlock = (LinearLayout) findViewById(R.id.libraryComputerStatusView);
        availableComputers = (TextView) findViewById(R.id.libraryAvailableComputers);
        unavailableComputers = (TextView) findViewById(R.id.libraryUnavailableComputers);
        publicComputers = (TextView) findViewById(R.id.libraryPublicComputers);

        // calendar card
        calendarCard = (LinearLayout) findViewById(R.id.calendarCard);
        mapCard = (LinearLayout) findViewById(R.id.mapCard);

        // contact developer card
        contactCard = (LinearLayout) findViewById(R.id.contactCard);

        Util.setFontRobotoRegular(this, musterStatusText, lastMusterCheckText, nextMusterCheckText,
                specialMenuView, moreNews, librarySubStatus,
                checkMusterAgainText, moreEvents);

        Util.setFontRobotoMedium(this, libraryStatus, specialDateView);

        Util.setFontRobotoLight(this, libraryStatus, availableComputers, unavailableComputers, publicComputers);

        setLibraryStatus();
        /* stop checking available computer status and 
		   hide the available computers block because dkl
		   isn't maintaining it  any more */
        //getAvailableComputers();
        availableComputersBlock.setVisibility(View.GONE);

    }

    /**
     * Create all required listeners for the UI.
     */
    private void setupListeners() {
        // musterPageButton.setOnClickListener(this);
        npsIcon.setOnClickListener(this);
        lunchIcon.setOnClickListener(this);
        checkMusterAgainText.setOnClickListener(this);
        moreNews.setOnClickListener(this);
        moreEvents.setOnClickListener(this);
        libraryStatus.setOnClickListener(this);
        availableComputersBlock.setOnClickListener(this);
        calendarCard.setOnClickListener(this);
        mapCard.setOnClickListener(this);
        newsIcon.setOnClickListener(this);
        eventsIcon.setOnClickListener(this);
        contactCard.setOnClickListener(this);
    }

    /**
     * After manually checking muster status, update the UI to reflect the new
     * saved results.
     */
    private void postMusterCheck() {
        getMusterInfo();
        musterCheckInProgress = false;
        Util.stopSpinningIcon(npsIcon);

        checkMusterAgainText.setText("Check Again");

        lastMusterCheckText.setText(removeDateExtra(lastChecked));
        nextMusterCheckText.setText(removeDateExtra(getNextAlarmDateString()));
        // if (verifiedBy != null){
        // lastMusterCheckText.append("\nVerification Method: " + verifiedBy);
        // }
        switch (mustered) {
            case 0:
                if (savedMusterDate != "Time Not Saved") {
                    // remove all text after the time
                    musterStatusText.setText("Mustered: "
                            + removeDateExtra(savedMusterDate));
                } else {
                    musterStatusText.setText("Mustered: time unknown.");
                }
                musterStatusText.setTextColor(Color.GREEN);
                // mainView.setBackgroundResource(R.drawable.gradient_bg_green);
                break;
            case 1:
                musterStatusText.setText("Not Mustered");
                musterStatusText.setTextColor(Color.RED);
                // mainView.setBackgroundResource(R.drawable.gradient_bg_red);
                break;
            case 2:
                musterStatusText.setText("Unknown. Verify user info.");
                musterStatusText.setTextColor(Color.RED);
                Toast.makeText(this, "User Info Incorrect", Toast.LENGTH_SHORT)
                        .show();
                setupUserInfoDialog();
                userInfoDlg.show();
                break;
            case 3:
                musterStatusText.setText("Unknown");
                musterStatusText.setTextColor(Color.RED);
                Toast.makeText(this,
                        "Unable to check muster status. Please try again.",
                        Toast.LENGTH_LONG).show();
                break;
        }
    }

    private String removeDateExtra(String date) {
        if (date == null) {
            return null;
        }
        return date.replaceFirst("([0-9]{2}:[0-9]{2}:[0-9]{2}).*", "$1");
    }

    /**
     * Sets initial values of TextViews in Main UI based on last saved results.
     */
    private void setMusterCardText() {
        lastMusterCheckText.setText(removeDateExtra(lastChecked));
        nextMusterCheckText.setText(removeDateExtra(getNextAlarmDateString()));

        switch (mustered) {
            case 0:
                Date tempDate = Util.convertStringToDate(savedMusterDate);
                if (tempDate != null && Util.dateIsToday(tempDate)) {
                    musterStatusText.setText("Mustered: "
                            + removeDateExtra(savedMusterDate));
                    musterStatusText.setTextColor(Color.GREEN);
                    // mainView.setBackgroundResource(R.drawable.gradient_bg_green);
                } else {
                    musterStatusText.setText("Not Mustered");
                    musterStatusText.setTextColor(Color.RED);
                    // mainView.setBackgroundResource(R.drawable.gradient_bg_red);
                }
                break;
            case 1:
                musterStatusText.setText("Not Mustered");
                musterStatusText.setTextColor(Color.RED);
                // mainView.setBackgroundResource(R.drawable.gradient_bg_red);
                break;
            case 2:
                musterStatusText.setText("Please verify user info");
                musterStatusText.setTextColor(Color.RED);
                // mainView.setBackgroundResource(R.drawable.gradient_bg_yellow);
                break;
            case 3:
                musterStatusText.setText("Muster status unknown");
                musterStatusText.setTextColor(Color.RED);
                // mainView.setBackgroundResource(R.drawable.gradient_bg_yellow);
                break;
        }
    }

    /**
     * Create dialog for input of User information.
     */
    private void setupUserInfoDialog() {

        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = LayoutInflater.from(this);

        View layout = inflater.inflate(R.layout.input, null);
        usernameInput = (EditText) layout.findViewById(R.id.usernameInput);
        pwdInput = (EditText) layout.findViewById(R.id.pwdInput);
        showPwdCheckBox = (CheckBox) layout.findViewById(R.id.checkBox1);

        showPwdCheckBox.setOnClickListener(this);
        usernameInput.setText(sUsername);

        // hide the "Show Password" option if the password
        // is already set. This is a security caution.
        if (sPassword != null) {
            usernameInput.setText(sUsername);
            pwdInput.setText(sPassword);
            showPwdCheckBox.setVisibility(View.GONE);
        }

        builder.setView(layout);
        builder.setTitle("Edit NPS User Info").setPositiveButton("Save", this)
                .setNegativeButton("Cancel", this);

        userInfoDlg = builder.create();
    }

    /**
     * Create dialog for input of User information.
     */
    private void setupUpdateDialog(String comments) {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = LayoutInflater.from(this);

        View layout = inflater.inflate(R.layout.update_dialog, null);
        TextView commentsView = (TextView) layout
                .findViewById(R.id.updateComments);
        ignoreUpdateCheckbox = (CheckBox) layout
                .findViewById(R.id.ignoreCheckBox);

        ignoreUpdateCheckbox.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Editor e = mcPrefs.edit();
                if (ignoreUpdateCheckbox.isChecked()) {
                    e.putInt("ignoreUpdate", currentVersion);
                } else {
                    e.putInt("ignoreUpdate", -1);
                }
                e.commit();
            }

        });
        commentsView.setText(comments);

        builder.setView(layout);
        builder.setTitle("Update Available")
                .setPositiveButton("Get Update", this)
                .setNegativeButton("Cancel", this);

        updateDialog = builder.create();
    }

    /**
     * Retrieve the saved user name and password from default shared
     * preferences.
     *
     * @return true if successfully retrieved, false if not
     */
    private boolean getConfigInfo() {

        try {
            sUsername = mcPrefs.getString("username", null);
            sPassword = mcPrefs.getString("password", null);
            if (sUsername == null || sPassword == null
                    || sUsername.length() == 0 || sPassword.length() == 0) {
                return false;
            }

            // set userInfoSet flag to true if successful
            userInfoSet = true;
            return true;

        } catch (Exception e) {// Catch exception if any
            Logger.e(e.getMessage());
            return false;
        }
    }

    /**
     * Save the user settings to the default shared preferences, and change
     * muster status to unknown.
     */
    private void saveConfigInfo() {
        try {
            Editor e = mcPrefs.edit();
            e.putString("username", sUsername);
            e.putString("password", sPassword);
            e.commit();
            mustered = 3; // set muster status to unknown
            musterStatusText.setText("Muster status unknown");
            Toast.makeText(this, "User Info Set", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {// Catch exception if any
            Logger.e("Save Config Exception: " + e.getMessage());
        }

    }

    // *************** Event Handlers *************************//

    /**
     * Retrieve the last saved muster information from the default shared
     * preferences.
     */
    private void getMusterInfo() {
        try {
            lastChecked = mcPrefs.getString("lastchecked", "Never!");
            savedMusterDate = mcPrefs.getString("mustertime", "Time Not Saved");
            mustered = mcPrefs.getInt("musterStatus", 3);
        } catch (Exception e) {// Catch exception if any
            Logger.e("Get Muster Info Exception: " + e.getMessage());
        }

    }

    /**
     * Change the layout file in use if the device changes orientation, and run
     * all actions that were completed in onCreate() and onStart() so that the
     * UI has full functionality.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Checks the orientation of the screen
        // if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        // setContentView(R.layout.muster_landscape);
        // } else if (newConfig.orientation ==
        // Configuration.ORIENTATION_PORTRAIT){
        // setContentView(R.layout.muster_portrait);
        // }
        createActivity();
        startActivity();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        SubMenu subMenu1 = menu.addSubMenu("Options");
        MenuInflater menuInflater = getSupportMenuInflater();
        menuInflater.inflate(R.menu.options_menu, subMenu1);

        MenuItem subMenu1Item = subMenu1.getItem();
        subMenu1Item.setIcon(R.drawable.abs__ic_menu_moreoverflow_holo_light);
        subMenu1Item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuItem_settings:
                startActivity(new Intent(this, PreferencesActivity.class));
                break;
            case R.id.menuItem_share:
                // ref:
                // http://blog.kwyps.com/2011/04/android-sharing-text-to-facebook.html

                // String text = Util
                // .CreateShortenedUrl("https://play.google.com/store/apps/details?id=com.pepinonline.mc");
                String text = "https://play.google.com/store/apps/details?id=com.pepinonline.mc";
                if (text.equals("ERROR")) {
                    Toast.makeText(this, "Network Error, Please Try Again.",
                            Toast.LENGTH_SHORT).show();
                } else {
                    text += "\nCheck out the NPS Mobile App!";
                    Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                    sharingIntent.setType("text/plain");
                    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, text);
                    sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                            "NPS Mobile android app");
                    startActivity(Intent
                            .createChooser(sharingIntent, "Share using"));

                }
                break;
            case R.id.menuItem_feedback:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(APPURL));
                startActivity(intent);
                break;
        }
        return true;
    }

    @Override
    public void onClick(View v) {

        if (v == npsIcon || v == checkMusterAgainText) {
            if (musterCheckInProgress) {
                return;
            }
            if (userInfoSet) {
                musterCheckInProgress = true;

                // rotate nps icon
                Util.startSpinningIcon(this, npsIcon);
                checkMusterAgainText.setText("Checking Muster Status");
                // checkMusterAgainText.setTextColor(Color.YELLOW);

                // start the service to check muster
                Intent mcIntent = new Intent(this,
                        MusterCheckIntentService.class);
                // flag to tell service that it's being called manually
                mcIntent.putExtra("manual", true);
                startService(mcIntent);

            } else {
                // if user info is not set, prompt for it instead of
                // launching the service.
                setupUserInfoDialog();
                userInfoDlg.show();
                Toast.makeText(this, "Please set NPS User info first.",
                        Toast.LENGTH_SHORT).show();
            }
        } else if (v == lunchIcon) {
            runLunchSpecialService();
        } else if (v == newsIcon) {
            runNewsService();
        } else if (v == eventsIcon) {
            runEventsService();
        } else if (v == moreNews) {
            Intent n = new Intent(this, NewsMain.class);
            startActivity(n);
        } else if (v == moreEvents) {
            Intent e = new Intent(this, EventsMain.class);
            startActivity(e);
        } else if (v == libraryStatus) {
            Uri uri = Uri.parse("http://libanswers.nps.edu/mobile.php");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } else if (v == availableComputersBlock) {
            Uri uri = Uri.parse("http://mdkl.nps.edu/computers/");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } else if (v == showPwdCheckBox) {
            if (showPwdCheckBox.isChecked()) {
                pwdInput.setTransformationMethod(null);
            } else {
                pwdInput.setTransformationMethod(PasswordTransformationMethod
                        .getInstance());
            }
        } else if (v == calendarCard) {
            Intent n = new Intent(this, AcademicCalendarMain.class);
            startActivity(n);
        } else if (v == mapCard) {
            Intent n = new Intent(this, CampusMapMain.class);
            startActivity(n);
        } else if (v == contactCard) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri data = Uri
                    .parse("mailto:npsmobile@pepinonline.com?subject=NPS Mobile App Support");
            intent.setData(data);
            startActivity(intent);
        }
    }

    // --------------- START LUNCH CARD METHODS ----------------//

    @Override
    public void onClick(DialogInterface dialog, int itemId) {

        if (dialog == userInfoDlg) {
            if (itemId == Dialog.BUTTON_POSITIVE) {
                if (usernameInput.getText().toString().trim().length() < 1
                        || pwdInput.getText().toString().trim().length() < 1) {
                    Toast.makeText(this, "Try Again", Toast.LENGTH_SHORT)
                            .show();
                    setupUserInfoDialog();
                    userInfoDlg.show();
                } else {
                    sUsername = usernameInput.getText().toString().trim();
                    sPassword = pwdInput.getText().toString().trim();
                    saveConfigInfo();
                    userInfoSet = true;
                    checkMusterAgainText.setText("Check Now");
                    Logger.i("user info set");
                }
            } else if (itemId == Dialog.BUTTON_NEGATIVE) {
                Toast.makeText(this, "Action Canceled", Toast.LENGTH_LONG)
                        .show();
                if (!userInfoSet) {
                    userInfoSet = false;
                    checkMusterAgainText.setText("Set User Info");
                }
            }
        }
        if (dialog == updateDialog) {
            if (itemId == Dialog.BUTTON_POSITIVE) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(APPURL));
                startActivity(intent);
            } else if (itemId == Dialog.BUTTON_NEGATIVE) {
                Logger.i("update dialog concelled");
            }
        }

    }

    private void runLunchSpecialService() {
        Util.startSpinningIcon(this, lunchIcon);
        // start the service to check for lunch specials
        Intent lunchIntent = new Intent(this, LunchCheckIntentService.class);
        // flag to tell service that it's being called manually
        lunchIntent.putExtra("manual", true);
        startService(lunchIntent);
    }

    private void getSavedLunchSpecial() {
        mcPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        specialDate = mcPrefs.getString("special_date", null);
        lunchSpecial = mcPrefs.getString("lunch_special", null);
    }

    private void updateLunchCardDisplay() {
        String monthString = getMonthString(today.get(Calendar.MONTH));
        specialDateView.setText("Lunch Specials for "
                + today.get(Calendar.DAY_OF_MONTH) + " " + monthString);
        try {
            if (isToday(specialDate)) {
                specialMenuView.setText(Html.fromHtml(lunchSpecial));
            } else {
                specialMenuView
                        .setText("Today's specials are not available right now.\nPlease try again later");
            }
        } catch (Exception e) {
            Logger.e("updateLunchCardDisplay() exception");
        }
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

    private boolean isToday(String dateString) {
        try {
            Calendar testCal = convertStringToDate(dateString);
            if (today.get(Calendar.DATE) == testCal.get(Calendar.DATE)
                    && today.get(Calendar.MONTH) == testCal.get(Calendar.MONTH)
                    && today.get(Calendar.YEAR) == testCal.get(Calendar.YEAR)) {
                return true;
            }
        } catch (Exception e) {
            // e.printStackTrace();
            Logger.e("isToday() exception: " + e.toString());
        }

        return false;
    }

    // --------- START LIBRARY CARD METHODS --------------//

    private String getMonthString(int monthNumber) {
        switch (monthNumber) {
            case 0:
                return "January";
            case 1:
                return "February";
            case 2:
                return "March";
            case 3:
                return "April";
            case 4:
                return "May";
            case 5:
                return "June";
            case 6:
                return "July";
            case 7:
                return "August";
            case 8:
                return "September";
            case 9:
                return "October";
            case 10:
                return "November";
            case 11:
                return "December";
            default:
                return null;
        }
    }

    public void setLibraryStatus() {
        String statusChangeString = "";

        Calendar calTZ = new GregorianCalendar(
                TimeZone.getTimeZone("America/Los_Angeles"));
        calTZ.setTimeInMillis(new Date().getTime());

        Calendar now = Calendar.getInstance();
        now.set(Calendar.DAY_OF_WEEK, calTZ.get(Calendar.DAY_OF_WEEK));
        now.set(Calendar.HOUR_OF_DAY, calTZ.get(Calendar.HOUR_OF_DAY));
        now.set(Calendar.MINUTE, calTZ.get(Calendar.MINUTE));

        int day = now.get(Calendar.DAY_OF_WEEK);
        int hour = now.get(Calendar.HOUR_OF_DAY);
        // Logger.i("Day: " + day + " | Hour PST: " + hour);
        // Hours - Mon-Thurs: 0700-2200 Fri: 0700-1700 Sat: 0900-1700 Sun:
        // 1200-2000
        switch (day) {
            case 1:
                if (hour > 11 && hour < 20) {
                    libraryIsOpen = true;
                    statusChangeString = "The library will close today at 2000 PST";
                } else if (hour < 12) {
                    statusChangeString = "The library will open today at 1200 PST";
                } else {
                    statusChangeString = "The library will open tomorrow at 0700 PST";
                    ;
                }
                break;
            case 2:
            case 3:
            case 4:
            case 5:
                if (hour > 6 && hour < 20) {
                    libraryIsOpen = true;
                    statusChangeString = "The library will close today at 2000 PST";
                } else if (hour < 7) {
                    statusChangeString = "The library will open today at 0700 PST";
                } else {
                    statusChangeString = "The library will open tomorrow at 0700 PST";
                    ;
                }
                break;
            case 6:
                if (hour > 6 && hour < 17) {
                    libraryIsOpen = true;
                    statusChangeString = "The library will close today at 1700 PST";
                } else if (hour < 7) {
                    statusChangeString = "The library will open today at 0700 PST";
                } else {
                    statusChangeString = "The library will open tomorrow at 0900 PST";
                    ;
                }
                break;
            case 7:
                if (hour > 8 && hour < 17) {
                    libraryIsOpen = true;
                    statusChangeString = "The library will close today at 1700 PST";
                } else if (hour < 9) {
                    statusChangeString = "The library will open today at 0900 PST";
                } else {
                    statusChangeString = "The library will open tomorrow at 1200 PST";
                    ;
                }
                break;
        }

        if (libraryIsOpen) {
            libraryStatus.setText("OPEN");
            libraryStatus.setTextColor(Color.GREEN);
        } else {
            libraryStatus.setText("CLOSED");
            libraryStatus.setTextColor(Color.RED);
        }
        librarySubStatus.setText(statusChangeString);
    }

    private void getAvailableComputers() {
        final Timer timer = new Timer();
        final Handler handler = new Handler();
        timer.schedule(new TimerTask() {
            public void run() {
                // get song info via AsyncTask in it's own thread
                handler.post(new Runnable() {
                    public void run() {
                        // Logger.i("Retrieving Song Metadata");
                        if (appIsActive) {
                            // may as well get and set the library status here
                            setLibraryStatus();
                            // if the library is open, check to see which
                            // computers are available
                            // if not, hide that part of the card
                            if (libraryIsOpen) {
                                availableComputersBlock
                                        .setVisibility(View.VISIBLE);
                                new UpdateAvailableComputersTask().execute();
                            } else {
                                availableComputersBlock
                                        .setVisibility(View.GONE);
                            }
                        } else {
                            timer.cancel();
                        }

                    }
                });
            }
        }, 0, 60000); // check every 1 minute

    }

    // ------------------------ Sub-classes -----------------------------//

    private String retrieveWebpage(String url) throws ClientProtocolException,
            IOException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(url);
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = httpClient.execute(httpget, responseHandler);
        // Logger.i("Response Body " + responseBody);
        httpClient.getConnectionManager().shutdown();
        // Logger.i("Connection closed.");
        return responseBody;
    }

    // ------------- NEWS CARD METHODS ------------------//

    protected boolean newsUpdatedRecently() {
        long lastUpdated = mcPrefs.getLong("newsUpdated", 0l);
        if (lastUpdated == 0L) {
            return false;
        }
        Date d = new Date();
        return (d.getTime() - lastUpdated) < EIGHT_HOURS;
    }

    private void runNewsService() {
        Util.startSpinningIcon(this, newsIcon);
        // start the service to check for news articles
        Intent lunchIntent = new Intent(this, NewsCheckIntentService.class);
        startService(lunchIntent);
    }

    // get news items
    private void getNewsArticlesFromFile() {
        
        LinearLayout newsArea = (LinearLayout) findViewById(R.id.recentNewsLayout);
        newsArea.removeAllViews();
        
        int marginPixels = dpToPixel(10);
        
        ArticleRetriever retriever = new ArticleRetriever(this);
        List<Article> newsList = retriever.getArticlesFromFile(NEWS_ITEM_FILE);
        if (newsList != null && !newsList.isEmpty()) {
            int index = 0;

            while (index < 3 && index < newsList.size()) {
                final String link = newsList.get(index).getLink();

                TextView item = new TextView(this);
                //item.setTextColor(Color.parseColor("#707070"));
                Util.setFontRobotoRegular(this, item);
                item.setPadding(marginPixels, marginPixels, marginPixels, marginPixels);
                item.setText(newsList.get(index).getHeadline());
                item.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, 
                                Uri.parse(link));
                        startActivity(browserIntent);
                    }
                });
                newsArea.addView(item);
                
                index++;
            }
        }
    }
    
    // converts number of pixels for the given dp value on this device
    private int dpToPixel(int sizeInDp){
        float scale = getResources().getDisplayMetrics().density;
        int dpAsPixels = (int) (sizeInDp*scale + 0.5f);
        return dpAsPixels;
    }

    // ------------- EVENTS CARD METHODS ------------------//

    protected boolean eventsUpdatedRecently() {
        long lastUpdated = mcPrefs.getLong("eventsUpdated", 0l);
        if (lastUpdated == 0L) {
            return false;
        }
        Date d = new Date();
        return (d.getTime() - lastUpdated) < EIGHT_HOURS;
    }

    private void runEventsService() {
        Util.startSpinningIcon(this, eventsIcon);
        // start the service to check for news articles
        Intent eventsIntent = new Intent(this, EventsCheckIntentService.class);
        startService(eventsIntent);
    }

    // get news items
    private void getEventsFromFile() {
        NpsEventRetriever retriever = new NpsEventRetriever(this);
        List<Article> newsList = retriever
                .getArticlesFromFile(EVENTS_ITEM_FILE);

        // find the next occurring event and display it
        if (newsList != null && !newsList.isEmpty()) {

            LinearLayout newsArea = (LinearLayout) findViewById(R.id.recentEventsLayout);
            newsArea.removeAllViews();
            
            int marginPixels = dpToPixel(10);

            int closestIndex = 0;

            // sort articles by date
            Collections.sort(newsList, EVENT_ORDER);
            // get index of first event that isn't in the past
            while (newsList.get(closestIndex).getDate().before(today)) {
                closestIndex++;
            }

            int index = closestIndex;
            String spacer = "";
            while (index - closestIndex < 3 && closestIndex < (newsList.size() - 1)) {
                String event = "";
                
                if (newsList.get(index).type == Type.NPS) {
                    event += "<b><font color=\"#0099CC\">NPS - ";
                } else {
                    event += "<b><font color=\"#E9AB17\">MWR - ";
                }

                event += newsList.get(index).getHeadline()
                        + "</font></b>" + "<br>"
                        + newsList.get(index).getSubtitle();

                final String link = newsList.get(index).getLink();

                TextView item = new TextView(this);
                Util.setFontRobotoRegular(this, item);
                item.setPadding(marginPixels, marginPixels, marginPixels, marginPixels);
                item.setText(Html.fromHtml(event));
                item.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(link));
                        startActivity(browserIntent);
                    }
                });
                newsArea.addView(item);
                
                index++;
            }
        }

    }

    /*
     * AsyncTask subclass for retrieving song info in the background. AsyncTask
     * must be a subclass.
     */
    private class UpdateAvailableComputersTask extends
            AsyncTask<Long, Integer, Integer> {

        //		String resultString = "";
        int avail, unavail, pub;

        protected Integer doInBackground(Long... params) {

            // get Json value from: http://mdkl.nps.edu/computers_json/
            // for each item [2] (3rd value): 0 = unavailabe, 2 = public
            // (may/may
            // not be available),
            // -1 = not responding, likely unavailable, 1 = available
            try {
                // Logger.i("Checking available DKL computers");
                String availableJSON = retrieveWebpage("http://mdkl.nps.edu/computers_json/");

                for (int i = 0; i < 3; i++) {
                    int count = 0;
                    Pattern pattern = Pattern.compile("," + i + ",");
                    Matcher matcher = pattern.matcher(availableJSON);

                    while (matcher.find()) {
                        count++;
                    }

                    if (i == 1) {
//						String countString = "<b><font color=\"#DD0000\">"
//								+ count + "</font></b>";
//						if (count > 0) {
//							countString = "<b><font color=\"#00DD00\">" + count
//									+ "</font></b>";
//						}
//						resultString += "Available: " + countString + "<br>";
                        avail = count;
                    } else if (i == 2) {
//						resultString += "Public: " + count + "<br>";
                        pub = count;
                    } else if (i == 0) {
//						resultString += "Unavailable: " + count + "<br>";
                        unavail = count;
                    }
                }

            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return 0;
        }

        protected void onPreExecute() {
        }

        protected void onPostExecute(Integer result) {
//			availableComputers.setText(Html.fromHtml(resultString));
            availableComputers.setText(Integer.toString(avail));
            unavailableComputers.setText(Integer.toString(unavail));
            publicComputers.setText(Integer.toString(pub));
        }

    }

    // ---------------------- APP UPDATE CHECK ---------------------//
	/*
	 * AsyncTask to check for any updates to the app from
	 * pepinonline.com/npsmobile/current.xml
	 */
    private class CheckForAppUpdate extends AsyncTask<Long, Integer, Integer> {

        int newVersion;
        String comments;

        boolean updateAvailable = false;

        protected Integer doInBackground(Long... params) {

            try {
                Logger.i("Checking available app update");
                String currentVersionXMl = retrieveWebpage("http://www.pepinonline.com/npsmobile/currentversion.xml");
                parseUpdateFromXml(currentVersionXMl);

                if (newVersion > currentVersion) {
                    updateAvailable = true;
                }

                // save update date - today's day of year
                Editor e = mcPrefs.edit();
                e.putInt("lastUpdateCheck", today.get(Calendar.DAY_OF_YEAR));
                e.commit();

            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return 0;
        }

        protected void onPreExecute() {
        }

        protected void onPostExecute(Integer result) {
            if (updateAvailable) {
                Logger.i("Update available");
                setupUpdateDialog(comments);
                updateDialog.show();
            } else {
                Logger.i("No Update available");
            }

        }

        public void parseUpdateFromXml(String xmlString) {
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory
                        .newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                InputSource is = new InputSource();
                is.setCharacterStream(new StringReader("<data>" + xmlString
                        + "</data>"));

                org.w3c.dom.Document doc = db.parse(is);

                NodeList name1 = doc.getElementsByTagName("version");
                org.w3c.dom.Element line1 = (org.w3c.dom.Element) name1.item(0);
                newVersion = Integer
                        .parseInt(getCharacterDataFromElement(line1));

                // get all comments
                NodeList name2 = doc.getElementsByTagName("comment");
                org.w3c.dom.Element line2 = (org.w3c.dom.Element) name2.item(0);
                comments = "� " + getCharacterDataFromElement(line2);

                for (int i = 1; i < name2.getLength(); i++) {
                    line2 = (org.w3c.dom.Element) name2.item(i);
                    comments += "\n� " + getCharacterDataFromElement(line2);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public String getCharacterDataFromElement(org.w3c.dom.Element e) {
            Node child = e.getFirstChild();
            if (child instanceof CharacterData) {
                CharacterData cd = (CharacterData) child;
                return cd.getData();
            }
            return null;
        }

    }

}
