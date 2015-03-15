package com.pepinonline.mc;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.SerializableEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class LunchCheckIntentService extends IntentService {

    private final static String LOCAL_URL_ROOT = "http://intranet.nps.edu";
    private final static String REMOTE_URL_ROOT = "https://npsbart.nps.edu/+CSCO+0h75676763663A2F2F766167656E6172672E6163662E727168++";
    private final static String URL_SUFFIX = "/webevent/root/scripts/"
            + "webevent.plx?cmd=calmonth&token="
            + "guest.8ac1a198830e0add2bb16e96ff7624a8&drilldown=1&calID=913";
    public static SharedPreferences mcPrefs;
    private static String sUsername;
    private static String sPassword;
    private final String LUNCH_SPECIAL_ACTION_COMPLETE = "LUNCH_SPECIAL_CHECK_COMPLETE";
    private DefaultHttpClient httpClient;
    private int dayOfYearPST;
    private String lunchSpecial;
    private String specialDate;

    public LunchCheckIntentService() {
        super("LunchCheckIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Logger.i("Starting Lunch Special Check Service");

        // get shared preferences
        mcPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        dayOfYearPST = cal.get(Calendar.DAY_OF_YEAR);

        if (getCurrentLunchFromGAE()) {
            saveSpecial();
            broadcastCompletion();

        } else {
            // if it's not already on the GAE server, get from NPS
            // and save to GAE if successful
            getSpecialFromNPSWebsite();
            if (lunchSpecial != null) {
                // save to device
                saveSpecial();
                // notify activity that the service is done
                broadcastCompletion();
                // save to GAE server
                String[] dateParts = specialDate.split(" ");
                int specialDay = Integer.parseInt(dateParts[1].replaceAll(",", ""));
                // if the dates match
                if (specialDay == cal.get(Calendar.DAY_OF_MONTH)) {
                    String lunchString = "<id>" + dayOfYearPST + "</id>" +
                            "<menu><![CDATA[" + lunchSpecial.trim() + "]]></menu>" +
                            "<date>" + specialDate + "</date>";
                    updateCurrentLunchOnServer(lunchString);
//					Logger.i("Sending: " + lunchString);
                }
            } else {
                broadcastCompletion();
            }
        }

        // stop the thread
        Logger.i("Stopping Lunch Special Check Service");
        stopSelf();
    }

    private void getSpecialFromNPSWebsite() {
        httpClient = Util.getNewHttpClient();

        try {
            if (Util.onNpsNetwork()) {
                Logger.i("Checking lunch specials via Intranet");
                Document localPage = getPageDocument(LOCAL_URL_ROOT
                        + URL_SUFFIX);
                String link = getLinkToSpecial(localPage);
                getSpecialDetails(link);
            } else {
                Logger.i("Checking lunch specials via VPN");
                retrieveUserInfo();
                if (sUsername != null && sPassword != null) {
                    loginToVpn();
                    Document remotePage = getPageDocument(REMOTE_URL_ROOT
                            + URL_SUFFIX);
                    String link = getLinkToSpecial(remotePage);
                    getSpecialDetails(link.replace("https://intranet.nps.edu",
                            REMOTE_URL_ROOT));
                } else {
                    Logger.i("NPS User info not set");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpClient.getConnectionManager().shutdown();
            Logger.i("Connection closed.");
        }
    }

    private Document getPageDocument(String url) throws Exception {
        HttpGet httpget = new HttpGet(url);

        // Create a response handler
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = httpClient.execute(httpget, responseHandler);
        Logger.i("Successfully accessed " + url);

        Document d = Jsoup.parse(responseBody);
        return d;
    }

    private void retrieveUserInfo() {
        sUsername = mcPrefs.getString("username", null);
        sPassword = mcPrefs.getString("password", null);
    }

    private void loginToVpn() throws Exception {
        // get WebVPN login page and post login details
        Logger.i("Logging in to WebVPN");
        getPageDocument("https://npsbart.nps.edu/+CSCOE+/logon.html");
        postToLogonForm();
    }

    private void postToLogonForm() {
        try {
            String action = "/+webvpn+/index.html";
            String vpnServer = "https://npsbart.nps.edu";
            HttpPost post = new HttpPost(vpnServer + action);

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("username", sUsername));
            nameValuePairs.add(new BasicNameValuePair("password", sPassword));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));

            HttpResponse response = httpClient.execute(post);
            Logger.i("VPN login form response: " + response.getStatusLine());
            HttpEntity entity = response.getEntity();
            entity.consumeContent();

        } catch (Exception e) {
            Logger.e(e.toString());
        }
    }

    /**
     * Retrieve the link we need to access the daily lunch special. It changes
     * daily.
     *
     * @return
     */
    private String getLinkToSpecial(Document doc) {
        String link = "";
        try {
            Element specialLink = doc
                    .select("a:contains(Club Del Monte Lunch)").last();
            link = specialLink.attr("href");
            if (link != null) {
                int start = link.indexOf("popupEvent") + 12;
                int end = link.indexOf(")", start) - 1;
                link = link.substring(start, end);
                // link = link.substring(link.indexOf("(")+2,
                // link.indexOf(")")-1);
                // Logger.i("lunch link: " + link);
            }
        } catch (Exception e) {
            Logger.e("getLinkToSpecial: " + e.toString());
        }
        return link;
    }

    /**
     * Connect to the daily special page and get the details
     *
     * @param url
     */
    private void getSpecialDetails(String url) {
        try {
            Document doc = getPageDocument(url);

            // get special date
            Element date = doc.select("th.fieldLabel:contains(Date:) ~ td")
                    .first();
            specialDate = date.text();

            // get special details
            Element special = doc.select("pre").first();
            lunchSpecial = special.text();

            // format special text
            lunchSpecial = lunchSpecial.replaceAll("[\n\r]+", " ");
            lunchSpecial = lunchSpecial.replaceAll(": ", ":\n");
            lunchSpecial = lunchSpecial.replaceAll("Sides:", "\n    Sides:");
            lunchSpecial = lunchSpecial.replaceAll("Side Choices:", "\n    Side Choices:");
            lunchSpecial = lunchSpecial.replaceAll("Soup of the Day:", "\n    Soup of the Day:");
            lunchSpecial = lunchSpecial.replaceAll("DEL MONTE ", "DEL MONTE\n");
            lunchSpecial = lunchSpecial.replaceAll("EL PRADO DINING ROOM ", "\n\nEL PRADO DINING ROOM\n    ");
            lunchSpecial = lunchSpecial.replaceAll("Subject to Availability", "");


//			 Logger.i(specialDate + " / " + lunchSpecial);
        } catch (Exception e) {
            Logger.e("getSpecialDetails: " + e.toString());
        }
    }

    /**
     * Save the daily special to the default shared preferences
     */
    private void saveSpecial() {
        if (lunchSpecial != null) {
            try {
                // format lunch menu text
                String formattedSpecial = lunchSpecial;
                formattedSpecial = formattedSpecial.replaceAll("\n", "<br>");
                formattedSpecial = formattedSpecial.replaceAll("(CAF� DEL MONTE|EL PRADO DINING ROOM)",
                        "<font color=\"#00CCFF\"><b>$1:</b></font>");

                formattedSpecial = formattedSpecial.replaceAll("(Sides|Main Entr�e Choices|Side Choices|Soup of the Day)",
                        "<font color=\"#777777\"><b>$1</b></font>");

                Editor e = mcPrefs.edit();
                e.putString("lunch_special", formattedSpecial);
                e.putString("special_date", specialDate);
                e.commit();
            } catch (Exception e) {// Catch exception if any
                Logger.e("Save Specials Exception: " + e.getMessage());
            }
        }
    }


    /**
     * Send broadcast that the service is done.
     */
    private void broadcastCompletion() {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(LUNCH_SPECIAL_ACTION_COMPLETE);
        sendBroadcast(broadcastIntent);
    }

    // ------ GET/SAVE LUNCH VALUE FROM/TO GAE SERVER -------------//

    public boolean getCurrentLunchFromGAE() {
        Logger.i("Retrieving lunch special from GAE");
        HttpClient client = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet("https://delmontelunch.appspot.com/savelunch"
                + "?date=" + dayOfYearPST);
        try {
            HttpResponse response = client.execute(getRequest);
            Logger.i("Response Status: " + response.getStatusLine());
            int status = response.getStatusLine().getStatusCode();
            if (status == 200) {
                String responseBody = getResponseBody(response);

                Logger.i("Response Body: " + responseBody);
                if (responseBody.contains("=none")) {
                    Logger.i("Special for " + dayOfYearPST + " not on GAE server yet");
                    return false;
                } else {
                    parseLunchFromXml(responseBody);
                    return true;
                }
            }
            return false;
        } catch (ClientProtocolException e) {
            Logger.e("getCurrentLunchFromGAE() ClientProtocolException: "
                    + e.toString());
        } catch (IOException e) {
            Logger.e("getCurrentLunchFromGAE() IOException: " + e.toString());
        } catch (Exception e) {
            Logger.e("getCurrentLunchFromGAE() Exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            client.getConnectionManager().shutdown();

        }
        return false;

    }

    public String updateCurrentLunchOnServer(String lunchSpecialString) {
        Logger.i("Saving lunch special to GAE");
        HttpClient client = new DefaultHttpClient();
        HttpPost postRequest = new HttpPost("https://delmontelunch.appspot.com/savelunch");
        String responseBody = null;

        try {
            postRequest.setEntity(new SerializableEntity(lunchSpecialString, true));

            HttpResponse response = client.execute(postRequest);
            Logger.i("Response Status: " + response.getStatusLine());
            int status = response.getStatusLine().getStatusCode();
            if (status == 200) {
                responseBody = getResponseBody(response);
                Logger.i("Response Body: " + responseBody);
            }
            return responseBody;
        } catch (ClientProtocolException e) {
            Logger.e("updateCurrentLunchOnServer() ClientProtocolException: "
                    + e.getMessage());
        } catch (IOException e) {
            Logger.e("updateCurrentLunchOnServer() IOException: " + e.getMessage());
        } catch (Exception e) {
            Logger.e("updateCurrentLunchOnServer() Exception: " + e.getMessage());
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            client.getConnectionManager().shutdown();

        }
        return null;

    }

    private String getResponseBody(HttpResponse response) {
        String response_text = null;
        ResponseHandler<String> responseHandler = new BasicResponseHandler();

        try {
            response_text = responseHandler.handleResponse(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response_text;
    }

    public void parseLunchFromXml(String xmlString) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader("<data>" + xmlString
                    + "</data>"));

            org.w3c.dom.Document doc = db.parse(is);
//			NodeList name = doc.getElementsByTagName("id");
//			org.w3c.dom.Element line = (org.w3c.dom.Element) name.item(0);
//			l.setId(getCharacterDataFromElement(line));

            NodeList name1 = doc.getElementsByTagName("menu");
            org.w3c.dom.Element line1 = (org.w3c.dom.Element) name1.item(0);
            lunchSpecial = getCharacterDataFromElement(line1);

            NodeList name2 = doc.getElementsByTagName("date");
            org.w3c.dom.Element line2 = (org.w3c.dom.Element) name2.item(0);
            specialDate = getCharacterDataFromElement(line2);


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
