/*
 * This class handles the storage and retrieval of records.
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

import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.telephony.SmsMessage;
import android.widget.Toast;

public class Database {
	private Context				_context;
	private SQLiteDatabase		_db;
	
	/*
	 * Constructor.
	 */
	public Database (
		Context		c
	) {
		_context = c;
		_db = null;
	}
	
	/*
	 * Open the database.
	 */
	public boolean
	open () {
		try {
			_db = _context.openOrCreateDatabase ("IAmHereDB",
												Context.MODE_PRIVATE, null);

			_db.execSQL ("CREATE TABLE IF NOT EXISTS received_sms ("
						+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
						+ "timestamp INTEGER NOT NULL, "
						+ "viewed INTEGER NOT NULL, "
						+ "phone VARCHAR NOT NULL, "
						+ "message VARCHAR NULL)");
		} catch (Exception e) {
			Toast.makeText (_context, e.getMessage (), Toast.LENGTH_LONG).show ();
			_db = null;
		}
		
		return (_db != null);
	}
	
	/*
	 * Close the database.
	 */
	public void
	close () {
		if (_db != null) {
			_db.close ();
			_db = null;
		}
	}

	/*
	 * Save the details of a received geo-tagged SMS, along with a time stamp.
	 */
	public long
	addRecord (
		SmsMessage		msg
	) {
		if (_db == null)
			return -1;

		ContentValues	cv = new ContentValues (4);
		Date			d = new Date ();
		long			ret = -1;

		cv.put ("timestamp", d.getTime ());
		cv.put ("viewed", 0);
		cv.put ("phone", msg.getOriginatingAddress ());
		cv.put ("message", msg.getMessageBody ());

		try {
			ret = _db.insertOrThrow ("received_sms", null, cv);
		} catch (Exception e) {
			Toast.makeText (_context, e.getMessage (), Toast.LENGTH_LONG).show ();
			return -1;
		}

		return ret;
	}

	/*
	 * Save the details of a self-sent geo-tagged SMS, along with a
	 * time stamp.
	 */
	public long
	addSelfSentRecord (
		String		msg
	) {
		if (_db == null)
			return -1;

		ContentValues	cv = new ContentValues (4);
		Date			d = new Date ();
		long			ret = -1;

		cv.put ("timestamp", d.getTime ());
		cv.put ("viewed", 0);
		cv.put ("phone", "Self");
		cv.put ("message", msg);

		try {
			ret = _db.insertOrThrow ("received_sms", null, cv);
		} catch (Exception e) {
			Toast.makeText (_context, e.getMessage (), Toast.LENGTH_LONG).show ();
			return -1;
		}

		return ret;
	}

	/*
	 * Load details of the specified message.
	 */
	public boolean
	getDetails (
		long		id,
		String		details[]
	) {
		if (_db == null)
			return false;

		Cursor		c;
		String		columns[] = new String [] {"phone", "message"};
		boolean		found = false;

		c = _db.query ("received_sms", columns, "_id=" + id, null, null,
																null, null);
		if (c != null) {
			if (c.moveToFirst ()) {
				details[0] = c.getString (c.getColumnIndex ("phone"));
				details[1] = c.getString (c.getColumnIndex ("message"));
				found = true;
			}
			
			c.close ();
		}

		return found;
	}

	/*
	 * Flag the specified message as read.
	 */
	public boolean
	flagAsRead (
		long		id
	) {
		try {
			_db.execSQL ("UPDATE received_sms SET viewed=1 WHERE _id=" + id);
		} catch (Exception e) {
			Toast.makeText (_context, e.getMessage (), Toast.LENGTH_LONG).show ();
			return false;
		}

		return true;
	}

	/*
	 * Delete the specified message.
	 */
	public boolean
	deleteMessage (
		long		id
	) {
		try {
			_db.delete ("received_sms", "_id=" + id, null);
		} catch (Exception e) {
			Toast.makeText (_context, e.getMessage (), Toast.LENGTH_LONG).show ();
			return false;
		}

		return true;
	}

	/*
	 * Return a cursor that iterates over all the received messages.
	 */
	public Cursor
	cursor () {
		if (_db == null)
			return null;

		return _db.query ("received_sms", null, null, null, null, null,
																"_id DESC");
	}
	
	/*
	 * Delete all the records.
	 */
	public boolean
	deleteAll ()
	{
		if (_db == null)
			return false;
		
		_db.delete ("received_sms", null, null);

		return true;
	}
}
