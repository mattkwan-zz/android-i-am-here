/*
 * This application allows the user to specify a location and send it by SMS.
 *
 * Written by Matthew Kwan - July 2010
 * Launch icon designed by Reuben Stanton http://www.absentdesign.com - August 2010
 *
 * Copyright (c) 2010 Matthew Kwan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
package au.com.darkside.iamhere;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class IAmHere extends Activity {
	private TextView			_counterText;
	private EditText			_messageEditor;
	private Button				_sendButton;
	private CheckBox			_selfCheckBox;
	private ProgressDialog		_progressDialog;
	private CurrentLocation		_currentLocation;

	private static final int	ACTIVITY_LOCATION_SELECT = 1;
	private static final int	DIALOG_MESSAGE_TOO_LONG = 1;
	private static final int	DIALOG_NO_LOCATION = 2;
	private static final int	DIALOG_QUERYING_LOCATION = 3;
	private static final int	DIALOG_EMPTY_MESSAGE = 4;
	private static final int	DIALOG_DISPLAY_UNITS = 5;
	private static final int	DIALOG_ABOUT = 6;
	private static final int	MENU_VIEW_RECEIVED = 1;
	private static final int	MENU_SETTINGS = 2;
	private static final int	MENU_ABOUT = 3;

	public static final String	LOCATION_DATA =
									"au.com.darkside.iamhere.LocationData";
	public static final String	RECEIVED_SMS_ID =
									"au.com.darkside.iamhere.ReceivedSmsId";

    /*
     * Called when the activity is first created.
     */
    @Override
    public void
    onCreate (
    	Bundle		savedInstanceState
    ) {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.main);

        _currentLocation = new CurrentLocation (this,
        			(LocationManager) getSystemService (LOCATION_SERVICE), 5000);
        _counterText = (TextView) findViewById (R.id.counter_text);
        _selfCheckBox = (CheckBox) findViewById (R.id.self_checkbox);
        _progressDialog = null;

        _messageEditor = (EditText) findViewById (R.id.message_editor);
        _messageEditor.addTextChangedListener (
        	new TextWatcher () {
        		public void afterTextChanged (Editable s) {
        			updateState ();
        		}
        		public void beforeTextChanged (CharSequence s, int start,
        										int count, int after) {
        		}
        		public void onTextChanged (CharSequence s, int start,
        										int before, int count) {
        		}
        	}
        );

        Button		b = (Button) findViewById (R.id.get_location_button);

        b.setOnClickListener (
        	new View.OnClickListener () {
        		public void	onClick (View v) {
        			launchLocationSelector ();
        		}
        	}
        );

        _sendButton = (Button) findViewById (R.id.send_button);
        _sendButton.setOnClickListener (
        	new View.OnClickListener () {
        		public void	onClick (View v) {
        			addLocationAndSend ();
        		}
        	}
        );

        updateState ();
        DistanceUnits.initialize (this);
    }

    /*
     * Called when the activity starts.
     */
    @Override
    public void
    onStart () {
    	super.onStart ();

    	SharedPreferences	prefs = getSharedPreferences ("IAmHerePrefs", MODE_PRIVATE);

    		// Show the About screen as a splash screen the first time the application runs.
    	if (!prefs.getBoolean ("splash_shown", false)) {
    		launchAboutScreen ();

    		SharedPreferences.Editor	editor = prefs.edit ();

        	editor.putBoolean ("splash_shown", true);
            editor.commit ();
    	}
    }

    /*
     * Update the header text and button sensitivity based on the contents of
     * the message editor.
     */
    private void
    updateState () {
    	String	s = _messageEditor.getText ().toString ();
    	int		length = s.length ();

    	_counterText.setText (String.valueOf (length));
    	_counterText.setTextColor (length > 160 ? 0xffff0000 : 0xffe0e0e0);

    	_sendButton.setEnabled (length <= 160);

    	if (s.contains ("geo:"))
    		_sendButton.setText ("Send");
    	else
    		_sendButton.setText ("Send with current location");
    }

    /*
     * This is called when a dialog is requested.
     */
    @Override
    protected Dialog
    onCreateDialog (
    	int			id
    ) {
    	if (id == DIALOG_QUERYING_LOCATION) {
    		_progressDialog = new ProgressDialog (this);
    		_progressDialog.setMessage ("Querying current location ...");
            _progressDialog.setIndeterminate (true);

            return _progressDialog;
    	}

    	AlertDialog.Builder		builder = new AlertDialog.Builder (this);
    	String					message;

    	switch (id) {
    		case DIALOG_MESSAGE_TOO_LONG:
    			message = "Message is too long for SMS.\n\n"
    					+ "Please reduce it to 160 characters or less.";
    			break;
    		case DIALOG_NO_LOCATION:
    			message = "Couldn't get current location.\n\nTry adding it manually.";
    			break;
    		case DIALOG_EMPTY_MESSAGE:
    			message = "You are about to send an SMS that will contain a location but no message.\n\n"
    					+ "Do you wish to continue?";
    			break;
    		default:
    			message = "Internal error";
    			break;
    	}

    	if (id == DIALOG_EMPTY_MESSAGE) {
    		builder.setMessage (message)
    			.setPositiveButton ("OK", new DialogInterface.OnClickListener () {
    				public void onClick (DialogInterface dialog, int id) {
    					sendEmptyMessage ();
    				}
    			})
    			.setNegativeButton ("Cancel", new DialogInterface.OnClickListener () {
    				public void onClick (DialogInterface dialog, int id) {
    					dialog.cancel ();
    				}
    			});
    	} else if (id == DIALOG_DISPLAY_UNITS) {
    		final CharSequence	items[] = {"Imperial", "Metric"};
    		int					index = DistanceUnits.useImperial () ? 0 : 1;

    		builder.setTitle ("Display units")
    			.setSingleChoiceItems (items, index, new DialogInterface.OnClickListener () {
    				public void onClick (DialogInterface dialog, int item) {
    					DistanceUnits.setUseImperial (item == 0);
    					dialog.dismiss ();
    				}
    			});
    	} else if (id == DIALOG_ABOUT) {
    		builder.setTitle (R.string.about_title)
    			.setIcon (R.drawable.about_icon)
    			.setMessage (R.string.about_text)
    			.setPositiveButton ("OK", new DialogInterface.OnClickListener () {
    				public void onClick (DialogInterface dialog, int id) {
    					dialog.cancel ();
    				}
    			});
    	} else {
    		builder.setMessage (message)
    			.setPositiveButton ("OK", new DialogInterface.OnClickListener () {
    				public void onClick (DialogInterface dialog, int id) {
    					dialog.cancel ();
    				}
    			});
    	}

    	return builder.create();
    }

    /*
     * Dispatch the message by SMS, or save it locally if it's being sent to self.
     */
    private void
    dispatchMessage (
    	String		msg
    ) {
    	if (_selfCheckBox.isChecked ()) {
    		Database		db = new Database (this);
    		
    		if (db.open ()) {
    			db.addSelfSentRecord (msg);
    			db.close ();
    			Toast.makeText (this, "Message successfully sent to self", Toast.LENGTH_LONG).show ();
    		}
    	} else
    		launchSmsSender (msg);
    }

    /*
     * Indicate whether the application is busy.
     */
    private void
    showBusy (
    	boolean		busy
    ) {
    	if (busy)
    		showDialog (DIALOG_QUERYING_LOCATION);
    	else if (_progressDialog != null)
    		_progressDialog.cancel ();
    }

    /*
     * Launch the location selector.
     */
    private void
    launchLocationSelector () {
		startActivityForResult (new Intent (this, LocationSelector.class),
													ACTIVITY_LOCATION_SELECT);
    }

    /*
     * Launch the SMS sender with the current message.
     */
    private void
    launchSmsSender (
    	String		msg
    ) {
    	Intent		intent = new Intent (Intent.ACTION_VIEW);

    	intent.putExtra ("sms_body", msg); 
    	intent.setType ("vnd.android-dir/mms-sms");
        startActivity (intent);
    }

    /*
     * Launch the received message viewer.
     */
    private void
    launchViewReceivedMessages () {
    	startActivity (new Intent (this, ReceivedMessageViewer.class));
    }

    /*
     * Launch the About screen.
     */
    private void
    launchAboutScreen () {
    	showDialog (DIALOG_ABOUT);
    }

    /*
     * Add the current location to the message and send it off.
     */
    private void
    addLocationAndSend () {
    	String		s = _messageEditor.getText ().toString ();

    	if (s.equals ("")) {
    		showDialog (DIALOG_EMPTY_MESSAGE);
    	} else if (s.contains ("geo:")) {
    		if (s.length () <= 160)
    			dispatchMessage (s);
    		else
    			showDialog (DIALOG_MESSAGE_TOO_LONG);
    	} else {
    		if (_currentLocation.requestLocation ())
    			showBusy (true);
    		else
    			showDialog (DIALOG_NO_LOCATION);
    	}
    }

    /*
     * Add the current location to an empty message and send it.
     */
    private void
    sendEmptyMessage () {
    	if (_currentLocation.requestLocation ())
			showBusy (true);
		else
			showDialog (DIALOG_NO_LOCATION);
    }
    
    /*
     * Called when the CurrentLocation class finds a location.
     */
    public void
    onLocationFound (
    	Location	loc
    ) {
    	showBusy (false);

    	if (loc == null) {
    		showDialog (DIALOG_NO_LOCATION);
    	} else {
    		appendLocation (loc);

    		String		s = _messageEditor.getText ().toString ();

    		if (s.length() <= 160)
    			dispatchMessage (s);
    		else
    			showDialog (DIALOG_MESSAGE_TOO_LONG);
    	}
    }

    /*
     * Append the location as a geo URI to the message being composed.
     */
    private void
    appendLocation (
    	Location	loc
    ) {
    	if (loc == null)
    		return;

    	String		s = _messageEditor.getText ().toString ();
    	String		geo = GeoUri.locationToString (loc);

    	if (s == null || s.length () == 0)
    		s = geo;
    	else if (s.charAt (s.length () - 1) == ' ')
    		s += geo;
    	else
    		s += " " + geo;

    	_messageEditor.setText (s);
    	updateState ();
    }

    /*
     * Handle return values from activities.
     */
    @Override
    protected void
    onActivityResult (
    	int			requestCode,
    	int			resultCode,
    	Intent		intent
    ) {
    	super.onActivityResult (requestCode, resultCode, intent);

    	if (resultCode == RESULT_CANCELED)
    		return;

    	if (requestCode == ACTIVITY_LOCATION_SELECT) {
    		try {
    			appendLocation ((Location) intent.getExtras().get (LOCATION_DATA));
    		} catch (Exception e) {
    		}
    	}
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

    	item = menu.add (0, MENU_VIEW_RECEIVED, 0, "View messages");
    	item.setIcon (android.R.drawable.ic_menu_view);

    	item = menu.add (0, MENU_SETTINGS, 0, "Settings");
    	item.setIcon (android.R.drawable.ic_menu_preferences);

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
    		case MENU_VIEW_RECEIVED:
    			launchViewReceivedMessages ();
    			return true;
    		case MENU_SETTINGS:
    			showDialog (DIALOG_DISPLAY_UNITS);
    			return true;
    		case MENU_ABOUT:
    			launchAboutScreen ();
    			return true;
    	}

    	return false;
    }
}