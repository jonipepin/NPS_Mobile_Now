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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ArticleRetriever {

    protected Context context;
    protected ArrayList<Article> articleList;
    protected int numberToRetrieve;
    protected String sourceURL;
    protected String mainHtmlComponent;
    protected String summaryHtmlComponent;

    public ArticleRetriever(Context context) {
        this(context, "", "", "");
    }

    ArticleRetriever(Context context, String sourceURL) {
        this(context, sourceURL, "", "");
    }

    public ArticleRetriever(Context context, String sourceURL, String mainHtmlComponent, String summaryHtmlComponent) {
        this.context = context;
        this.sourceURL = sourceURL;
        this.mainHtmlComponent = mainHtmlComponent;
        this.summaryHtmlComponent = summaryHtmlComponent;
    }

    protected void setSourceURL(String sourceURL) {
        this.sourceURL = sourceURL;
    }

    protected void setMainComponent(String htmlComponent) {
        this.mainHtmlComponent = htmlComponent;
    }

    protected void setSummaryComponent(String htmlComponent) {
        this.summaryHtmlComponent = htmlComponent;
    }

    protected void setNumberToRetrieve(int numberToRetrieve) {
        this.numberToRetrieve = numberToRetrieve;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<Article> getArticlesFromFile(String filename) {
        try {
            FileInputStream fis = context.openFileInput(filename);
            ObjectInputStream ois = new ObjectInputStream(fis);

            articleList = (ArrayList<Article>) ois.readObject();

            ois.close();
            Logger.i("articles loaded from file");
            return articleList;
        } catch (Exception e) {
            return null;
        }
    }

    public ArrayList<Article> getLatestArticles(int numberOfArticles) {
        articleList = new ArrayList<Article>();
        numberToRetrieve = numberOfArticles;
        retrieveArticlesFromWebsite(sourceURL);
        return articleList;
    }

    public void saveListItems(List<Article> articles, String filename) {
        try {

            FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(articles);
            oos.close();

            Logger.i("Articles Saved");

        } catch (IOException e) {
        }
    }

    private void retrieveArticlesFromWebsite(String sourceURL) {
        try {
            Document doc = Jsoup.connect(sourceURL).get();
            Element allItems = doc.select(
                    mainHtmlComponent).first();
//			Logger.i("getLatestArticles allItems: " + allItems.toString());
            getHeaderInfo(allItems);
            getSummaries(allItems);

        } catch (Exception e) {
            Logger.e("getLatestArticles exception: " + e.toString());
        }
    }

    protected void getHeaderInfo(Element page) {
        Elements headlines = page.select(
                // get all header links
                "a.UniversityNewsTitle");
        int number = Math.min(headlines.size(), numberToRetrieve);
        for (int i = 0; i < number; i++) {
            String link = headlines.get(i).attr("href");
            String head = headlines.get(i).text();
            articleList.add(new Article(head, link));
        }
    }

    protected void getSummaries(Element page) {
        Elements sum = page.select(
                // get all html paragraph elements that follow the span element
                summaryHtmlComponent);
        if (sum.size() > 0) {
            for (int i = 0; i < articleList.size(); i++) {
                String summary = sum.get(i).text();
                articleList.get(i).setSummary(summary);

                // set article date as today
                Calendar c = Calendar.getInstance();
                c.set(Calendar.YEAR, 2020);
                articleList.get(i).setDate(c);
            }
        }
    }
}
