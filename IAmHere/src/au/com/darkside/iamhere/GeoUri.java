/*
 * This class encodes/decodes geospatial locations as text according RFC 5870.
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

public class GeoUri {
	/*
	 * Return the location encoded as an RFC 5870 URI.
	 */
	public static String
	locationToString (
		final Location	loc
	) {
		String			s = "geo:";

		s += doubleToString (loc.getLatitude (), 6) + ","
						+ doubleToString (loc.getLongitude (), 6);
		
		if (loc.hasAltitude ())
			s += "," + doubleToString (loc.getAltitude (), 3);
		
		if (loc.hasAccuracy ())
			s += ";u=" + doubleToString (loc.getAccuracy (), 3);

		return s;
	}

	/*
	 * Return a real number as a string, with a maximum number of
	 * decimal places.
	 */
	private static String
	doubleToString (
		double		d,
		int			decimalPlaces
	) {
		String		ret;

		if (d < 0.0) {
			d = -d;
			ret = "-";
		} else
			ret = "";

		String		s = String.valueOf ((int) Math.round (d * Math.pow (10,
															decimalPlaces)));

		if (s == "0")
			return s;

		while (s.length () < decimalPlaces + 1)
			s = "0" + s;

		ret += s.substring (0, s.length() - decimalPlaces);	// Integer component.
		s = s.substring (s.length () - decimalPlaces);	// Fractional component.

		while (s.length () > 0 && s.charAt (s.length () - 1) == '0')
			s = s.substring(0, s.length () - 1);	// Strip trailing zeroes.

		if (s.length () > 0)
			ret += "." + s;		// Append decimal places, if any.

		return ret;
	}

	/*
	 * Parse an RFC 5870 URI string representation of a location.
	 * geo:lat,lon[,alt][;u=accuracy]
	 */
	public static Location
	stringToLocation (
		String		sloc
	) {
		String		s = sloc.toLowerCase ();

		if (!s.startsWith ("geo:") || s.length () < 5)
			return null;

		String		sa[] = s.substring (4).split (";");
		Location	loc = parseLatLongAlt (sa[0]);
		int			i;

		if (loc == null)
			return null;

		for (i = 1; i < sa.length; i++)
			addLocationAttribute (loc, sa[i]);

		return loc;
	}

	/*
	 * Parse a comma-separated latitude, longitude, and optional altitude.
	 * Return the result in a Location.
	 */
	private static Location
	parseLatLongAlt (
		String		s
	) {
		String		sa[] = s.split (",");

		if (sa.length < 2 || sa.length > 3)
			return null;

		try {
			Location	loc = new Location ("GeoUri");

			loc.setLatitude (Double.valueOf (sa[0]));
			loc.setLongitude (Double.valueOf (sa[1]));

			if (sa.length == 3)
				loc.setAltitude (Double.valueOf (sa[2]));

			return loc;
		} catch (Exception e) {
			return null;
		}
	}

	/*
	 * Parse an location attribute and add it to the location.
	 */
	private static boolean
	addLocationAttribute (
		Location	loc,
		String		attr
	) {
		int			pos = attr.indexOf ('=');

		if (pos < 0)
			return false;

		String		s = attr.substring (0, pos);

		if (s.equals ("u")) {
			try {
				loc.setAccuracy (Float.valueOf (attr.substring (pos + 1)));
				return true;
			} catch (Exception e) {
				return false;
			}
		}

		return false;
	}
}
