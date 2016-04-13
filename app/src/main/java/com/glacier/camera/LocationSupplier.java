package com.glacier.camera;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;


public class LocationSupplier {
	private static final String TAG = "LocationSupplier";

	private Context context = null;
	private LocationManager locationManager = null;
	private MyLocationListener [] locationListeners = null;

	LocationSupplier(Context context) {
		this.context = context;
		locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
	}

	public Location getLocation() {

		if( locationListeners == null )
			return null;

		for(int i=0;i<locationListeners.length;i++) {
			Location location = locationListeners[i].getLocation();
			if( location != null )
				return location;
		}
		return null;
	}
	
	private static class MyLocationListener implements LocationListener {
		private Location location = null;
		public boolean test_has_received_location = false;
		
		Location getLocation() {
			return location;
		}
		
	    public void onLocationChanged(Location location) {
			if( MyDebug.LOG )
				Log.d(TAG, "onLocationChanged");
			this.test_has_received_location = true;

    		if( location.getLatitude() != 0.0d || location.getLongitude() != 0.0d ) {
	    		if( MyDebug.LOG ) {
	    			Log.d(TAG, "received location:");
	    			Log.d(TAG, "lat " + location.getLatitude() + " long " + location.getLongitude() + " accuracy " + location.getAccuracy());
	    		}
				this.location = location;
    		}
	    }

	    public void onStatusChanged(String provider, int status, Bundle extras) {
	         switch( status ) {
	         	case LocationProvider.OUT_OF_SERVICE:
	         	case LocationProvider.TEMPORARILY_UNAVAILABLE:
	         	{
					if( MyDebug.LOG ) {
						if( status == LocationProvider.OUT_OF_SERVICE )
							Log.d(TAG, "location provider out of service");
						else if( status == LocationProvider.TEMPORARILY_UNAVAILABLE )
							Log.d(TAG, "location provider temporarily unavailable");
					}
					this.location = null;
					this.test_has_received_location = false;
	         		break;
	         	}
	         	default:
	         		break;
	         }
	    }

	    public void onProviderEnabled(String provider) {
	    }

	    public void onProviderDisabled(String provider) {
			if( MyDebug.LOG )
				Log.d(TAG, "onProviderDisabled");
			this.location = null;
			this.test_has_received_location = false;
	    }
	}


	boolean setupLocationListener() {
		if( MyDebug.LOG )
			Log.d(TAG, "setupLocationListener");
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);


		boolean store_location = sharedPreferences.getBoolean(PreferenceKeys.getLocationPreferenceKey(), false);
		if( store_location && locationListeners == null ) {








			boolean has_coarse_location_permission = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
			boolean has_fine_location_permission = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
			if( MyDebug.LOG ) {
				Log.d(TAG, "has_coarse_location_permission? " + has_coarse_location_permission);
				Log.d(TAG, "has_fine_location_permission? " + has_fine_location_permission);
			}
			if( !has_coarse_location_permission && !has_fine_location_permission ) {
				if( MyDebug.LOG ) {
					Log.e(TAG, "don't have ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permissions");
					Log.e(TAG, "ACCESS_COARSE_LOCATION returns " + ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION));
					Log.e(TAG, "ACCESS_FINE_LOCATION returns " + ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION));
				}
				return false;
			}
			locationListeners = new MyLocationListener[2];
			locationListeners[0] = new MyLocationListener();
			locationListeners[1] = new MyLocationListener();
			



			if( locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER) ) {
				if( has_coarse_location_permission ) {
					locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, locationListeners[1]);
					if( MyDebug.LOG )
						Log.d(TAG, "created coarse (network) location listener");
				}
				else {
					if( MyDebug.LOG ) {
						Log.e(TAG, "don't have ACCESS_COARSE_LOCATION permission");
						Log.e(TAG, "ACCESS_COARSE_LOCATION returns " + ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION));
					}
				}
			}
			else {
				if( MyDebug.LOG )
					Log.e(TAG, "don't have a NETWORK_PROVIDER");
			}
			if( locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER) ) {
				if( has_fine_location_permission ) {
					locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListeners[0]);
					if( MyDebug.LOG )
						Log.d(TAG, "created fine (gps) location listener");
				}
				else {
					if( MyDebug.LOG ) {
						Log.e(TAG, "don't have ACCESS_FINE_LOCATION permission");
						Log.e(TAG, "ACCESS_FINE_LOCATION returns " + ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION));
					}
				}
			}
			else {
				if( MyDebug.LOG )
					Log.e(TAG, "don't have a GPS_PROVIDER");
			}
		}
		else if( !store_location ) {
			freeLocationListeners();
		}
		return true;
	}
	
	void freeLocationListeners() {
		if( MyDebug.LOG )
			Log.d(TAG, "freeLocationListeners");
		if( locationListeners != null ) {
			for(int i=0;i<locationListeners.length;i++) {
				locationManager.removeUpdates(locationListeners[i]);
	            locationListeners[i] = null;
			}
            locationListeners = null;
		}
	}
	


	public boolean testHasReceivedLocation() {
		if( locationListeners == null )
			return false;
		for(int i=0;i<locationListeners.length;i++) {
			if( locationListeners[i].test_has_received_location )
				return true;
		}
		return false;
	}

	public boolean hasLocationListeners() {
		if( this.locationListeners == null )
			return false;
		if( this.locationListeners.length != 2 )
			return false;
		for(int i=0;i<this.locationListeners.length;i++) {
			if( this.locationListeners[i] == null )
				return false;
		}
		return true;
	}

	public static String locationToDMS(double coord) {
		String sign = (coord < 0.0) ? "-" : "";
		boolean is_zero = true;
		coord = Math.abs(coord);
	    int intPart = (int)coord;
	    is_zero = is_zero && (intPart==0);
	    String degrees = String.valueOf(intPart);
	    double mod = coord - intPart;

	    coord = mod * 60;
	    intPart = (int)coord;
	    is_zero = is_zero && (intPart==0);
	    mod = coord - intPart;
	    String minutes = String.valueOf(intPart);

	    coord = mod * 60;
	    intPart = (int)coord;
	    is_zero = is_zero && (intPart==0);
	    String seconds = String.valueOf(intPart);

	    if( is_zero ) {

	    	sign = "";
	    }
	    

	    return sign + degrees + "\u00b0" + minutes + "'" + seconds + "\"";
	}
}
