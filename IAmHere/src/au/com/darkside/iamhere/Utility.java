/*
 * Assorted utility functions.
 *
 * Written by Matthew Kwan - August 2010.
 *
 * Copyright (c) 2010 Matthew Kwan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
package au.com.darkside.iamhere;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.provider.Contacts;

public class Utility {
	public static final String		MAPS_API_KEY =
							"0FbNa1kPAWp2yviNxYJm_o4kVTdlcomGd0X-nxw";

	/*
	 * Convert a phone number into a name from the contact database,
	 * if it exists.
	 */
	public static String
	getContactNameFromNumber (
		Context		context,
		String		number
	) {
		String		projection[] = new String[] {
			Contacts.Phones.DISPLAY_NAME, Contacts.Phones.NUMBER
		};
		Uri			uri = Uri.withAppendedPath (
										Contacts.Phones.CONTENT_FILTER_URL,
														Uri.encode (number));
		Cursor		c = context.getContentResolver ().query (uri, projection,
														null, null, null);
 
		if (c.moveToFirst ()) {
			String name = c.getString (c.getColumnIndex (
											Contacts.Phones.DISPLAY_NAME));
			return name;
		}

		return number;		// Return the original number if no match.
	}

	/*
     * Return the best currently-known location.
     */
    public static Location
    bestKnownLocation (
    	Context			context
    ) {
    	LocationManager	lm = (LocationManager) context.getSystemService (
    												Context.LOCATION_SERVICE);
    	Location		locGPS = lm.getLastKnownLocation (
    											LocationManager.GPS_PROVIDER);
    	Location		locNetwork = lm.getLastKnownLocation (
    										LocationManager.NETWORK_PROVIDER);

    	if (locNetwork == null)
    		return locGPS;
    	else if (locGPS == null)
    		return locNetwork;

    	if (locNetwork.getTime () > locGPS.getTime ())	// Use most recent.
    		return locNetwork;
    	else
    		return locGPS;
    }

    /*
     * Is the internet available?
     */
    public static boolean
    internetAvailable (
    	Context			context
    ) {
    	ConnectivityManager		cm;
    
    	cm = (ConnectivityManager) context.getSystemService (
    											Context.CONNECTIVITY_SERVICE);
    	if (cm == null)
    		return false;
    
    	return (cm.getActiveNetworkInfo () != null);
    }
}
