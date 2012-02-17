/*
 * Present metre distances in a reader-friendly format,
 * converting to imperial units if necessary.
 *
 * Written by Matthew Kwan - October 2010
 *
 * Copyright (c) 2010 Matthew Kwan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
package au.com.darkside.iamhere;

import android.content.Context;
import android.content.SharedPreferences;

public class DistanceUnits {
	public static boolean		_useImperial = false;
	public static Context		_context = null;

	/*
	 * Initialize the units we are using.
	 */
	public static void
	initialize (
		Context		c
	) {
		SharedPreferences	prefs = c.getSharedPreferences ("IAmHerePrefs",
														Context.MODE_PRIVATE);

		_useImperial = prefs.getBoolean ("use_imperial", false);
		_context = c;
	}

	/*
	 * Are we currently using imperial units?
	 */
	public static boolean
	useImperial () {
		return _useImperial;
	}

	/*
	 * Switch to/from imperial units.
	 */
	public static void
	setUseImperial (
		boolean		flag
	) {
		_useImperial = flag;

		if (_context == null)
			return;

		SharedPreferences			prefs = _context.getSharedPreferences (
										"IAmHerePrefs", Context.MODE_PRIVATE);
		SharedPreferences.Editor	editor = prefs.edit ();

		editor.putBoolean ("use_imperial", flag);
		editor.commit ();
	}

	/*
	 * Return a string representing the distance.
	 */
	public static String
	distanceString (
		double		metres
	) {
		if (_useImperial) {
			double		feet = metres / 0.3048;

			if (feet < 300.0)
				return String.valueOf ((int) feet) + "ft";
			else if (feet < 1320)
				return String.valueOf ((int) (feet / 3)) + "yd";
			else if (feet < 52800.0)
				return String.valueOf (((int) (feet / 528)) / 10.0) + "mi";
			else
				return String.valueOf ((int) (feet / 5280.0)) + "mi";
		} else {
			if (metres < 1000.0)
				return String.valueOf ((int) metres) + "m";
			else if (metres < 10000.0)
				return String.valueOf (((int) (metres / 100)) / 10.0) + "km";
			else
				return String.valueOf ((int) (metres / 1000.0)) + "km";
		}
	}

	/*
	 * Return a string representing the accuracy.
	 */
	public static String
	accuracyString (
		double		metres
	) {
		if (_useImperial) {
			double		feet = metres / 0.3048;

			if (feet < 300.0)
				return String.valueOf ((int) feet) + " feet";
			else if (feet < 5280.0)
				return String.valueOf ((int) (feet / 3.0)) + " yards";
			else
				return String.valueOf ((int) (feet / 5280.0)) + " miles";
		} else
			return String.valueOf ((int) metres) + " metres";
	}
}
