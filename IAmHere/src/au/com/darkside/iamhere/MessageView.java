/*
 * This class displays a geo-tagged SMS using coloured circles to indicate
 * locations.
 * 
 * Written by Matthew Kwan - May 2010
 *
 * Copyright (c) 2010 Matthew Kwan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package au.com.darkside.iamhere;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;

public class MessageView extends View {
	private String		_phone;
	private String		_messageParts[] = null;
	private int			_colours[] = null;
	private Paint		_paint;
	private float		_x = 0.0f;
	private float		_y = 0.0f;
	private float		_rowHeight;
	private float		_wordSeparator;
	private float		_circleDiameter;

	/*
	 * Constructor.
	 */
	public MessageView (
		Context			c,
		String			phone,
		String			messageParts[],
		int				colours[],
		int				numColours,
		float			fontSize
	) {
		super (c);

		setLayoutParams (new ViewGroup.LayoutParams (
									ViewGroup.LayoutParams.FILL_PARENT,
									ViewGroup.LayoutParams.FILL_PARENT));

		_phone = phone;

		int			n = (messageParts == null) ? 0 : messageParts.length;

		if (n > 0) {
			_messageParts = new String[n];
			System.arraycopy (messageParts, 0, _messageParts, 0, n);
		}

		if (numColours > 0) {
			_colours = new int[numColours];
			System.arraycopy (colours, 0, _colours, 0, numColours);
		}

		_paint = new Paint (Paint.ANTI_ALIAS_FLAG);
		_paint.setTextSize (fontSize);
		_rowHeight = _paint.getFontSpacing ();

		float		widths[] = new float[1];

		_paint.getTextWidths (" ", widths);
		_wordSeparator = widths[0];
		_circleDiameter = 2.0f * (float) Math.floor (fontSize / 3.0);
	}
	
	/*
	 * Called when the image needs drawing.
	 */
	@Override
	protected void
	onDraw (
		Canvas		canvas
	) {
		_x = 0.0f;
		_y = 0.0f;

		if (_phone != null)
			drawPhone (canvas, _phone);

		int			i;
 
		for (i = 0; i < _messageParts.length || i < _colours.length; i++) {
			if (i < _messageParts.length && _messageParts[i] != null)
				drawMessageText (canvas, _messageParts[i]);
			if (i < _colours.length)
				drawCircle (canvas, _colours[i], _circleDiameter);
		}
	}

	/*
	 * Draw the phone number in bold text.
	 */
	private void
	drawPhone (
		Canvas		canvas,
		String		s
	) {
		Rect		rect = new Rect ();

		_paint.setTypeface (Typeface.DEFAULT_BOLD);
		_paint.getTextBounds (s, 0, s.length (), rect);
		_paint.setColor (0xffe0e0e0);

		canvas.drawText (s, 0.0f, _rowHeight, _paint);
		_x = rect.width () + _wordSeparator;
	}

	/*
	 * Draw the text of a word.
	 */
	private void
	drawWordText (
		Canvas		canvas,
		String		s
	) {
		Rect		rect = new Rect ();

		_paint.setTypeface (Typeface.DEFAULT);
		_paint.getTextBounds(s, 0, s.length (), rect);

		float		textWidth = rect.width ();

		if (_x > 0.0f && _x + textWidth + _wordSeparator > getWidth ()) {
			_x = 0.0f;
			_y += _rowHeight;
		}

		float		separator = (_x == 0.0f) ? 0.0f : _wordSeparator;

		_paint.setColor (0xffe0e0e0);
		canvas.drawText (s, _x + separator, _y + _rowHeight, _paint);
		_x += separator + textWidth;
	}

	/*
	 * Draw the message text.
	 */
	private void
	drawMessageText (
		Canvas		canvas,
		String		s
	) {

		String		sa[] = s.split (" ");
		int			i;

		for (i = 0; i < sa.length; i++)
			drawWordText (canvas, sa[i]);
	}

	/*
	 * Draw a coloured circle with a radius of 5 pixels.
	 */
	private void
	drawCircle (
		Canvas		canvas,
		int			colour,
		float		diameter
	) {
		float		radius = diameter * 0.5f;

		if (_x + diameter + _wordSeparator > getWidth ()) {
			_x = 0.0f;
			_y += _rowHeight;
		}

		_paint.setColor (colour | 0xff000000);
		canvas.drawCircle (_x + _wordSeparator + radius,
								_y + _rowHeight - radius, radius, _paint);
		_x += _wordSeparator + diameter;
	}
}
