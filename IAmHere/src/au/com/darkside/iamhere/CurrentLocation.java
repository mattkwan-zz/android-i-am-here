/*
 * Request the current GPS and/or network location with a specified timeout.
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

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;

public class CurrentLocation extends CountDownTimer 
											implements LocationListener {
	private IAmHere			_iAmHere;
	private LocationManager	_locationManager;
	private Location		_bestLocation;
	private boolean			_waitingForGps = false;
	private boolean			_waitingForNetwork = false;

	/*
	 * Constructor.
	 */
	CurrentLocation (
		IAmHere				iAmHere,
		LocationManager		lm,
		int					timeout
	) {
		super (timeout, timeout);

		_iAmHere = iAmHere;
		_locationManager = lm;
	}

	/*
	 * Request the current GPS and/or network location.
	 * Specifies a timeout in milliseconds.
	 */
	public boolean
	requestLocation () {
		boolean		gpsEnabled = _locationManager.isProviderEnabled (
											LocationManager.GPS_PROVIDER);
		boolean		networkEnabled = _locationManager.isProviderEnabled (
											LocationManager.NETWORK_PROVIDER);

		if (!gpsEnabled && !networkEnabled)
			return false;

		_bestLocation = null;
		_waitingForGps = false;
		_waitingForNetwork = false;

		if (gpsEnabled) {
			_locationManager.requestLocationUpdates (
									LocationManager.GPS_PROVIDER, 0, 0, this);
			_waitingForGps = true;
		}

		if (networkEnabled) {
			_locationManager.requestLocationUpdates (
								LocationManager.NETWORK_PROVIDER, 0, 0, this);
			_waitingForNetwork = true;
		}

		start ();

		return true;
	}

	/*
     * Called when we get a location update.
     */
    @Override
    public void
    onLocationChanged (
    	Location	loc
    ) {
    	if (_bestLocation == null || !_bestLocation.hasAccuracy ()
    				|| (loc.hasAccuracy () && loc.getAccuracy ()
    										< _bestLocation.getAccuracy ()))
    		_bestLocation = loc;

    	if (loc.getProvider () == LocationManager.NETWORK_PROVIDER)
    		_waitingForNetwork = false;
    	else if (loc.getProvider () == LocationManager.GPS_PROVIDER
    					&& loc.hasAccuracy () && loc.getAccuracy () < 50)
    		_waitingForGps = false;		// Wait for a GPS reading within 50m.

    	if (!_waitingForGps && !_waitingForNetwork) {
    		cancel ();
    		_locationManager.removeUpdates (this);
    		_iAmHere.onLocationFound (_bestLocation);
    	}
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

	/*
	 * Tick callback for the countdown timer.
	 * Do nothing.
	 */
	@Override
	public void
	onTick (
		long		untilFinished
	) {
	}
	
	/*
	 * Timed out. Use whatever location data we have.
	 */
	@Override
	public void
	onFinish () {
		_locationManager.removeUpdates (this);
		_iAmHere.onLocationFound (_bestLocation);
	}
}
