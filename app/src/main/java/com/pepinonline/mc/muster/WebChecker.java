/** WebChecker 
 *
 *  Author: 		Joni Pepin, jpepin@nps.edu
 *  Date:			22 Dec 2011
 *  Description: 	This class provides the ability to check the student 
 *  				check-in page and determine if the user has mustered.  		
 *
 *  Library Requirements:
 * 		JSoup - http://jsoup.org/download
 */

package com.pepinonline.mc.muster;

import com.pepinonline.mc.Logger;
import com.pepinonline.mc.MainActivity;

import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.security.KeyStore;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
//import org.jsoup.select.Elements;

//import android.graphics.Color;

public class WebChecker {

    public Date musterDate;
    private String dateString;
    private String username;
    private String password;

    WebChecker(String user, String pwd) {
        this.username = user;
        this.password = pwd;
    }

    /**
     * Create a new http client that is compatible with the NPS website
     *
     * @return NPS-compatible DefaultHttpClient object
     */
    public static DefaultHttpClient getNewHttpClient() {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            // use extended SSLSocketFactory so that I can connect to the
            // http server ignoring unknown certificate errors by overriding
            // the default Trust Manager.
            SSLSocketFactory sf = new CustomSSLSocketFactory(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            // set http client parameters
            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));

            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

            return new DefaultHttpClient(ccm, params);
        } catch (Exception e) {
            return new DefaultHttpClient();
        }
    }

    /**
     * Check to see if the user has mustered.
     *
     * @return 0 = mustered, 1 = not mustered, 2 = bad credentials,
     * 3 = other connection error.
     */
    public int musterCheck() {
        // create new http client
        DefaultHttpClient httpClient = getNewHttpClient();

        // set website login credentials
        httpClient.getCredentialsProvider().setCredentials(
                new AuthScope("intranet.nps.edu", 443),
                new UsernamePasswordCredentials(username, password));

        try {
            Logger.i("Accessing student check-in page.");
            // get the check-in page contents
            HttpGet httpget = new HttpGet(MainActivity.localUrl);

            // Create a response handler
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String responseBody = httpClient.execute(httpget, responseHandler);
            Logger.i("Successfully accessed student check-in page.");

            // scrape page with JSoup
            org.jsoup.nodes.Document doc = Jsoup.parse(responseBody);

            // find muster status text on page
            Element musterConf = doc.select(
                    "span#ctl00_ContentPlaceHolder1_welcometxt").first();
            if (musterConf.text().contains("you have mustered today")) {
                String temp = musterConf.text();
                temp = temp.substring(temp.indexOf("(") + 1).replaceFirst("\\)", "");
                dateString = temp;
                convertStringToDate();
                MainActivity.savedMusterDate = temp;
                return 0; // 0 = mustered
            }

        } catch (HttpResponseException he) {
            // user name or password incorrect
            String exception = he.toString();
            Logger.e(exception);
            if (exception.contains("Unauthorized")) {
                return 2; // 2 = bad credentials
            }
            return 3; // unable to connect to server
        } catch (Exception e) {
            Logger.e("Website Exception: " + e.toString());
            return 3; // other exception
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpClient.getConnectionManager().shutdown();

        }
        return 1; // 1 = not mustered
    }

    private void convertStringToDate() {
        String pattern = "EEE, MMM dd, yyyy 'at' hh:mm:ss a";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        try {
            musterDate = sdf.parse(dateString);
            //Log.i(Main.MYTAG, Main.musterDate.toLocaleString());
        } catch (ParseException e) {
            Logger.e(e.getMessage());
        }
    }

}
