package com.hangapp.android.network.rest;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import com.hangapp.android.database.Database;
import com.hangapp.android.model.Me;
import com.hangapp.android.model.User;

final class GetMyDataAsyncTask extends BaseGetRequestAsyncTask<Me> {

	private static final String URL_SUFFIX = "/users/";

	private Database database;
	private List<User> library;

	protected GetMyDataAsyncTask(Database database, Context context,
			String jid) {
		super(context, URL_SUFFIX + jid);

		// Set dependencies.
		this.database = database;
	}

	@Override
	public Me call() throws Exception {
		// Execute the GET request
		super.call();

		// Try to parse the resulting JSON for my user data.
		Me me = Me.parseMyUserData(responseString);

		// Try to parse the resulting JSON for the library.
		library = Me.parseLibrary(responseString);

		// If it worked, return the User.
		return me;
	}

	@Override
	protected void onSuccess(Me me) throws Exception {
		// Save the library of other users' data into the database.
		// Do this first so that the setMy* calls after this work.
		database.saveLibrary(library);

		// Save the user data in the database.
		database.setMyUserData(me.getJid(), me.getFirstName(), me.getLastName());
		database.setMyIncomingBroadcasts(me.getIncomingBroadcasts());
		database.setMyOutgoingBroadcasts(me.getOutgoingBroadcasts());
		database.setMyAvailability(me.getAvailability());
		database.setMyProposal(me.getProposal());
		database.setMySeenProposals(me.getSeenProposals());

		// Use the resulting user data to construct a list of JIDs of users
		// whose Proposals I am interested in.
		final List<String> jidsImInterestedIn = getJidsImInterestedIn(me);
		database.setJidsImInterestedIn(jidsImInterestedIn);

		// Attempt to join the MUC belonging to each of the JIDs that I am
		// interested in.
		//
		// TODO: You should join your own MUC, if you have one.
	}

	/**
	 * Helper method to loop through my new Incoming Broadcasts and construct a
	 * list of JIDs of users whose Proposals I am interested in.
	 * 
	 * @param me
	 * @return
	 */
	private List<String> getJidsImInterestedIn(Me me) {
		List<String> jidsImInterestedIn = new ArrayList<String>(me
				.getIncomingBroadcasts().size());

		for (String incomingBroadcastJid : me.getIncomingBroadcasts()) {
			// Reference the Incoming Broadcast User object for that JID.
			User incomingBroadcast = database
					.getIncomingUser(incomingBroadcastJid);

			// If the Incoming Broadcast indeed has a Proposal and an Interested
			// list...
			if (incomingBroadcast != null
					&& incomingBroadcast.getProposal() != null
					&& incomingBroadcast.getProposal().getInterested() != null) {

				// If my JID shows up in this user's Proposal's Interested list,
				// then this Incoming Broadcast is a JID that I'm "Interested"
				// in. Add him to my list of "JIDs I'm Interested in".
				for (String incomingBroadcastInterestedJid : incomingBroadcast
						.getProposal().getInterested()) {
					if (incomingBroadcastInterestedJid.equals(me.getJid())) {
						jidsImInterestedIn.add(incomingBroadcastJid);
					}
				}
			}
		}

		return jidsImInterestedIn;
	}
}
