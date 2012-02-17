/*
 * This activity displays all the received messages in a scrolling list,
 * where they can be viewed or deleted.
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class ReceivedMessageViewer extends ListActivity
										implements OnItemClickListener {
	private Database	_database = null;
	private Cursor		_cursor = null;
	private long		_selectedMessageId = -1;

	private final static int	DIALOG_MESSAGE_SELECTED = 1;

    /*
     * Called when the activity is first created.
     */
    @Override
    public void
    onCreate (
    	Bundle		savedInstanceState
    ) {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.received_message_viewer);

        getListView ().setOnItemClickListener (this);
    }

    /*
     * Called when the activity becomes active.
     */
    @Override
    public void
    onResume () {
    	super.onResume ();

    	_database = new Database (this);
        if (_database.open ())
        	populate ();
        else
        	_database = null;
    }

    /*
     * Called when the activity pauses.
     */
    @Override
    public void
    onPause () {
    	super.onPause ();

    	if (_database != null) {
    		_database.close ();
    		_database = null;
    	}
    }

    /*
     * Populate the list.
     */
    private void
    populate () {
       	if ((_cursor = _database.cursor ()) != null) {
        	ListAdapter		adapter = new SimpleCursorAdapter (this,
        						android.R.layout.two_line_list_item, _cursor,
        						new String[] { "phone", "message" },
        						new int[] { android.R.id.text1, android.R.id.text2 });

        	setListAdapter (adapter);
       	}
    }

    /*
     * Delete the selected message.
     */
    private void
    deleteSelectedMessage () {
    	if (_selectedMessageId < 0 || _database == null)
    		return;

       	if (_database.deleteMessage (_selectedMessageId)) {
       		_selectedMessageId = -1;
       		if (_cursor != null)
       			_cursor.requery ();
        }
    }

    /*
     * View the selected message.
     */
    private void
    viewSelectedMessage () {
    	if (_selectedMessageId < 0)
    		return;

		Intent		intent = new Intent (this, GetMeThere.class);

		intent.putExtra (IAmHere.RECEIVED_SMS_ID, _selectedMessageId);
		startActivity (intent);
    }

    /*
     * Callback for creating the selected message confirmation dialog.
     */
    @Override
    protected Dialog
    onCreateDialog (
    	int			id
    ) {
    	AlertDialog.Builder		builder = new AlertDialog.Builder (this);

    	builder.setMessage ("Do you wish to view the message or delete it?")
    		.setPositiveButton ("View", new DialogInterface.OnClickListener () {
    			public void onClick (DialogInterface dialog, int id) {
    				viewSelectedMessage ();
    			}
    		})
    		.setNegativeButton ("Delete", new DialogInterface.OnClickListener () {
    			public void onClick (DialogInterface dialog, int id) {
    				deleteSelectedMessage ();
    			}
    		});
  
    	return builder.create();
    }

    /*
     * Called when an item is selected.
     */
    @Override
    public void
    onItemClick (
    	AdapterView<?>	parent,
    	View			v,
    	int				position,
    	long			id
    ) {
    	final Cursor	c = (Cursor) parent.getItemAtPosition (position);

    	if (c != null) {
    		_selectedMessageId = c.getLong (c.getColumnIndex ("_id"));
    		showDialog (DIALOG_MESSAGE_SELECTED);
    	}
    }
}
