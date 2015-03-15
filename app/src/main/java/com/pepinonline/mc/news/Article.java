package com.pepinonline.mc.news;

import java.io.Serializable;
import java.util.Calendar;

public class Article implements Serializable {

    private static final long serialVersionUID = 1L;

    ;
    public Type type = Type.NPS; // default to NPS
    private String headline;
    private String subtitle;
    private String summary;
    private String link;
    private Calendar date;
    public Article(String headline, String link) {
        this(headline, "", link);
    }

    public Article(String title, String summary, String link) {
        this.headline = title;
        this.summary = summary;
        if (link.contains("http://")) {
            this.link = link;
        } else {
            this.link = "http://www.nps.edu" + link;
        }
    }

    public String getHeadline() {
        return this.headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getSubtitle() {
        return this.subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getSummary() {
        return this.summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getLink() {
        return this.link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Calendar getDate() {
        return this.date;
    }

    public void setDate(Calendar date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return headline;
    }

    public static enum Type {NPS, MWR}

}
