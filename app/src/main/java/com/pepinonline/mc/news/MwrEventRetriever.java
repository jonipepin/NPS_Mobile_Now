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

import android.content.Context;

import com.pepinonline.mc.Logger;
import com.pepinonline.mc.Util;
import com.pepinonline.mc.news.Article.Type;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Calendar;

public class MwrEventRetriever extends ArticleRetriever {

    MwrEventRetriever(Context context) {
        this(context, "", "", "");
    }

    MwrEventRetriever(Context context, String sourceURL) {
        this(context, sourceURL, "", "");
    }

    public MwrEventRetriever(Context context, String sourceURL, String mainHtmlComponent, String summaryHtmlComponent) {
        super(context, sourceURL, mainHtmlComponent, summaryHtmlComponent);
    }

//	@Override
//	protected void getHeaderInfo(Element page) {
//		Elements headlines = page.select(
//				// get all header links
//				"h3 > a");
//		Logger.i("headerlines size: " + headlines.size());
//		for (int i = 0; i < headlines.size(); i++) {
//			String link = headlines.get(i).attr("href");
//			String head = headlines.get(i).text();
//			Logger.i("link: " + link + ", header: " + head);
//			articleList.add(new Article(head, link));
//		}
//	}
//	
//	@Override
//	protected void getSummaries(Element page) {
//		Elements sum = page.select(
//				// get all html paragraph elements that follow the span element
//				summaryHtmlComponent);
//		if (sum.size() > 0) {
//			for (int i = 0; i < articleList.size(); i++){
//				Elements paragraphs = sum.get(i).select("p");
//				if (paragraphs.size() > 1) {
//					// find subtitle by the first paragraph
//					String subtitle = paragraphs.get(0).text();
//					articleList.get(i).setSubtitle(subtitle);
//				}
//				// find summary by parsing last paragraph element of each list item
//				String summary = paragraphs.get(paragraphs.size()-1).text();
//				articleList.get(i).setSummary(summary);
//			}	
//		}
//	}	

    @Override
    protected void getHeaderInfo(Element page) {

    }

    @Override
    protected void getSummaries(Element page) {
        Logger.i("inside getSummaries");
        Elements sum = page.select(
                // get all html paragraph elements that follow the span element
                summaryHtmlComponent);
        Logger.i("Number of articles: " + sum.size());
        for (int i = 0; i < sum.size(); i++) {
            Logger.i("Article: " + sum.get(i).html());
            String head = sum.get(i).select("h3").first().text();
            String link = sum.get(i).select("a").first().attr("href");
            Logger.i("link: " + link + ", header: " + head);

            Article a = new Article(head, link);

            Element st = sum.get(i).select("strong").first();
            String subtitle = "";
            if (st != null) {
                subtitle = st.text();
                // set subtitle
                a.setSubtitle(subtitle);

                // get date from subtitle text
                Calendar c = Calendar.getInstance();
                String[] dateParts = subtitle.split(" ");
                int month = Util.getMonth(dateParts[1]);
                if (month >= 0) {
                    int day = Integer.parseInt(dateParts[2].replaceAll(",", ""));

                    // GET THE CURRENT MONTH
                    int currentMonth = c.get(Calendar.MONTH);

					/* IF THE EVENT MONTH IS BEFORE THE CURRENT MONTH,
					   SET THE EVENT YEAR AS NEXT YEAR.
					*/
                    if (currentMonth > month) {
                        Logger.i("setting event year to " + (c.get(Calendar.YEAR) + 1));
                        c.set(Calendar.YEAR, c.get(Calendar.YEAR) + 1);
                    }

                    // Log.i("MyTag", "article day/month: " + day + "/" + month);
                    c.set(Calendar.DAY_OF_MONTH, day);
                    c.set(Calendar.MONTH, month);

                } else {
                    c.set(Calendar.YEAR, 2020);
                }
                a.setDate(c);
//				Log.i("MyTag", "article date: " + c.toString());

            }

            Elements paragraphs = sum.get(i).select("p");
            String summary = paragraphs.text().replace(subtitle, "");
            a.setSummary(summary);

            // set the type of article
            a.type = Type.MWR;

            articleList.add(a);
        }
    }
}
