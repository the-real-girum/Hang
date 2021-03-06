package com.hangapp.android.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;

import android.util.Log;

public final class Proposal {
	private String description;
	private String location;
	private DateTime startTime;
	private List<String> interestedUsers;
	private List<String> confirmedUsers;

	/**
	 * A proposal's default duration, in hours.
	 */
	public static final Integer PROPOSAL_DURATION_IN_HOURS = 2;
	public static final int LOCATION_MAX_CHARS = 50;
	public static final int DESCRIPTION_MAX_CHARS = 100;

	public Proposal(String description, String location, DateTime time) {
		this.description = description;
		this.location = location;
		this.startTime = time;

		// We need at least an empty list to check for Users
		this.interestedUsers = new ArrayList<String>();
		this.confirmedUsers = new ArrayList<String>();
	}

	public static Proposal createProposal(String description, String location,
			DateTime startTime) throws Exception {
		DateTime rightNow = new DateTime();

		// Sanity checks.
		if (description == null) {
			throw new Exception("Description was null");
		} else if (description.trim().equals("")) {
			throw new Exception("Description was empty");
		} else if (location == null) {
			location = "";
		} else if (startTime == null) {
			throw new Exception("Start time was null");
		} else if (startTime.isBefore(rightNow)) {
			throw new Exception("Proposal start time already passed");
		}

		Proposal newProposal = new Proposal(description, location, startTime);

		return newProposal;
	}

	public String getDescription() {
		return description;
	}

	public String getLocation() {
		return location;
	}

	public DateTime getStartTime() {
		return startTime;
	}

	public List<String> getInterested() {
		return interestedUsers;
	}

	public void addInterested(String interestedUserJid) {
		interestedUsers.add(interestedUserJid);
	}

	public void removeInterested(String uninterestedUserJid) {
		interestedUsers.remove(uninterestedUserJid);
	}

	public void setInterested(List<String> interestedUsers) {
		this.interestedUsers = interestedUsers;
	}

	public void setConfirmed(List<String> confirmedUsers) {
		this.confirmedUsers = confirmedUsers;
	}

	public List<String> getConfirmed() {
		return confirmedUsers;
	}

	public void addConfirmed(String confirmedUserJid) {
		confirmedUsers.add(confirmedUserJid);
	}

	public void removeConfirmed(String unconfirmedUserJid) {
		confirmedUsers.remove(unconfirmedUserJid);
	}

	public boolean isActive() {
		// TODO: For now, Proposals expire after 2 hours.

		long diff = startTime.toDate().getTime() - new Date().getTime();
		int minLeft = (int) (diff / (60 * 1000));

		if (minLeft < 0 - PROPOSAL_DURATION_IN_HOURS * 60) {
			return false;
		} else {
			return true;
		}
	}

	// Field sanity checks should be performed in the field's setter method.
	// public static boolean descriptionIsValid(String proposalDescription) {
	// return proposalDescription != null
	// && !proposalDescription.trim().equals("")
	// && proposalDescription.length() <= Proposal.DESCRIPTION_MAX_CHARS;
	// }
	//
	// public static boolean locationIsValid(String proposalLocation) {
	// if (proposalLocation != null) {
	// return proposalLocation.length() <= Proposal.LOCATION_MAX_CHARS;
	// }
	//
	// return true;
	// }
	//
	// public static boolean timeIsValid(Calendar proposalTime) {
	// return !proposalTime.before(Calendar.getInstance());
	// }

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Proposal)) {
			if (o != null) {
				Log.e(Proposal.class.getSimpleName(),
						".equals() called with Proposal against a "
								+ o.getClass().getName());
			} else {
				Log.e(Proposal.class.getSimpleName(),
						".equals() called with Proposal against null");
			}

			return false;
		} else {
			Proposal otherProp = (Proposal) o;
			if (otherProp.getDescription().equals(this.description)
					&& otherProp.getLocation().equals(this.location)
					&& otherProp.getStartTime().equals(this.startTime)
					&& otherProp.interestedUsers.equals(this.interestedUsers)
					&& otherProp.confirmedUsers.equals(this.confirmedUsers)) {
				return true;
			} else {
				return false;
			}
		}
	}

}
