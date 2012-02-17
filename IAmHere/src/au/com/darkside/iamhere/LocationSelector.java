/*
 * This GUI allows you to select a location.
 * It can be the current GPS or network location, or selected from a map.
 *
 * Written by Matthew Kwan - July 2010
 *
 * Copyright (c) 2010 Matthew Kwan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package au.com.darkside.iamhere;

import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class LocationSelector extends Activity implements LocationListener {
	private LocationManager		_locationManager;
	private Button				_requestGpsButton;
	private Button				_requestNetworkButton;

	private final static int	ACTIVITY_ENABLE_GPS = 1;
	private final static int	ACTIVITY_ENABLE_NETWORK = 2;
	private final static int	ACTIVITY_LAUNCH_MAP = 3;

    /*
     * Called when the activity is first created.
     */
    @Override
    public void
    onCreate (
    	Bundle		savedInstanceState
    ) {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.location_selector);

        _locationManager = (LocationManager) getSystemService (LOCATION_SERVICE);

        Button		b;

        b = (Button) findViewById (R.id.use_gps_button);
        b.setOnClickListener (
        	new View.OnClickListener () {
        		public void	onClick (View v) {
        			returnLocation (_locationManager.getLastKnownLocation
        										(LocationManager.GPS_PROVIDER));
        		}
        	}
        );

        b = (Button) findViewById (R.id.use_network_button);
        b.setOnClickListener (
        	new View.OnClickListener () {
        		public void	onClick (View v) {
        			returnLocation (_locationManager.getLastKnownLocation
        										(LocationManager.NETWORK_PROVIDER));
        		}
        	}
        );

        _requestGpsButton = (Button) findViewById (R.id.request_gps_button);
        _requestGpsButton.setOnClickListener (
            new View.OnClickListener () {
            	public void	onClick (View v) {
            		requestGps ();
            	}
            }
        );

        _requestNetworkButton = (Button) findViewById (R.id.request_network_button);
        _requestNetworkButton.setOnClickListener (
            new View.OnClickListener () {
            	public void	onClick (View v) {
            		requestNetwork ();
            	}
            }
        );

        setGpsButtonStatus ();
        setNetworkButtonStatus ();
        showLocations ();

        b = (Button) findViewById (R.id.launch_map_button);
        b.setOnClickListener (
        	new View.OnClickListener () {
        		public void	onClick (View v) {
        			launchMapSelector ();
        		}
        	}
        );
    }

    /*
     * Launch the map selector activity.
     */
    private void
    launchMapSelector () {
    	Intent		intent = new Intent (this, MapSelector.class);

		startActivityForResult (intent, ACTIVITY_LAUNCH_MAP);
    }

    /*
     * Show the details of a location.
     */
    private void
    showLocation (
    	final Location	loc,
    	TextView		latitudeField,
    	TextView		longitudeField,
    	TextView		accuracyField,
    	TextView		ageField
    ) {
    	if (loc == null) {
    		latitudeField.setText ("unknown");
    		longitudeField.setText ("unknown");
    		accuracyField.setText ("");
    		ageField.setText ("");
    	} else {
    		double		d;

    		d = Math.round (loc.getLatitude() * 1.0e6) / 1.0e6;
    		latitudeField.setText (String.valueOf (d));
    		d = Math.round (loc.getLongitude() * 1.0e6) / 1.0e6;
    		longitudeField.setText (String.valueOf (d));

    		if (loc.hasAccuracy ())
    			accuracyField.setText (DistanceUnits.accuracyString (
    													loc.getAccuracy ()));
    		else
    			accuracyField.setText ("unknown");

    		ageField.setText (ageString (loc.getTime ()));
    	}
    }

    /*
     * Set the status of the GPS request button depending on the current
     * state of the GPS.
     */
    private void
    setGpsButtonStatus () {
    	if (_locationManager.isProviderEnabled (LocationManager.GPS_PROVIDER))
    		_requestGpsButton.setText ("Update location");
    	else
    		_requestGpsButton.setText ("Enable GPS");
    }

    /*
     * Set the status of the network request button depending on the
     * current state of the network location provider.
     */
    private void
    setNetworkButtonStatus () {
    	if (!_locationManager.isProviderEnabled (LocationManager.NETWORK_PROVIDER)
    									|| !Utility.internetAvailable (this))
    		_requestNetworkButton.setText ("Enable network");
    	else
    		_requestNetworkButton.setText ("Update location");
    }

    /*
     * If the GPS provider is enabled, issue a GPS request.
     * Otherwise enable the GPS provider.
     */
    private void
    requestGps () {
    	if (_locationManager.isProviderEnabled (LocationManager.GPS_PROVIDER))
    		_locationManager.requestLocationUpdates (LocationManager.GPS_PROVIDER,
    															0, 0, this);
    	else
    		startActivityForResult (new Intent
    				(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS),
    													ACTIVITY_ENABLE_GPS);
    }

    /*
     * If the GPS provider is enabled, issue a GPS request.
     * Otherwise enable the GPS provider.
     */
    private void
    requestNetwork () {
    	if (!_locationManager.isProviderEnabled (LocationManager.NETWORK_PROVIDER))
    		startActivityForResult (new Intent
					(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS),
													ACTIVITY_ENABLE_NETWORK);
    	else if (!Utility.internetAvailable (this))
    		startActivityForResult (new Intent
					(android.provider.Settings.ACTION_WIRELESS_SETTINGS),
													ACTIVITY_ENABLE_NETWORK);
    	else
    		_locationManager.requestLocationUpdates (
    							LocationManager.NETWORK_PROVIDER, 0, 0, this);
    }

    /*
     * Convert an absolute time, in milliseconds since UTC, into a
     * human-readable relative time.
     */
    private String
    ageString (
    	long		time
    ) {
    	Date		d = new Date ();
    	long		duration = (d.getTime () - time) / 1000;

    	if (duration < 1)
    		return "now";
    	else if (duration < 60)
    		return String.valueOf (duration) + " seconds ago";
    	else if (duration < 120)
    		return "1 minute ago";
    	else if (duration < 3600)
    		return String.valueOf (duration / 60) + " minutes ago";
    	else if (duration < 7200)
    		return "1 hour ago";
    	else if (duration < 86400)
    		return String.valueOf (duration / 3600) + " hours ago";
    	else if (duration < 172800)
    		return "1 day ago";
    	else
    		return String.valueOf (duration / 86400) + " days ago";
    }

    /*
     * Show details of the last known GPS and network locations.
     */
    private void
    showLocations () {
    	showLocation (_locationManager.getLastKnownLocation (
    			LocationManager.GPS_PROVIDER),
    			(TextView) findViewById (R.id.gps_latitude),
        		(TextView) findViewById (R.id.gps_longitude),
        		(TextView) findViewById (R.id.gps_accuracy),
        		(TextView) findViewById (R.id.gps_age));

    	showLocation (_locationManager.getLastKnownLocation (
    			LocationManager.NETWORK_PROVIDER),
    			(TextView) findViewById (R.id.network_latitude),
        		(TextView) findViewById (R.id.network_longitude),
        		(TextView) findViewById (R.id.network_accuracy),
        		(TextView) findViewById (R.id.network_age));
    }

    /*
     * Return the image currently displayed by the MapView and finish.
     */
    private void
    returnLocation (
    	Location	loc
    ) {
    	Intent		intent = new Intent ();
    	
    	intent.putExtra (IAmHere.LOCATION_DATA, loc);
    	setResult (RESULT_OK, intent);
    	finish ();

    		// Just in case the request is still active.
    	_locationManager.removeUpdates (this);
    }

    /*
     * Called when we get a location update.
     */
    @Override
    public void
    onLocationChanged (
    	Location	loc
    ) {
		_locationManager.removeUpdates (this);	// One-off updates.
		showLocations ();
    }

	/*
	 * Called when a provider is enabled.
	 */
	@Override
    public void
    onProviderEnabled (
    	String		provider
    ) {
		if (provider == LocationManager.GPS_PROVIDER)
			setGpsButtonStatus ();
		else if (provider == LocationManager.NETWORK_PROVIDER)
			setNetworkButtonStatus ();
    }

	/*
	 * Called when a provider is disabled.
	 */
	@Override
    public void
    onProviderDisabled (
    	String		provider
    ) {
		if (provider == LocationManager.GPS_PROVIDER)
			setGpsButtonStatus ();
		else if (provider == LocationManager.NETWORK_PROVIDER)
			setNetworkButtonStatus ();
    }

	/*
	 * Unused. Required by LocationListener.
	 */
	@Override
	public void
	onStatusChanged (
		String		provider,
		int			status,
		Bundle		extras
	) {
	}

	/*
     * Handle return values from activities.
     */
    @Override
    protected void
    onActivityResult (
    	int			requestCode,
    	int			resultCode,
    	Intent		intent
    ) {
    	super.onActivityResult (requestCode, resultCode, intent);

    	switch (requestCode) {
    		case ACTIVITY_ENABLE_GPS:
    			setGpsButtonStatus ();
    			showLocations ();
    			break;
    		case ACTIVITY_ENABLE_NETWORK:
    			setNetworkButtonStatus ();
    			showLocations ();
    			break;
    		case ACTIVITY_LAUNCH_MAP:
    			if (resultCode == RESULT_CANCELED)
    	    		break;

    			try {
        			Location		loc = null;

        			if (intent != null)
        				loc = (Location) intent.getExtras().get (
        											IAmHere.LOCATION_DATA);

        			if (loc != null)
        				returnLocation (loc);
        		} catch (Exception e) {
        		}
    			break;
    	}
    }
}