/** CampusMapMain 
 *
 *  Author: 		Joni Pepin, jpepin@nps.edu
 *  Date:			22 Dec 2011
 *  Description: 	This activity displays a .jpg version of the campus map 
 *  				in a WebView.  It's in a WebView instead of an ImageView so 
 *  				that all the multi-touch goodness is already built in.		
 */

package com.pepinonline.mc;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

public class CampusMapMain extends SherlockActivity {

    private WebView map;
    private DisplayMetrics metrics;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.campus_map);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        map = (WebView) findViewById(R.id.campus_map);
        setZoomAttributes();

        map.loadUrl("file:///android_res/drawable/nps_campusmap_jan2013.jpg");
//		map.loadUrl("file:///android_res/drawable/nps_campusmap_oct2011.png");

        metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        Logger.i("Screen ht: " + metrics.heightPixels);
        map.setInitialScale(getInitialScale());
    }

    private void setZoomAttributes() {
        WebSettings webSettings = map.getSettings();
        webSettings.setSupportZoom(true); // for regular zoom
        webSettings.setBuiltInZoomControls(true); // for multi-touch zoom
        webSettings.setDefaultZoom(WebSettings.ZoomDensity.FAR);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
    }

    /**
     * Scale the image so that it fits each individual device properly
     * based on screen size based on map image size of 1162x710.
     *
     * @return
     */
    private int getInitialScale() {

        double scale = 0;
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            int displayWidth = metrics.widthPixels;
            scale = displayWidth / 3125.0 * 100;
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            int displayHeight = metrics.heightPixels; //
            scale = displayHeight / 2385.0 * 100;
        }
        return (int) scale;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
                Intent intentHome = new Intent(this, MainActivity.class);
                intentHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intentHome);
                break;
        }
        return true;
    }
}
