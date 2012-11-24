package com.gnychis.awmon.GUIs;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.ProgressDialog;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.gnychis.awmon.R;
import com.gnychis.awmon.Core.UserSettings;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class HomeLocation extends MapActivity {
	
	static final String TAG = "HomeLocation";
	static final boolean VERBOSE=true;
	
    private MapView mapView;
    Drawable drawable;
    List<Overlay> mapOverlays;
    Itemization itemizedOverlay;
    UserSettings _settings;
    ProgressDialog _pd;
	
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
				if(setHomeLocationIfKnown())
					cancel();
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
        mapView.getController().setZoom(18);

        mapOverlays = mapView.getOverlays();
        drawable = this.getResources().getDrawable(R.drawable.mapmarker);
        itemizedOverlay = new Itemization(drawable);
        
        OverlayItem overlayitem = new OverlayItem(point, "", "");
        itemizedOverlay.addOverlay(overlayitem);
        mapOverlays.add(itemizedOverlay);
        if(_pd!=null)
        	_pd.dismiss();
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
}
