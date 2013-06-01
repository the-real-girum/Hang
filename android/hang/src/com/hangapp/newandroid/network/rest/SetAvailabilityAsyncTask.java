package com.hangapp.newandroid.network.rest;

import java.util.List;

import org.apache.http.NameValuePair;

import android.content.Context;
import android.util.Log;

import com.hangapp.newandroid.database.Database;
import com.hangapp.newandroid.model.Availability;

public class SetAvailabilityAsyncTask extends
		BasePutRequestAsyncTask<Availability> {

	private static final String USERS_URI_SUFFIX = "/users/";
	private static final String STATUS_URI_SUFFIX = "/status";

	private Database database;

	protected SetAvailabilityAsyncTask(Database database, Context context,
			String jid, List<NameValuePair> parameters) {
		super(context, USERS_URI_SUFFIX + jid + STATUS_URI_SUFFIX, parameters);

		// Set dependencies.
		this.database = database;
	}

	@Override
	public Availability call() throws Exception {
		// Execute the PUT request
		super.call();

		// TODO: Try to parse the resulting JSON
		Log.e("SetAvailabilityAsyncTask", "Should parse " + responseString);

		return null;
	}

}