package com.pepinonline.mc;

import java.util.ArrayList;
import java.util.Date;

import com.pepinonline.mc.news.Article;
import com.pepinonline.mc.news.ArticleRetriever;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class NewsCheckIntentService extends IntentService {

	private final String NEWS_ACTION_COMPLETE = "NEWS_CHECK_COMPLETE";
	private final String NEWS_URL = "http://www.nps.edu/About/News/University/UniverNews.html";
	private final int NUMBER_OF_ARTICLES = 10;

	public static SharedPreferences mcPrefs;
	
	protected ArticleRetriever retriever;
	protected ArrayList<Article> newsList;
	private String NEWS_ITEM_FILE = "NPS_News_Articles";
	
	public NewsCheckIntentService() {
		super("NewsCheckIntentService");
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		Logger.i("Starting News Check Service");
		
		// get shared preferences
		mcPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		updateNews();
		
		saveLastUpdatedTime();
		
		broadcastCompletion();
		// stop the thread
		Logger.i("Stopping News Check Service");
		stopSelf();
	}
	
	/**
	 * Send broadcast that the service is done.
	 */
	private void broadcastCompletion() {
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(NEWS_ACTION_COMPLETE);
		sendBroadcast(broadcastIntent);
	}	
	
	protected void updateNews() {
		retriever = new ArticleRetriever(this, NEWS_URL, "div#NewsFacItems",
				"span.HeaderLink ~ p");
		newsList = retriever.getLatestArticles(NUMBER_OF_ARTICLES);
		if (!newsList.isEmpty()) {
			retriever.saveListItems(newsList, NEWS_ITEM_FILE);
		}
	}
	
	/**
	 * Save the user settings to the default shared preferences, and change
	 * muster status to unknown.
	 */
	protected void saveLastUpdatedTime() {
		try {
			Editor e = mcPrefs.edit();
			e.putLong("newsUpdated", new Date().getTime());
			e.commit();
		} catch (Exception e) {// Catch exception if any
			Logger.e("saveLastUpdatedTime Exception: " + e.getMessage());
		}

	}
	
	
}
