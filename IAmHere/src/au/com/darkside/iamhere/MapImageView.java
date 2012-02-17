/*
 * Draw a Google map with crosshairs.
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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.location.Location;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;

public class MapImageView extends MapView {
	private Bitmap		_crosshairsBitmap;

	/*
	 * Constructor.
	 */
	public MapImageView (
		Context			c
	) {
		super (c, Utility.MAPS_API_KEY);

		_crosshairsBitmap = BitmapFactory.decodeResource (getResources (),
													R.drawable.crosshairs);

		setBuiltInZoomControls (true);
		setClickable (true);
		setEnabled (true);
	}

	/*
	 * Show the location.
	 */
	public void
	showLocation (
		Location		loc
	) {
		MapController	mc = getController ();
		int				iLat = (int) (loc.getLatitude () * 1.0e6);
		int				iLong = (int) (loc.getLongitude () * 1.0e6);

		mc.animateTo (new GeoPoint (iLat, iLong));
		mc.setZoom (16);
		invalidate ();
	}

	/*
	 * Called when the map is drawn.
	 * Overlay some crosshairs on the centre.
	 */
	@Override
	public void
	dispatchDraw (
		Canvas		canvas
	) {
		super.dispatchDraw (canvas);

		float		x = (getWidth () - _crosshairsBitmap.getWidth ()) * 0.5f;
		float		y = (getHeight () - _crosshairsBitmap.getHeight ()) * 0.5f;

		canvas.drawBitmap (_crosshairsBitmap, x, y, null);
	}

	/*
	 * Get the location of the centre of the map.
	 */
	public Location
	getLocation () {
		Location		loc = new Location ("MapSelector");
		GeoPoint		gp = getMapCenter ();

		loc.setLatitude (gp.getLatitudeE6() / 1.0e6);
		loc.setLongitude (gp.getLongitudeE6() / 1.0e6);

				// Accuracy is the width of two pixels.
		loc.setAccuracy ((float) (2 * Math.ceil ((313093.75 / Math.pow (2.0,
														getZoomLevel ())))));

		return loc;
	}
}
