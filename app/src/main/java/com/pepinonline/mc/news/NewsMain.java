/** NewsMain 
 * 
 *  Author: 		Joni Pepin, jpepin@nps.edu
 *  Date:			22 Dec 2011
 *  Description: 	This activity retrieves the latest news articles from
 *  				the NPS public site and displays them in a ListView.		
 */

package com.pepinonline.mc.news;

import java.util.ArrayList;
import java.util.Date;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.actionbarsherlock.view.Window;
import com.pepinonline.mc.Logger;
import com.pepinonline.mc.MainActivity;
import com.pepinonline.mc.R;
import com.pepinonline.mc.Util;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

public class NewsMain extends SherlockActivity implements
		AdapterView.OnItemClickListener {

	public final long EIGHT_HOURS = 28800000; // 8 hours in milliseconds
	private final String NEWS_URL = "http://www.nps.edu/About/News/University/UniverNews.html";
	private final int NUMBER_OF_ARTICLES = 10;
	private String NEWS_ITEM_FILE = "NPS_News_Articles";
	private ListView newsListView;
	private ArrayAdapter<Article> articleListAdapter;
	private ProgressDialog progressDlg;

	protected ArticleRetriever retriever;
	protected ArrayList<Article> newsList;

	public static SharedPreferences mcPrefs;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.news);

		setSupportProgressBarIndeterminateVisibility(false);
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		// get shared preferences
		mcPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		newsListView = (ListView) findViewById(R.id.newsList);
		newsListView.setOnItemClickListener(this);

		retrieveArticles();
		setListItems();

		articleListAdapter = new ArticlesAdapter(this, R.layout.news_item,
				newsList);
		newsListView.setAdapter(articleListAdapter);
	}

	protected void updateNews() {
		retriever = new ArticleRetriever(this, NEWS_URL, "div#NewsFacItems",
				"span.HeaderLink ~ p");
		newsList = retriever.getLatestArticles(NUMBER_OF_ARTICLES);
		if (!newsList.isEmpty()) {
			retriever.saveListItems(newsList, NEWS_ITEM_FILE);
		}
	}

	protected void retrieveArticles() {
		retriever = new ArticleRetriever(this);
		newsList = retriever.getArticlesFromFile(NEWS_ITEM_FILE);
	}

	protected void setListItems() {
		if (newsList == null) {
			newsList = new ArrayList<Article>();
			new UpdateNewsTask().execute();
		} else if (!updatedRecently()) {
			Logger.i("News NOT updated recently");
			new UpdateNewsTask().execute();
		}
	}

	protected boolean updatedRecently() {
		long lastUpdated = mcPrefs.getLong("newsUpdated", 0l);
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
	protected void saveLastUpdatedTime() {
		try {
			Editor e = mcPrefs.edit();
			e.putLong("newsUpdated", new Date().getTime());
			e.commit();
		} catch (Exception e) {// Catch exception if any
			Logger.i("saveLastUpdatedTime Exception: " + e.getMessage());
		}

	}

	// ----------------- Event Handlers ----------------- //

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// do nothing on changes to orientation or slide-out keyboard
		// Logger.i(, "Main configuration change detected");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		SubMenu subMenu1 = menu.addSubMenu("Options");
		MenuInflater menuInflater = getSupportMenuInflater();
		menuInflater.inflate(R.menu.cal_options_menu, subMenu1);

		MenuItem subMenu1Item = subMenu1.getItem();
		subMenu1Item.setIcon(R.drawable.abs__ic_menu_moreoverflow_holo_light);
		subMenu1Item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.calmenu_update:
			new UpdateNewsTask().execute();
			break;
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
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		Logger.i("item selected");
		Article item = articleListAdapter.getItem(position);
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(item
				.getLink()));
		startActivity(browserIntent);
	}

	// ------------------------ Sub-classes -----------------------------//

	/*
	 * AsyncTask subclass for retrieving news in the background. AsyncTask must
	 * be a subclass.
	 */
	protected class UpdateNewsTask extends AsyncTask<Long, Integer, Integer> {

		protected Integer doInBackground(Long... params) {
			updateNews();
			return 0;
		}

		protected void onPreExecute() {
			// showProgressDialog();
			// Hack to hide the regular progress bar
			setSupportProgress(Window.PROGRESS_END);
			setSupportProgressBarIndeterminateVisibility(true);
		}

		protected void onPostExecute(Integer result) {
			// progressDlg.cancel();
			// Hack to hide the regular progress bar
			setSupportProgressBarIndeterminateVisibility(false);

			if (newsList.isEmpty()) {
				Toast.makeText(NewsMain.this, "Please try again",
						Toast.LENGTH_SHORT).show();
			} else {
				articleListAdapter = new ArticlesAdapter(getBaseContext(),
						R.layout.news_item, newsList);
				newsListView.setAdapter(articleListAdapter);
				saveLastUpdatedTime();

				Toast.makeText(NewsMain.this, "Update Complete",
						Toast.LENGTH_SHORT).show();
			}
		}

		public void showProgressDialog() {
			progressDlg = new ProgressDialog(NewsMain.this);
			progressDlg.setMessage("Retrieving Latest Items...");
			progressDlg.getWindow().setGravity(Gravity.BOTTOM);
			progressDlg.setCancelable(true);
			progressDlg.show();
		}
	}

}
