/*
 * This GUI allows you to select a location from a map.
 *
 * Written by Matthew Kwan - August 2009
 *
 * Copyright (c) 2010 Matthew Kwan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package au.com.darkside.iamhere;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;

import com.google.android.maps.MapActivity;

public class MapSelector extends MapActivity {
	private MapImageView		_mapImageView;
	private CheckBox			_satelliteCheckBox;

    /*
     * Called when the activity is first created.
     */
    @Override
    public void
    onCreate (
    	Bundle		savedInstanceState
    ) {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.map_selector);

        FrameLayout		fl = (FrameLayout) findViewById (R.id.map_frame);

        _mapImageView = new MapImageView (this);
        fl.addView (_mapImageView);

        Button			b;

        b = (Button) findViewById (R.id.button_ok);
        b.setOnClickListener (
            new View.OnClickListener () {
                public void	onClick (View v) {
                	returnLocation ();
                }
            }
        );

        _satelliteCheckBox = (CheckBox) findViewById (R.id.satellite_checkbox);
        _satelliteCheckBox.setOnCheckedChangeListener (
        	new CompoundButton.OnCheckedChangeListener () {
           		public void onCheckedChanged (
            		CompoundButton	b,
            		boolean			checked
            	) {
            		_mapImageView.setSatellite (checked);
            	}
        	}
        );
    }

    /*
     * Called when the application is started.
     */
    @Override
    public void
    onStart () {
    	super.onStart ();

    	_mapImageView.setSatellite (_satelliteCheckBox.isChecked ());

        Location		loc = Utility.bestKnownLocation (this);

    	if (loc != null)
    		_mapImageView.showLocation (loc);
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
     * Return the location currently displayed by the MapView and finish.
     */
    private void
    returnLocation () {
    	Intent		intent = new Intent ();

    	intent.putExtra (IAmHere.LOCATION_DATA, _mapImageView.getLocation ());
    	setResult (RESULT_OK, intent);
    	finish ();
    }
}