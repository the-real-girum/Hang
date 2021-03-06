package com.hangapp.android.network.rest;

import java.util.List;

import org.apache.http.NameValuePair;

import android.content.Context;
import android.util.Log;

public class PutBroadcastsAsyncTask extends
		BasePutRequestAsyncTask<String> {

	private static final String USERS_URI_SUFFIX = "/users/";
	private static final String BROADCAST_URI_SUFFIX = "/broadcasts";

	// Set dependencies.
	private RestClient restClient;

	protected PutBroadcastsAsyncTask(Context context,
			RestClient restClient, String uriSuffix,
			List<NameValuePair> parameters) {
		super(context, USERS_URI_SUFFIX + uriSuffix + BROADCAST_URI_SUFFIX,
				parameters);
		this.restClient = restClient;
	}

	@Override
	public String call() throws Exception {
		// Execute the PUT request
		super.call();

		Log.i("AddMultipleBroadcasteesAsyncTask", responseString);

		return null;
	}

	@Override
	protected void onSuccess(String result) throws Exception {
		restClient.getMyData();
	}

}
