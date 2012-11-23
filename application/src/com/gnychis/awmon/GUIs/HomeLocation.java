package com.gnychis.awmon.GUIs;

import java.util.ArrayList;
import java.util.List;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import com.gnychis.awmon.R;
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
	
	@Override
	protected boolean isRouteDisplayed() {
	    return false;
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        debugOut("Entering the creation of the Home Location activity");
        setContentView(R.layout.home_location);
       
        mapView = (MapView) findViewById(R.id.mapview);
        
        //sets the zoom to see the location closer
        mapView.getController().setZoom(18);
 
        //this will let you to zoom in or out using the controllers
        mapView.setBuiltInZoomControls(true);
 
        GeoPoint point = new GeoPoint((int)(40.443181 * 1E6), (int) (-79.943060 * 1E6));
       //this will show you the map at the exact location you want (if you not set this you will see the map somewhere in America)
        mapView.getController().setCenter(point);
        
        mapOverlays = mapView.getOverlays();
        drawable = this.getResources().getDrawable(R.drawable.mapmarker);
        itemizedOverlay = new Itemization(drawable);
        
        OverlayItem overlayitem = new OverlayItem(point, "", "");
        itemizedOverlay.addOverlay(overlayitem);
        mapOverlays.add(itemizedOverlay);
        debugOut("done loading Home Location activity");
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
