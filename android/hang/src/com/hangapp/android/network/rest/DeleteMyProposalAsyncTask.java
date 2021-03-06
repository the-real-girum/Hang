package com.hangapp.android.network.rest;

import android.content.Context;
import android.util.Log;

import com.hangapp.android.database.Database;

final class DeleteMyProposalAsyncTask extends
		BaseDeleteRequestAsyncTask<String> {
	private static final String USERS_URI_SUFFIX = "/users/";
	private static final String PROPOSAL_URI_SUFFIX = "/proposal";

	private Database database;

	protected DeleteMyProposalAsyncTask(Database database, Context context,
			String jid) {
		super(context, USERS_URI_SUFFIX + jid + PROPOSAL_URI_SUFFIX);

		// Set dependencies
		this.database = database;
	}

	@Override
	public String call() throws Exception {
		// Execute the PUT request
		super.call();

		// TODO: Try to parse the resulting JSON
		Log.e("DeleteMyProposalAsyncTask.call", "Should parse "
				+ responseString);

		database.deleteMyProposal();

		return null;
	}
}
