package com.pepinonline.mc;

import android.util.Log;

public class Logger {
	
	private static final boolean DEBUG = true;
	private static String TAG = "PEPIN";
	
	public static void d(String string) {
		if(DEBUG) {
			Log.d(TAG, string);
		}
	}
	
	public static void v(String string) {
		if(DEBUG) {
			Log.v(TAG, string);
		}
	}
	
	public static void i(String string) {
		if(DEBUG) {
			Log.i(TAG, string);
		}
	}
	
	public static void e(String string) {
		if(DEBUG) {
			Log.e(TAG, string);
		}
	}
	
	public static void w(String string) {
		if(DEBUG) {
			Log.w(TAG, string);
		}
	}

}
