/*
 * This class displays one or more compass needles and distance values that
 * are updated in real time to show where targets are located.
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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.view.View;

public class CompassView extends View implements SensorEventListener {
	private boolean		_currentLocationKnown = false;
	private Location	_locations[] = null;
	private int			_colours[] = null;
	private float		_bearings[] = null;
	private float		_distances[] = null;
	private float		_rotations[] = null;
	private Bitmap		_bitmaps[] = null;
	private float		_bitmapCentreX = 0.0f;
	private float		_bitmapCentreY = 0.0f;
	private Paint		_paint = null;

	private float		_gravityMatrix[] = null;
	private float		_magneticMatrix[] = null;
	private float		_rMatrix[] = new float[9];
	private float		_angles[] = new float[3];

	/*
	 * Constructor.
	 */
	CompassView (
		Context		context,
		Location	locations[],
		int			colours[]
	) {
		super (context);

		if (locations == null || locations.length == 0)
			return;

		_paint = new Paint (Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

		int			i, n = locations.length;

		_locations = new Location[n];
		System.arraycopy (locations, 0, _locations, 0, n);
		_colours = new int[n];
		System.arraycopy (colours, 0, _colours, 0, n);
		_bearings = new float[n];
		_distances = new float[n];
		_rotations = new float[n];

		for (i = 0; i < n; i++) {
			_bearings[i] = 0.0f;
			_distances[i] = 0.0f;
			_rotations[i] = 0.0f;
		}
	}

	/*
	 * Set the current location.
	 */
	public void
	setCurrentLocation (
		Location	loc
	) {
		if (loc == null)
			return;

		int			i;

		for (i = 0; i < _locations.length; i++) {
			_bearings[i] = loc.bearingTo (_locations[i]);
			_distances[i] = loc.distanceTo (_locations[i]);
		}

		_currentLocationKnown = true;
	}

    /*
     * Called when the compass heading changes.
     */
    public void
    onSensorChanged (
    	SensorEvent		se
    ) {
    	if (se.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
    		return;

    	boolean			usefulEvent = false;

    	switch (se.sensor.getType()) {
    		case Sensor.TYPE_ACCELEROMETER:
    			if (_gravityMatrix == null)
    				_gravityMatrix = se.values.clone ();
    			else
    				System.arraycopy (se.values, 0, _gravityMatrix, 0,
    													se.values.length);
    			usefulEvent = true;
    			break;
    		case Sensor.TYPE_MAGNETIC_FIELD:
    			if (_magneticMatrix == null)
    				_magneticMatrix = se.values.clone ();
    			else
    				System.arraycopy (se.values, 0, _magneticMatrix, 0,
    													se.values.length);
    			usefulEvent = true;
    			break;
    	}
    	
    	if (!usefulEvent || _gravityMatrix == null || _magneticMatrix == null)
    		return;

    	if (!SensorManager.getRotationMatrix (_rMatrix, null, _gravityMatrix,
    														_magneticMatrix))
    		return;

        SensorManager.getOrientation (_rMatrix, _angles);

        float		heading = (float) Math.toDegrees (_angles[0]);
        boolean		changed = false;
        int			i;

        for (i = 0; i < _locations.length; i++) {
        	float		diff = _bearings[i] - heading - _rotations[i];

        	if (diff < -180.0)
        		diff += 360.0;
        	else if (diff > 180.0)
        		diff -= 360.0;

        	if (diff > -0.001 && diff < 0.001)
        		continue;

        	if (diff < -1.0 || diff > 1.0)
        		diff *= 0.2;	// Smooth out the rotation.

        	_rotations[i] += diff;
        	if (_rotations[i] < -180.0)
        		_rotations[i] += 360.0;
        	else if (_rotations[i] > 180.0)
        		_rotations[i] -= 360.0;

        	changed = true;
        }

        if (changed)
        	invalidate ();
    }

    /*
     * Mandatory function for SensorListener.
     */
    @Override
    public void
    onAccuracyChanged (
    	Sensor		s,
    	int			accuracy
    ) {
    }

	/*
	 * Create the bitmaps of the compass needles.
	 */
	private void
	createBitmaps (
		int			canvasWidth
	) {
		int			i, n = _locations.length;
		Bitmap		bm = BitmapFactory.decodeResource (getResources (),
													R.drawable.needle);
		double		scale = (double) canvasWidth
										/ (double) bm.getHeight () * 0.6;
		int			w = (int) (bm.getWidth () * scale);
		int			h = (int) (bm.getHeight () * scale);
		Bitmap		sbm = Bitmap.createScaledBitmap (bm, w, h, true);
		int			pixels[] = new int[w * h];

		_bitmapCentreX = w * 0.5f;
		_bitmapCentreY = h * 0.5f;
		sbm.getPixels (pixels, 0, w, 0, 0, w, h);

		_bitmaps = new Bitmap[n];
		for (i = 0; i < n; i++)
			_bitmaps[i] = createColouredBitmap (pixels, w, h, _colours[i]);
	}

	/*
	 * Create a copy of the bitmap, but colourized.
	 */
	private Bitmap
	createColouredBitmap (
		int			pixels[],
		int			width,
		int			height,
		int			colour
	) {
		int			i, n = pixels.length;
		int			npix[] = new int[n];
		double		rs = ((colour >> 16) & 0xff) / 255.0;
		double		gs = ((colour >> 8) & 0xff) / 255.0;
		double		bs = (colour & 0xff) / 255.0;

		for (i = 0; i < n; i++) {
			int			c = pixels[i];
			int			alpha = c & 0xff000000;

			if (alpha == 0) {	// Transparent.
				npix[i] = 0;
			} else {	// Apply the colour to the pixel.
				int			r = (int) (((c >> 16) & 0xff) * rs);
				int			g = (int) (((c >> 8) & 0xff) * gs);
				int			b = (int) ((c & 0xff) * bs);

				npix[i] = alpha | (r << 16) | (g << 8) | b;
			}
		}

		return Bitmap.createBitmap (npix, width, height,
												Bitmap.Config.ARGB_8888);
	}

    /*
	 * Called when the size changes.
	 */
	@Override
	protected void
	onSizeChanged (
		int			width,
		int			height,
		int			oldWidth,
		int			oldHeight
	) {
		super.onSizeChanged (width, height, oldWidth, oldHeight);

		_paint.setTextSize (width / 20);
		createBitmaps (width);
	}

	/*
	 * Draw the compasses and distances.
	 */
	@Override
	protected void
	onDraw (
		Canvas		canvas
	) {
		if (!_currentLocationKnown || _bitmaps == null)
			return;

		int			i;

		for (i = _locations.length - 1; i >= 0; i--)
			drawCompass (canvas, i);
	}

	/*
	 * Draw a compass that points to the specified location.
	 */
	private void
	drawCompass (
		Canvas		canvas,
		int			lid
	) {
		float		cx = getWidth () * 0.5f;
		float		cy = getHeight () * 0.5f;

		canvas.save (Canvas.MATRIX_SAVE_FLAG);
		canvas.rotate (_rotations[lid], cx, cy);
		canvas.drawBitmap (_bitmaps[lid], cx - _bitmapCentreX,
											cy - _bitmapCentreY, _paint);
		canvas.restore ();

			// Draw the distance indicator.
		float		dist = _distances[lid];
		String		s = DistanceUnits.distanceString (dist);
		Rect		rect = new Rect ();

		_paint.getTextBounds (s, 0, s.length (), rect);

		double		r = (getWidth () - rect.width () - 8) * 0.49;
		double		a = Math.toRadians (_rotations[lid]);
		float		x = cx - (float) (r * Math.sin (a)) - rect.exactCenterX ();
		float		y = cy + (float) (r * Math.cos (a)) - rect.exactCenterY ();

		rect.left += x - 4;
		rect.right += x + 4;
		rect.top += y - 4;
		rect.bottom += y + 4;
		_paint.setColor (0x80000000 | (_colours[lid] & 0xffffff));
		canvas.drawRect (rect, _paint);

		_paint.setColor (0xffffffff);
		canvas.drawText (s, x, y, _paint);
	}
}
