package com.pepinonline.mc.news;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;

import com.pepinonline.mc.R;
import com.pepinonline.mc.news.Article.Type;

import android.R.color;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ArticlesAdapter extends ArrayAdapter<Article> {

	ViewHolder viewHolder;
	private ArrayList<Article> items;
	
	static final Comparator<Article> EVENT_ORDER = new Comparator<Article>() {
		public int compare(Article a1, Article a2) {
			if(a1.getDate() == null) {
				Calendar c = Calendar.getInstance();
				c.set(Calendar.YEAR, 2020);
				a1.setDate(c);
			}
			if(a2.getDate() == null) {
				Calendar c = Calendar.getInstance();
				c.set(Calendar.YEAR, 2020);
				a2.setDate(c);
			}
			return a1.getDate().compareTo(a2.getDate());
		}
	};

	public ArticlesAdapter(Context context, int textViewResourceId,
			ArrayList<Article> items) {
		super(context, textViewResourceId, items);
		this.items = items;
		// put them in order
		Collections.sort(items, EVENT_ORDER);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			LayoutInflater li = LayoutInflater.from(getContext());
			view = li.inflate(R.layout.news_item, null);

			viewHolder = new ViewHolder();

			// cache the views
			viewHolder.headline = (TextView) view
					.findViewById(R.id.news_item_headline);
			viewHolder.summary = (TextView) view
					.findViewById(R.id.news_item_summary);
			viewHolder.subtitle = (TextView) view
					.findViewById(R.id.news_item_subtitle);

			// link the cached views to the convertview
			view.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) view.getTag();
		}

		Article article = items.get(position);
		if (article != null) {
			// set the data to be displayed
			
			if(article.type == Type.MWR) {
				viewHolder.headline.setText("MWR - " + article.getHeadline());
				viewHolder.headline.setTextColor(Color.parseColor("#E9AB17")); // #goldenrod
			} else {
				viewHolder.headline.setTextColor(Color.parseColor("#0099CC")); // #cyan
				viewHolder.headline.setText(article.getHeadline());
			}
			
			viewHolder.summary.setText(article.getSummary());
			
			String sub = article.getSubtitle();
			if (sub != null) {
				viewHolder.subtitle.setText(article.getSubtitle());
			} else {
				// hide this field if there is no subtitle
				viewHolder.subtitle.setVisibility(8);
			}

		}
		return view;
	}

	// class for caching the views in a row
	private class ViewHolder {
		TextView headline;
		TextView summary;
		TextView subtitle;
	}

}
