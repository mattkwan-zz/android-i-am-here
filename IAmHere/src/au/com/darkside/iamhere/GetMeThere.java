/*
 * This GUI navigates the user to a location.
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

public class GetMeThere extends MapActivity implements LocationListener {
	private boolean				_useMapMode = false;
	private String				_phone;
	private String				_messageParts[];
	private Location			_locations[];
	private LocationManager		_locationManager;
	private SensorManager		_sensorManager;
	private FrameLayout			_frame;
	private ImageButton			_switchButton;
	private CompassView			_compassView;
	private MapView				_mapView;
	private MyLocationOverlay	_myLocationOverlay;
	private TargetOverlay		_targetOverlay;

	private static final int	MENU_SETTINGS = 1;
	private static final int	MENU_MAP_MODE = 2;
	private static final int	MENU_ABOUT = 3;
	private static final int	DIALOG_DISPLAY_UNITS = 1;
	private static final int	DIALOG_MAP_MODE = 2;
	private static final int	DIALOG_ABOUT = 3;
	
	private static final int	DEFAULT_COLOURS[] = new int[] {
		0x00a000, 0x0000e0, 0xc00000, 0xc0c000,
		0xc000c0, 0xff8000, 0x0000a0, 0xa0a0a0
	};

    /*
     * Called when the activity is first created.
     */
    @Override
    public void
    onCreate (
    	Bundle		savedInstanceState
    ) {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.get_me_there);

        _phone = null;
        _messageParts = null;
        _locations = null;

        _locationManager = (LocationManager) getSystemService (LOCATION_SERVICE);
        _sensorManager = (SensorManager) getSystemService (SENSOR_SERVICE);
        _frame = (FrameLayout) findViewById (R.id.frame);

        _switchButton = (ImageButton) findViewById (R.id.switch_button);
        _switchButton.setImageResource (android.R.drawable.ic_menu_mapmode);
        _switchButton.setOnClickListener (
        	new View.OnClickListener () {
            	public void	onClick (View v) {
            		toggleMode ();
            	}
            }
        );

        Intent		intent = getIntent ();
        long		id = -1;
        String		msg = null;

        if (intent != null)
        	id = intent.getLongExtra (IAmHere.RECEIVED_SMS_ID, -1);

        if (id >= 0) {
        	Database	db = new Database (this);

        	if (db.open ()) {
        		String		details[] = new String[2];

        		if (db.getDetails (id, details)) {
        			_phone = Utility.getContactNameFromNumber (this, details[0]);
        			msg = details[1];
        			db.flagAsRead (id);
        		}

        		db.close ();
        	}
        }

        if (msg != null)
        	parseMessage (msg);

        	// Create the compass view.
        _compassView = new CompassView (this, _locations, DEFAULT_COLOURS);
        _compassView.setLayoutParams (new LayoutParams (LayoutParams.FILL_PARENT,
        										LayoutParams.FILL_PARENT));
        _compassView.setVisibility (View.INVISIBLE);
         _frame.addView (_compassView);

        	// Create the map view.
        _mapView = new MapView (this, Utility.MAPS_API_KEY);
        _mapView.setLayoutParams (new LayoutParams (LayoutParams.FILL_PARENT,
        										LayoutParams.FILL_PARENT));
        _mapView.setVisibility (View.INVISIBLE);
        _frame.addView (_mapView);

        _targetOverlay = new TargetOverlay (_mapView, _locations,
        													DEFAULT_COLOURS);

        _myLocationOverlay = new MyLocationOverlay (this, _mapView);
    	_myLocationOverlay.runOnFirstFix (
    		new Runnable () {
    			public void run () {
    				_targetOverlay.showLocation (Utility.bestKnownLocation (
    														GetMeThere.this));
    			}
    		}
    	);

    	_mapView.getOverlays ().add (_targetOverlay);
    	_mapView.getOverlays ().add (_myLocationOverlay);
    	_mapView.setBuiltInZoomControls (true);
    	_mapView.setClickable (true);
    	_mapView.setEnabled (true);

        FrameLayout		frame = (FrameLayout) findViewById (R.id.message_frame);
        DisplayMetrics	metrics = new DisplayMetrics ();

		getWindowManager ().getDefaultDisplay ().getMetrics (metrics);

		float			fontSize = (float) Math.floor (metrics.xdpi / 10.25);

        frame.addView (new MessageView (this, _phone, _messageParts,
        		DEFAULT_COLOURS, _locations == null ? 0 : _locations.length,
        														fontSize));
    }

    /*
     * Called when a menu is needed.
     */
    @Override
    public boolean
    onCreateOptionsMenu (
    	Menu		menu
    ) {
    	MenuItem	item;

    	item = menu.add (0, MENU_SETTINGS, 0, "Settings");
    	item.setIcon (android.R.drawable.ic_menu_preferences);

    	item = menu.add (0, MENU_MAP_MODE, 0, "Map mode");
    	item.setIcon (android.R.drawable.ic_menu_mapmode);

    	item = menu.add (0, MENU_ABOUT, 0, "About");
    	item.setIcon (android.R.drawable.ic_menu_info_details);

        return true;
    }

    /*
     * Called when a menu selection has been made.
     */
    @Override
    public boolean
    onOptionsItemSelected (
    	MenuItem	item
    ) {
    	super.onOptionsItemSelected (item);
    	
    	switch (item.getItemId ()) {
    		case MENU_SETTINGS:
    			showDialog (DIALOG_DISPLAY_UNITS);
    			return true;
    		case MENU_MAP_MODE:
    			showDialog (DIALOG_MAP_MODE);
    			return true;
    		case MENU_ABOUT:
    			showDialog (DIALOG_ABOUT);
    			return true;
    	}

    	return false;
    }

    /*
     * This is called when a dialog is requested.
     */
    @Override
    protected Dialog
    onCreateDialog (
    	int			id
    ) {
    	AlertDialog.Builder		builder = new AlertDialog.Builder (this);

    	if (id == DIALOG_ABOUT) {
    		builder.setTitle (R.string.about_title)
    			.setIcon (R.drawable.about_icon)
    			.setMessage (R.string.about_text)
    			.setPositiveButton ("OK", new DialogInterface.OnClickListener () {
    				public void onClick (DialogInterface dialog, int id) {
    					dialog.cancel ();
    				}
    			});
    	} else if (id == DIALOG_MAP_MODE) {
    		final CharSequence	items[] = {"Map", "Satellite"};
    		int					index = _mapView.isSatellite () ? 1 : 0;

    		builder.setTitle ("Map mode")
    			.setSingleChoiceItems (items, index,
    									new DialogInterface.OnClickListener () {
    				public void onClick (DialogInterface dialog, int item) {
    					_mapView.setSatellite (item == 1);
    					dialog.dismiss ();
    				}
    			});

    	} else {	// DIALOG_DISPLAY_UNITS
    		final CharSequence	items[] = {"Imperial", "Metric"};
    		int					index = DistanceUnits.useImperial () ? 0 : 1;

    		builder.setTitle ("Display units")
    			.setSingleChoiceItems (items, index,
    									new DialogInterface.OnClickListener () {
    				public void onClick (DialogInterface dialog, int item) {
    					DistanceUnits.setUseImperial (item == 0);
    					dialog.dismiss ();
    				}
    			});
    	}

    	return builder.create ();
    }

    /*
     * Switch between map and compass mode.
     */
    private void
    toggleMode () {
    	_useMapMode = !_useMapMode;
    	if (_useMapMode) {
    		enableCompassMode (false);
    		enableMapMode (true);
    	} else {
    		enableMapMode (false);
    		enableCompassMode (true);
    	}
    	_frame.invalidate ();

    	if (_useMapMode)
    		_switchButton.setImageResource (android.R.drawable.ic_menu_compass);
    	else
    		_switchButton.setImageResource (android.R.drawable.ic_menu_mapmode);
    	_switchButton.invalidate ();
    }

    /*
     * Called when the activity becomes active.
     */
    @Override
    public void
    onResume () {
    	super.onResume ();

    	if (_useMapMode)
    		enableMapMode (true);
    	else
    		enableCompassMode (true);
    }
    
    /*
     * Called when the activity is paused.
     */
    @Override
    public void
    onPause () {
    	super.onPause ();

    	if (_useMapMode)
    		enableMapMode (false);
    	else
    		enableCompassMode (false);
    }

    /*
     * Enable/disable map mode.
     */
    private void
    enableMapMode (
    	boolean		enable
    ) {
    	if (enable) {
    		_mapView.setVisibility (View.VISIBLE);
    		_myLocationOverlay.enableMyLocation ();
        	_myLocationOverlay.enableCompass ();
        	_targetOverlay.showLocation (Utility.bestKnownLocation (this));
    	} else {
    		_mapView.setVisibility (View.INVISIBLE);
    		_myLocationOverlay.disableMyLocation ();
        	_myLocationOverlay.disableCompass ();
    	}
    }

    /*
	 * Enable/disable compass mode.
	 */
	private void
	enableCompassMode (
		boolean		enable
	) {
		if (enable) {
			_compassView.setVisibility (View.VISIBLE);
			_compassView.setCurrentLocation (Utility.bestKnownLocation (this));

	    	_sensorManager.registerListener (_compassView,
					_sensorManager.getDefaultSensor (Sensor.TYPE_MAGNETIC_FIELD),
					SensorManager.SENSOR_DELAY_UI);
			_sensorManager.registerListener (_compassView,
					_sensorManager.getDefaultSensor (Sensor.TYPE_ACCELEROMETER),
					SensorManager.SENSOR_DELAY_UI);

			boolean		providerFound = false;

	    	if (_locationManager.isProviderEnabled (
	    									LocationManager.GPS_PROVIDER)) {
	    		_locationManager.requestLocationUpdates (
	    							LocationManager.GPS_PROVIDER, 0, 0, this);
	    		providerFound = true;
	    	}

	    	if (_locationManager.isProviderEnabled (
	    								LocationManager.NETWORK_PROVIDER)) {
	    		_locationManager.requestLocationUpdates (
	    						LocationManager.NETWORK_PROVIDER, 0, 0, this);
	    		providerFound = true;
	    	}

	    	if (!providerFound)
	    		Toast.makeText (this,
"Unable to determine your location.\nPlease enable GPS or network positioning.",
	    											Toast.LENGTH_LONG).show ();
		} else {
			_compassView.setVisibility (View.INVISIBLE);
			_sensorManager.unregisterListener (_compassView);
	    	_locationManager.removeUpdates (this);
		}
	}

    /*
     * This is required when MapActivity is the superclass.
     */
    @Override
    protected boolean
    isRouteDisplayed () {
        return false;
    }

    /*
     * Strip whitespace from the start and end of the string.
     */
    static private String
    stripWhitespace (
    	String		s
    ) {
    	int			leadingWhitespace = 0;
    	int			trailingWhitespace = 0;
    	int			i, n = s.length ();

    	for (i = 0; i < n; i++) {
    		if (s.charAt (i) == ' ')
    			leadingWhitespace++;
    		else
    			break;
    	}

    	if (leadingWhitespace == n)
    		return null;

    	for (i = 0; i < n; i++) {
    		if (s.charAt (n - i - 1) == ' ')
    			trailingWhitespace++;
    		else
    			break;
    	}

    	if (leadingWhitespace + trailingWhitespace == 0)
    		return s;
    	else
    		return s.substring (leadingWhitespace, n - trailingWhitespace);
    }

    /*
     * Parse the SMS message, extracting all GEO URIs and message components.
     */
    private void
    parseMessage (
    	String		msg
    ) {
    	int			numMessages = 0;
    	int			numLocations = 0;
    	String		messagePart = null;
    	String		messages[] = new String[160];
    	Location	locations[] = new Location[160];

    	while (msg.length () > 0) {
    		int			pos = msg.indexOf ("geo:");
    		String		s;

    		if (pos < 0) {		// No geo URI, so it's all message text.
    			s = stripWhitespace (msg);
    			if (s != null) {
    				if (messagePart == null)
    					messagePart = s;
    				else
    					messagePart += " " + s;
    			}
    			break;
    		} else {
    				// Message text before the URI
    			s = stripWhitespace (msg.substring (0, pos));
    			if (s != null) {
    				if (messagePart == null)
    					messagePart = s;
    				else
    					messagePart += " " + s;
    			}

    				// Extract the geo URI into its own string.
    			msg = msg.substring (pos);
    			pos = msg.indexOf (' ');

    			if (pos < 0) {
    				s = msg;
    				msg = "";
    			} else {
    				s = msg.substring (0, pos);
    				msg = msg.substring (pos + 1);
    			}

    			Location	loc = GeoUri.stringToLocation (s);

    			if (loc != null) {
    				messages[numMessages++] = messagePart;
    				locations[numLocations++] = loc;
    				messagePart = null;
    			} else {	// Not a valid URI, must be part of the message.
    				messagePart += " " + s;
    			}
    		}
    	}

    	if (messagePart != null)
    		messages[numMessages++] = messagePart;

    	if (numMessages > 0) {
    		_messageParts = new String[numMessages];
    		System.arraycopy (messages, 0, _messageParts, 0, numMessages);
    	}

    	if (numLocations > 0) {
    		_locations = new Location[numLocations];
    		System.arraycopy (locations, 0, _locations, 0, numLocations);
    	}
    }

    /*
     * Called when we get a location update.
     */
    @Override
    public void
    onLocationChanged (
    	Location	loc
    ) {
    	_compassView.setCurrentLocation (loc);
    }

	/*
	 * Called when a provider is enabled.
	 */
	@Override
    public void
    onProviderEnabled (
    	String		provider
    ) {
    }

	/*
	 * Called when a provider is disabled.
	 */
	@Override
    public void
    onProviderDisabled (
    	String		provider
    ) {
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
}