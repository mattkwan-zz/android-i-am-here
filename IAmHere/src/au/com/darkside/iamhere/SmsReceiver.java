/*
 * This class is called whenever an SMS is received.
 * If the SMS contains a geo tag, the GetMeThere activity is launched.
 *
 * Written by Matthew Kwan - July 2010.
 *
 * Copyright (c) 2010 Matthew Kwan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
package au.com.darkside.iamhere;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

public class SmsReceiver extends BroadcastReceiver {
	/*
	 * Called when an SMS is received.
	 */
	@Override
    public void
    onReceive (
    	Context		context,
    	Intent		intent
    ) {
        Bundle 		bundle = intent.getExtras ();        

        if (bundle != null) {
            Object[]		pdus = (Object[]) bundle.get ("pdus");
            int				i;

            for (i = 0; i < pdus.length; i++) {
            	SmsMessage		msg = SmsMessage.createFromPdu ((byte[])
            													pdus[i]);

            	if (containsGeoUri (msg))
            		saveAndNotify (context, msg);
            }
        }                         
    }

	/*
	 * Does the SMS message contain a valid geo URI?
	 */
	private boolean
	containsGeoUri (
		final SmsMessage	msg
	) {
		String		s = msg.getMessageBody ();

		if (s == null)
			return false;

		int			pos = s.indexOf ("geo:");

		if (pos < 0)
			return false;

		s = s.substring (pos);
		if ((pos = s.indexOf (' ')) > 0)
			s = s.substring (0, pos);
		
		return (GeoUri.stringToLocation(s) != null);
	}

	/*
	 * Save the contents of the message in a database and notify the user.
	 */
	private void
	saveAndNotify (
		Context		context,
		SmsMessage	msg
	) {
		Database	db = new Database (context);
		long		id = -1;

		if (db.open ()) {
			id = db.addRecord (msg);
			db.close ();
		}

		if (id >= 0) {
			Intent			intent = new Intent (context, GetMeThere.class);

			intent.putExtra (IAmHere.RECEIVED_SMS_ID, id);

			PendingIntent	pi = PendingIntent.getActivity (context, 0,
																intent, 0);
			String			from = Utility.getContactNameFromNumber (context,
												msg.getOriginatingAddress ());
			String			ticker = "Geo SMS from " + from;
			Notification	n = new Notification (
										R.drawable.notify_geo_sms_icon,
										ticker, System.currentTimeMillis ());

			n.setLatestEventInfo (context, from, msg.getMessageBody(), pi);
			n.flags |= Notification.FLAG_AUTO_CANCEL;

				// Don't do any vibration or beeps - the regular SMS receiver
				// will do that.
			NotificationManager		nm;
			
			nm = (NotificationManager) context.getSystemService (
												Context.NOTIFICATION_SERVICE);
			nm.notify (1, n);
		}
	}
}
