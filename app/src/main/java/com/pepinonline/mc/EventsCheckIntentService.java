package com.pepinonline.mc;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.pepinonline.mc.news.Article;
import com.pepinonline.mc.news.ArticleRetriever;
import com.pepinonline.mc.news.MwrEventRetriever;
import com.pepinonline.mc.news.NpsEventRetriever;

import java.util.ArrayList;
import java.util.Date;

public class EventsCheckIntentService extends IntentService {

    public static SharedPreferences mcPrefs;
    public final String LOG_TAG = "MyTag";
    private final String NPS_EVENTS_URL = "http://www.nps.edu/About/Events/Events.html";
    private final String MWR_EVENTS_URL = "http://navylifesw.com/monterey/events/";
    private final int NUMBER_OF_ARTICLES = 10;
    private final String EVENTS_ACTION_COMPLETE = "EVENTS_CHECK_COMPLETE";
    protected ArticleRetriever retriever;
    protected ArrayList<Article> newsList;
    private String EVENTS_ITEM_FILE = "NPS_Events";

    public EventsCheckIntentService() {
        super("EventsCheckIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Logger.i("Starting Events Check Service");

        // get shared preferences
        mcPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        updateNews();

        saveLastUpdatedTime();

        broadcastCompletion();
        // stop the thread
        Logger.i("Stopping Events Check Service");
        stopSelf();
    }

    /**
     * Send broadcast that the service is done.
     */
    private void broadcastCompletion() {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(EVENTS_ACTION_COMPLETE);
        sendBroadcast(broadcastIntent);
    }

    protected void updateNews() {
        // get nps events
        retriever = new NpsEventRetriever(this, NPS_EVENTS_URL, "div#EventItem",
                "div[style^=float:right]");
        newsList = retriever.getLatestArticles(NUMBER_OF_ARTICLES);

        // get mwr events
        retriever = new MwrEventRetriever(this, MWR_EVENTS_URL,
                "ul.eventslistul2",
                "li");
        newsList.addAll(retriever.getLatestArticles(NUMBER_OF_ARTICLES));

        if (newsList != null && newsList.size() > 0) {
            retriever.saveListItems(newsList, EVENTS_ITEM_FILE);
        }
    }

    /**
     * Save the check time to the default shared preferences
     */
    protected void saveLastUpdatedTime() {
        try {
            Editor e = mcPrefs.edit();
            e.putLong("eventsUpdated", new Date().getTime());
            e.commit();
        } catch (Exception e) {//Catch exception if any
            Logger.e("saveLastUpdatedTime Exception: " + e.getMessage());
        }

    }


}
