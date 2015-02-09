package com.pepinonline.mc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import com.pepinonline.mc.muster.CustomSSLSocketFactory;

import android.content.Context;
import android.graphics.Typeface;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

public class Util {
	
	private static Map<ImageView, Animation> animMap;

	/**
	 * Create a new http client that is compatible with the NPS website
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
	 * Checks to see if the device is connected to the NPS wireless network.
	 * 
	 * @return true if connected, false if not.
	 */
	public static boolean onNpsNetwork() {
		try {
			// get all of the devices network interfaces
			Enumeration<NetworkInterface> nets = NetworkInterface
					.getNetworkInterfaces();
			
			// for each interface, check IP addresses to see if the device is
			// on the NPS network and return if true.  
			for (NetworkInterface netint : Collections.list(nets)) {
				Enumeration<InetAddress> ips = netint.getInetAddresses();

				for (InetAddress ip : Collections.list(ips)) {
					String ipString = ip.toString();
					if (ipString.contains("/172.20.")) {
						Logger.i("\nNPS IP: " + ipString + " | Interface: "
								+ netint.getDisplayName() + "\n");
						return true;
					}
				}

			}
		} catch (Exception e) {
			Logger.e(e.toString());
			return false;
		}
		return false;
	}
	
	/**
	 * Start spinning the NPS icon.
	 */
	public static void startSpinningIcon(Context context, ImageView view){
		// Start animating the image
		Animation myAnim = AnimationUtils.loadAnimation(context, R.anim.spin_indefinitely);
		view.startAnimation(myAnim);
		view.setClickable(false);
		// add to map for later reference
		 if(animMap == null) {
			 animMap = new HashMap<ImageView, Animation>();
		 }
		animMap.put(view, myAnim);
	}
	
	/**
	 * Stop spinning the NPS icon.
	 */
	public static void stopSpinningIcon(ImageView view){
		// Stop animating the image
		if(animMap != null) {
			Animation myAnim = animMap.get(view);
			if(myAnim != null) {
				myAnim.cancel();
				myAnim.reset();
			}
		}
		view.setClickable(true);
	}
	
	/**
	 * Converts the input String into a Date Object.
	 * 
	 * @param dateString String of format "EEE MMM dd HH:mm:ss zzz yyyy"
	 * @return Date object
	 */
	public static Date convertStringToDate(String dateString){
		String pattern = "EEE MMM dd HH:mm:ss zzz yyyy";
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		Date date = null;
		try
        {
            date = sdf.parse(dateString);
        } 
        catch (ParseException e)
        {
            Logger.e(e.getMessage());
        }
		return date;
	}
	
	/**
	 * Checks to see if the input date has today's day, month and year.
	 * 
	 * @param d Date object to be evaluated.
	 * @return true if the date is today, false if not.
	 */
	public static boolean dateIsToday(Date d){
		Date today = new Date();
		try {
			if (today.getDate() == d.getDate() &&		
				today.getMonth() == d.getMonth() &&
				today.getYear() == d.getYear()) {
				return true;
			}
		} catch (Exception e){
			Logger.i("Exception in dateIsToday(): ");
		}
		return false;
	}
	
	/**
	 * Return the integer corresponding with the month of 
	 * the year.  Jan == 0
	 * @param monthString
	 * @return -1 if not found
	 */
	public static int getMonth(String monthString) {
		if(monthString.startsWith("Jan")) {
			return 0;
		} else if(monthString.startsWith("Feb")) {
			return 1;
		} else if(monthString.startsWith("Mar")) {
			return 2;
		} else if(monthString.startsWith("Apr")) {
			return 3;
		} else if(monthString.startsWith("May")) {
			return 4;
		} else if(monthString.startsWith("Jun")) {
			return 5;
		} else if(monthString.startsWith("Jul")) {
			return 6;
		} else if(monthString.startsWith("Aug")) {
			return 7;
		} else if(monthString.startsWith("Sep")) {
			return 8;
		} else if(monthString.startsWith("Oct")) {
			return 9;
		} else if(monthString.startsWith("Nov")) {
			return 10;
		} else if(monthString.startsWith("Dec")) {
			return 11;
		}
		return -1;
	}
	
	/**
	 * Return the integer corresponding with the month of 
	 * the year.  Jan == 0
	 * @param dayString
	 * @return -1 if not found
	 */
	public static int getDayOfWeek(String dayString) {
		if(dayString.startsWith("Sun")) {
			return 0;
		} else if(dayString.startsWith("Mon")) {
			return 1;
		} else if(dayString.startsWith("Tue")) {
			return 2;
		} else if(dayString.startsWith("Wed")) {
			return 3;
		} else if(dayString.startsWith("Thu")) {
			return 4;
		} else if(dayString.startsWith("Fri")) {
			return 5;
		} else if(dayString.startsWith("Sat")) {
			return 6;
		} 
		return -1;
	}
	
	public static String CreateShortenedUrl(String original) {
		// ref:
		// http://www.androidpeople.com/android-url-shortener-using-tinyurl-example
		// modified for bit.ly instead
		String shortUrl = null;

		// bit.ly user info
		String bitlyUser = "jpepin";
		String apiKey = "R_c2942c8e7472f2d7d7e249eb341c9c3d";

		try {
			HttpClient client = new DefaultHttpClient();
			// String urlTemplate = "http://tinyurl.com/api-create.php?url=%s";
			String urlTemplate = "http://api.bit.ly/v3/shorten?login="
					+ bitlyUser + "&apiKey=" + apiKey + "&longUrl=%s";
			String uri = String
					.format(urlTemplate, URLEncoder.encode(original));
			HttpGet request = new HttpGet(uri);
			HttpResponse response = client.execute(request);
			HttpEntity entity = response.getEntity();
			InputStream in = entity.getContent();
			try {
				StatusLine statusLine = response.getStatusLine();
				int statusCode = statusLine.getStatusCode();
				if (statusCode == HttpStatus.SC_OK) {
					String enc = "utf-8";
					Reader reader = new InputStreamReader(in, enc);
					BufferedReader bufferedReader = new BufferedReader(reader);
					String results = bufferedReader.readLine();
					if (results != null) {
						shortUrl = parseShortenedUrlFromJson(results);
						Logger.i("Original Url: " + original + " | Created Url: " + shortUrl);
					} else {
						throw new IOException("empty response");
					}
				} else {
					String errorTemplate = "unexpected response: %d";
					String msg = String.format(errorTemplate, statusCode);
					throw new IOException(msg);
				}
			} finally {
				in.close();
			}
		} catch (IOException e) {
			shortUrl = "ERROR";
			Logger.e("CreateShortenedUrl IOException: " + e);
		} catch (JSONException e) {
			shortUrl = "ERROR";
			Logger.e("CreateShortenedUrl JSONException: " + e);
		} catch (Exception e) {
			shortUrl = "ERROR";
			Logger.e("CreateShortenedUrl Exception: " + e);
		}
		return shortUrl;
	}

	public static String parseShortenedUrlFromJson(String jsonString)
			throws JSONException {
		String url = "";
		JSONObject obj = new JSONObject(jsonString);
		JSONObject urlData = obj.getJSONObject("data");
		url = urlData.getString("url");
		return url;
	}
	
	public static void setFontRobotoRegular(Context context, TextView... views) {
		Typeface font = Typeface.createFromAsset(context.getAssets(),
				"fonts/Roboto-Regular.ttf");
		for(TextView v: views) {
			v.setTypeface(font);
		}
	}
	
	public static void setFontRobotoMedium(Context context, TextView... views) {
		Typeface font = Typeface.createFromAsset(context.getAssets(),
				"fonts/Roboto-Medium.ttf");
		for(TextView v: views) {
			v.setTypeface(font);
		}
	}
	
	public static void setFontRobotoLight(Context context, TextView... views) {
		Typeface font = Typeface.createFromAsset(context.getAssets(),
				"fonts/Roboto-Light.ttf");
		for(TextView v: views) {
			v.setTypeface(font);
		}
	}
}
