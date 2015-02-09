/** EventsMain 
 * 
 *  Author: 		Joni Pepin, jpepin@nps.edu
 *  Date:			22 Dec 2011
 *  Description: 	This activity extends NewsMain so that instead of
 *  				retrieving news, it retrieves upcoming events. 		
 */

package com.pepinonline.mc.news;

import java.util.Date;

import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.widget.TextView;

import com.pepinonline.mc.Logger;
import com.pepinonline.mc.R;
import com.pepinonline.mc.news.NewsMain;

public class EventsMain extends NewsMain {

	private final String NPS_EVENTS_URL = "http://www.nps.edu/About/Events/Events.html";
	private final String MWR_EVENTS_URL = "http://navylifesw.com/monterey/events/";
	private final int NUMBER_OF_ARTICLES = 10;
	private String EVENTS_ITEM_FILE = "NPS_Events";

	/*
	 * Changed the html component that is parsed out.
	 */
	@Override
	protected void updateNews() {
		// get nps events
		retriever = new NpsEventRetriever(this, NPS_EVENTS_URL,
				"div#EventItem", "div[style^=float:right]");
		newsList = retriever.getLatestArticles(NUMBER_OF_ARTICLES);

		// get mwr events
		retriever = new MwrEventRetriever(this, MWR_EVENTS_URL,
				"ul.eventslistul2", "li");
		newsList.addAll(retriever.getLatestArticles(NUMBER_OF_ARTICLES));

		if(newsList != null && newsList.size() > 0) {
			retriever.saveListItems(newsList, EVENTS_ITEM_FILE);
		}
	}

	/*
	 * Changed the file that the events are saved to.
	 */
	@Override
	protected void retrieveArticles() {
		retriever = new MwrEventRetriever(this);
		newsList = retriever.getArticlesFromFile(EVENTS_ITEM_FILE);
	}

	@Override
	protected boolean updatedRecently() {
		long lastUpdated = mcPrefs.getLong("eventsUpdated", 0l);
		if (lastUpdated == 0L) {
			return false;
		}
		Date d = new Date();
		return (d.getTime() - lastUpdated) < EIGHT_HOURS;
	}

	/**
	 * Save the user settings to the default shared preferences, and change
	 * muster status to unknown.
	 */
	@Override
	protected void saveLastUpdatedTime() {
		try {
			Editor e = mcPrefs.edit();
			e.putLong("eventsUpdated", new Date().getTime());
			e.commit();
		} catch (Exception e) {// Catch exception if any
			Logger.e("saveLastUpdatedTime Exception: " + e.getMessage());
		}

	}

}
