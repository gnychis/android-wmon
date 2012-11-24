package com.gnychis.awmon.GUIs;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.gnychis.awmon.R;
import com.gnychis.awmon.BackgroundService.LocationMonitor;
import com.gnychis.awmon.Core.UserSettings;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;

public class HomeLocation extends MapActivity {
	
	static final String TAG = "HomeLocation";
	static final boolean VERBOSE=true;
	
    private MapView mapView;
    Drawable drawable;
    List<Overlay> mapOverlays;
    Itemization itemizedOverlay;
    UserSettings _settings;
    ProgressDialog _pd;
    Handler _handler;
	
	@Override
	protected boolean isRouteDisplayed() {
	    return false;
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_location);
               
        debugOut("Entering the creation of the Home Location activity");
        
        // Initialize a few local variables
        mapView = (MapView) findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        _settings = new UserSettings(this);
        _handler = new Handler();

        // Try to get the location of the user's home, if it is set we zoom
        // in on it and then we return.
        if(setHomeLocationIfKnown())
        	return;
        
        // We don't have the home location, so we wait for it to be broadcast from the background service
        _pd = ProgressDialog.show(this, "", "Retrieving your current location", true, false);  
		Timer scan_timer=new Timer();
		scan_timer.schedule(new TimerTask() {
			@Override
			public void run() {
	          _handler.post(new Runnable() {	// Must do this on the main UI thread...
	              @Override
	              public void run() {
	  				if(setHomeLocationIfKnown())
						cancel();
	              }
	            });
			}

		}, 0, 1000);  // every second, we will check if we have the home location yet
  
        debugOut("done loading Home Location activity");
    }
    
    public boolean setHomeLocationIfKnown() {
        Location homeLoc = _settings.getHomeLocation();
        if(homeLoc != null) {
        	zoomAndMarkLocation(homeLoc);
        	return true;
        }
        return false;
    }
    
    public void zoomAndMarkLocation(Location l) {
        GeoPoint point = new GeoPoint((int)(l.getLatitude() * 1E6), (int) (l.getLongitude() * 1E6));
        mapView.getController().setCenter(point);
        mapView.getController().setZoom(19);

        mapOverlays = mapView.getOverlays();
        drawable = this.getResources().getDrawable(R.drawable.mapmarker);
        itemizedOverlay = new Itemization(drawable);
        
        OverlayItem overlayitem = new OverlayItem(point, "", "");
        itemizedOverlay.addOverlay(overlayitem);
        mapOverlays.add(itemizedOverlay);
        
        CircleOverlay errorRadius = new CircleOverlay(this.getApplicationContext(), l.getLatitude(), l.getLongitude(), (float)l.getAccuracy());
        mapOverlays.add(errorRadius);
        
        if(_pd!=null)
        	_pd.dismiss();
    }
    
    public class CircleOverlay extends Overlay {

        Context context;
        double mLat;
        double mLon;
        float mRadius;

         public CircleOverlay(Context _context, double _lat, double _lon, float radius ) {
                context = _context;
                mLat = _lat;
                mLon = _lon;
                mRadius = radius + LocationMonitor.LOCATION_TOLERANCE/5;
         }

         public void draw(Canvas canvas, MapView mapView, boolean shadow) {

             super.draw(canvas, mapView, shadow); 

             Projection projection = mapView.getProjection();

             Point pt = new Point();

             GeoPoint geo = new GeoPoint((int) (mLat *1e6), (int)(mLon * 1e6));

             projection.toPixels(geo ,pt);
             float circleRadius = projection.metersToEquatorPixels(mRadius);

             Paint innerCirclePaint;

             innerCirclePaint = new Paint();
             innerCirclePaint.setColor(Color.GRAY);
             innerCirclePaint.setAlpha(25);
             innerCirclePaint.setAntiAlias(true);

             innerCirclePaint.setStyle(Paint.Style.FILL);

             canvas.drawCircle((float)pt.x, (float)pt.y, circleRadius, innerCirclePaint);
        }
    }
    
    public class Itemization extends ItemizedOverlay<OverlayItem> {

        private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();

        public Itemization(Drawable defaultMarker) {

            super(boundCenterBottom(defaultMarker));
            // super(defaultMarker);

        }

        @Override
        protected OverlayItem createItem(int i) {
            return mOverlays.get(i);
        }

        public void addOverlay(OverlayItem overlay) {
            mOverlays.add(overlay);
            populate();
        }

        @Override
        public int size() {
            return mOverlays.size();
        }

    }

	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
	
    // When the user clicks finished, we save some information locally.  The home network name is
    // only saved locally (so that our application can work), and it is never shared back with us.
    public void clickedCorrect(View v) {
		Intent i = new Intent(HomeLocation.this, YourDevices.class);
        startActivity(i);
    	finish();
    }
    
    @Override
    public void onBackPressed() {
    	if(_pd!=null)
    		return;
    	
    	Intent i = new Intent(HomeLocation.this, Welcome.class);
        startActivity(i);
    	finish();
    }
    
    public void clickedIncorrect(View v) {
		
    }
}
