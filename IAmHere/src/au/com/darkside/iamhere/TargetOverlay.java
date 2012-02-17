/*
 * This class implements a map overlay that displays the destination targets.
 *
 * Written by Matthew Kwan - August 2010
 *
 * Copyright (c) 2010 Matthew Kwan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
package au.com.darkside.iamhere;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Location;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class TargetOverlay extends Overlay {
	private MapView		_mapView;
	private Paint		_paint;
	private Location	_locations[];
	private int			_colours[];

	private static final double		METRES_PER_DEGREE = 40075017.0 / 360.0;

	/*
	 * Constructor.
	 */
	TargetOverlay (
		MapView		mapView,
		Location	locations[],
		int			colours[]
	) {
		_mapView = mapView;
		_paint = new Paint (Paint.ANTI_ALIAS_FLAG);

		if (locations == null || locations.length == 0)
			return;

		int			n = locations.length;

		_locations = new Location[n];
		System.arraycopy (locations, 0, _locations, 0, n);
		_colours = new int[n];
		System.arraycopy (colours, 0, _colours, 0, n);
	}

	/*
	 * Update the bounds to encompass the location.
	 */
	private void
	updateBounds (
		Location	loc,
		boolean		initialized,
		double		bounds[]
	) {
		double		lat = loc.getLatitude ();
		double		lon = loc.getLongitude ();
		double		laterr, lonerr;

		if (loc.hasAccuracy ()) {
			laterr = loc.getAccuracy () / METRES_PER_DEGREE;
			lonerr = laterr / Math.cos (Math.toRadians (lat));
		} else {
			laterr = 0.0;
			lonerr = 0.0;
		}

		if (!initialized) {
			bounds[0] = lat - laterr;
			bounds[1] = lat + laterr;
			bounds[2] = lon - lonerr;
			bounds[3] = lon + lonerr;
		} else {
			if (lat - laterr < bounds[0])
				bounds[0] = lat - laterr;
			if (lat + laterr > bounds[1])
				bounds[1] = lat + laterr;
			if (lon - lonerr < bounds[2])
				bounds[2] = lon - lonerr;
			if (lon + lonerr > bounds[3])
				bounds[3] = lon + lonerr;
		}
	}

	/*
	 * Ensure that the specified location and all targets are visible.
	 */
	public void
	showLocation (
		Location	myloc
	) {
		boolean		initialized = false;
		double		bounds[] = new double[4];
		int			i, n = _locations.length;

		if (myloc != null) {
			updateBounds (myloc, initialized, bounds);
			initialized = true;
		}

		for (i = 0; i < n; i++) {
			Location	loc = _locations[i];

			updateBounds (loc, initialized, bounds);
			initialized = true;
		}

		if (!initialized)
			return;

		MapController	mc = _mapView.getController ();

		mc.animateTo (new GeoPoint ((int) ((bounds[0] + bounds[1]) * 500000.0),
									(int) ((bounds[2] + bounds[3]) * 500000)));

		double		cosLat = Math.cos (Math.toRadians ((bounds[0]
		      		                                     + bounds[1]) * 0.5));
		double		mlat = (bounds[1] - bounds[0]) * METRES_PER_DEGREE;
		double		mlon = (bounds[3] - bounds[2]) * METRES_PER_DEGREE * cosLat;
		int			zoomLevel = 21;

		if (mlat != 0.0 || mlon != 0.0) {
								// Metres per pixel at zoom level 1.
			final double	metresPerPixel = METRES_PER_DEGREE * 360.0 / 256;
			double			w = _mapView.getWidth () * metresPerPixel;
			double			h = _mapView.getHeight () * metresPerPixel;

			zoomLevel = 1;
			while (w > mlon * 3.0 && h > mlat * 3.0) {
				w *= 0.5;
				h *= 0.5;

				if (++zoomLevel == 21)
					break;
			}
		}

		mc.setZoom (zoomLevel);
	}

	/*
	 * Draw an individual target.
	 */
	private void
	drawTarget (
		Canvas		canvas,
		Projection	proj,
		Location	loc,
		int			colour
	) {
		Point		p = proj.toPixels (new GeoPoint (
								(int) (loc.getLatitude () * 1.0e6),
								(int) (loc.getLongitude() * 1.0e6)), null);
		float		pixlen = 0.0f;

		if (loc.hasAccuracy ())
			pixlen = proj.metersToEquatorPixels (loc.getAccuracy ());

		if (pixlen > 6.0f) {	// Draw the area of uncertainty.
			_paint.setColor (colour | 0xf0000000);
			_paint.setStyle (Paint.Style.STROKE);
			canvas.drawCircle (p.x, p.y, pixlen, _paint);

			_paint.setColor (colour | 0x60000000);
			_paint.setStyle (Paint.Style.FILL);
			canvas.drawCircle (p.x, p.y, pixlen, _paint);
		}

			// Draw the target dot.
		_paint.setColor (colour | 0xf0000000);
		canvas.drawCircle (p.x, p.y, 5.0f, _paint);
	}

	/*
	 * Draw the targets.
	 */
	@Override
	public void
	draw (
		Canvas		canvas,
		MapView		mapView,
		boolean		shadow
	) {
		if (shadow)
			return;

		int			i;

		for (i = 0; i < _locations.length; i++)
			drawTarget (canvas, mapView.getProjection (), _locations[i],
																_colours[i]);
	}
}
