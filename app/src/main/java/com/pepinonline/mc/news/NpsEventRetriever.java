/** ArticleRetriever 
 * 
 *  Author: 		Joni Pepin, jpepin@nps.edu
 *  Date:			22 Dec 2011
 *  Description: 	This class fetches articles from a given website.
 *  
 *  Library Requirements:
 * 		JSoup - http://jsoup.org/download		
 */

package com.pepinonline.mc.news;

import java.util.Calendar;
import java.util.Date;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.pepinonline.mc.Logger;
import com.pepinonline.mc.Util;
import com.pepinonline.mc.news.Article.Type;

import android.content.Context;

public class NpsEventRetriever extends ArticleRetriever{
	
	public NpsEventRetriever(Context context) {
		this(context, "", "", "");
	}
	
	NpsEventRetriever(Context context, String sourceURL) {
		this(context, sourceURL, "", "");
	}
	
	public NpsEventRetriever(Context context, String sourceURL, String mainHtmlComponent, String summaryHtmlComponent) {
		super(context, sourceURL, mainHtmlComponent, summaryHtmlComponent);
	}
		
	@Override
	protected void getSummaries(Element page) {
		Elements sum = page.select(
				// get all html paragraph elements that follow the span element
				summaryHtmlComponent);
		
		Elements date = page.select(
				// get all html paragraph elements that follow the span element
				"div[style^=float:left]");

		if (sum.size() > 0) {
			for (int i = 0; i < articleList.size(); i++){
				Element summaryElem = sum.get(i).select(
						// get all html paragraph elements that follow the span element
						"p:eq(2)").first();
				
				String summary = summaryElem.text();
				articleList.get(i).setSummary(summary);
				
				try {
					String dateString = date.get(i).text();
					String[] dateParts = dateString.split(" ");
					int day = Integer.parseInt(dateParts[1]);
					int month = Util.getMonth(dateParts[2]);
//					Log.i("MyTag", "article day/month: " + day + "/" + month);
					
					Calendar c = Calendar.getInstance();
					
					// GET THE CURRENT MONTH
					int currentMonth = c.get(Calendar.MONTH);
					
					/* IF THE EVENT MONTH IS BEFORE THE CURRENT MONTH,
					   SET THE EVENT YEAR AS NEXT YEAR.
					*/
					if(currentMonth > month){
						Logger.i("setting event year to " + (c.get(Calendar.YEAR) + 1));
						c.set(Calendar.YEAR, c.get(Calendar.YEAR) + 1);
					}
					
					c.set(Calendar.DAY_OF_MONTH, day);
					c.set(Calendar.MONTH, month);
					
					articleList.get(i).setDate(c);
//					Log.i("MyTag", "article date: " + c.toString());
					
					// set subtitle
					String sub = dateString;
					
					Element locationElem = sum.get(i).select(
							// get all html paragraph elements that follow the span element
							"p:containsOwn(Location:)").first();

					if (locationElem != null) {
						String loc = locationElem.text().replace("Location:", "");
						sub += ", " + loc;
					}
					
					articleList.get(i).setSubtitle(sub);
					
					// set the type of article
					articleList.get(i).type = Type.NPS;
					
				} catch (Exception e) {
					Logger.e("getSummaries exception: " + e);
				}
				
			}	
		}
	}	
}
