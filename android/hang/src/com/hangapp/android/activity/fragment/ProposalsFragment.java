package com.hangapp.android.activity.fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.facebook.widget.ProfilePictureView;
import com.hangapp.android.R;
import com.hangapp.android.activity.ProfileActivity;
import com.hangapp.android.database.Database;
import com.hangapp.android.model.User;
import com.hangapp.android.model.callback.IncomingBroadcastsListener;
import com.hangapp.android.model.callback.MyProposalListener;
import com.hangapp.android.model.callback.SeenProposalsListener;
import com.hangapp.android.util.Fonts;
import com.hangapp.android.util.Keys;
import com.hangapp.android.util.MyExpandableViewGroup;

public class ProposalsFragment extends SherlockFragment implements
		IncomingBroadcastsListener, SeenProposalsListener {

	private static final String TAG = ProposalsFragment.class.getSimpleName();

	// UI stuff
	private ListView listViewFriends;
	private ProposalsAdapter adapter;
	private static MyExpandableViewGroup expandableView;
	private static boolean viewIsExpanded = false;

	// Member datum.
	private ArrayList<User> incomingBroadcasts = new ArrayList<User>();
	private Set<String> seenProposals = new HashSet<String>();

	// Dependencies.
	private Database database;

	private static MyProposalFragment myProposalFragment;
	private static CreateProposalFragment createProposalFragment;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");

		// Instantiate dependencies.
		database = Database.getInstance();

		// Reload the incoming broadcasts from savedInstanceState.
		if (savedInstanceState != null) {
			final List<User> savedIncomingBroadcasts = savedInstanceState
					.getParcelableArrayList(Keys.FRIENDS);
			this.incomingBroadcasts.clear();
			this.incomingBroadcasts.addAll(savedIncomingBroadcasts);

			final List<String> savedSeenJids = savedInstanceState
					.getStringArrayList(Keys.PROPOSAL_SEEN);
			this.seenProposals.clear();
			this.seenProposals.addAll(savedSeenJids);
		}

		// Set up TabsHost change listener?
		
		
		// Set up the fragments
		myProposalFragment = new MyProposalFragment();
//		database.addMyProposalListener((MyProposalListener) myProposalFragment);
		createProposalFragment = new CreateProposalFragment();

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
        Log.d(TAG, "onCreateView called");

		// Inflate the View for this Fragment.
		View view = inflater.inflate(R.layout.fragment_proposals, container,
				false);

		// Inflate create proposal fragment

		// setupMyFragment();
		// TODO: fragment should be dependent on whether the user has a proposal

		// Reference Views.
		listViewFriends = (ListView) view
				.findViewById(R.id.listViewProposalsFragment);

		// Set up the Adapter.
		adapter = new ProposalsAdapter(getActivity(), incomingBroadcasts,
				seenProposals);

		listViewFriends.setAdapter(adapter);
		listViewFriends.setEmptyView(view.findViewById(android.R.id.empty));

		// Set OnClickListeners.
		listViewFriends.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Context context = ProposalsFragment.this.getActivity();
				User user = incomingBroadcasts.get(position);

				Intent proposalLeechIntent = new Intent(context,
						ProfileActivity.class);
				proposalLeechIntent.putExtra(Keys.HOST_JID, user.getJid());
				context.startActivity(proposalLeechIntent);
			}
		});

		expandableView = (MyExpandableViewGroup) view
				.findViewById(R.id.expandingLayout);
//		expandableView.initialize(getActivity(), database);
//		expandableView.expand(expandableView);
		
/*		if (database.getMyProposal() != null) {
			Toast.makeText(getActivity(), "i should probably expand the view", Toast.LENGTH_SHORT).show();
			expandableView.expand(expandableView);
			setupMyFragment(database, getActivity().getSupportFragmentManager());
		} else {
			
			Toast.makeText(getActivity(), "i'm not expanding the view", Toast.LENGTH_SHORT).show();
		}
		*/
		return view;
	}

	public static void setupMyFragment(Database database,
			android.support.v4.app.FragmentManager fm) {
		Log.d(TAG, "setupMyFragment called");

		// Get a new Fragment for my proposal
		Fragment fragment = null;
		if (database.getMyProposal() != null
				&& database.getMyProposal().isActive()) {
			Log.d(TAG, "proposal isActive");
//			fragment = new MyProposalFragment();
			fragment = myProposalFragment;
            expandableView.resize(expandableView, MyExpandableViewGroup.Resize.EXISTING);
			database.addMyProposalListener((MyProposalListener) fragment);
		} else {
			Log.d(TAG, "proposal is NOT active");
//			fragment = new CreateProposalFragment();
			fragment = createProposalFragment;
            expandableView.resize(expandableView, MyExpandableViewGroup.Resize.DEFAULT);
		}

		// Update my proposal area on the Proposals tab
		FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.fragment_myy_proposal, fragment);
		ft.commit();

        // resize expandableView
    //    expandableView.resize(expandableView, MyExpandableViewGroup.Resize.CREATE);

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putParcelableArrayList(Keys.FRIENDS, incomingBroadcasts);

		final ArrayList<String> seenProposalsList = new ArrayList<String>(
				this.seenProposals);
		outState.putStringArrayList(Keys.PROPOSAL_SEEN, seenProposalsList);
	}

	@Override
	public void onResume() {
		super.onResume();
        Log.d(TAG, "onResume called");

		database.addIncomingBroadcastsListener(this);
		database.addSeenProposalListener(this);

		final List<User> incomingBroadcasts = database.getMyIncomingBroadcasts();
		onIncomingBroadcastsUpdate(incomingBroadcasts);

		final List<String> seenJids = database.getMySeenProposals();
		if (seenJids != null) {
			onMySeenProposalsUpdate(seenJids);
		}

		Log.d(TAG, "about to call setupMyFragment from onResume()");
		// TODO make sure this is being called in the right place
		// TODO I should only call setupMyFragment if this is the prop tab
		setupMyFragment(database, ProposalsFragment.this.getSherlockActivity()
				.getSupportFragmentManager());

        Log.d(TAG, "about to call expandableView.initialize");
		expandableView.initialize(getActivity(), database);
    //    Log.d(TAG, "about to call expandableView.expand");
    //    expandableView.expand(expandableView);
		
//		expandView();

	}

	@Override
	public void onPause() {
		super.onPause();

		database.removeIncomingBroadcastsListener(this);
		database.removeSeenProposalListener(this);
	}

	private static class ProposalsAdapter extends BaseAdapter {
		private List<User> friends;
		private Set<String> seenJids;
		private LayoutInflater inflater;
		private Context context;

		public ProposalsAdapter(Context context, List<User> friends,
				Set<String> seenJids) {
			inflater = LayoutInflater.from(context);
			this.context = context;
			this.friends = friends;
			this.seenJids = seenJids;
		}

		@Override
		public int getCount() {
			return friends.size();
		}

		@Override
		public Object getItem(int position) {
			return friends.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			final User user = friends.get(position);

			// Inflate the View if necessary.
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = inflater.inflate(R.layout.cell_proposals_fragment,
						null);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			// If this user's proposal hasn't been seen, color in cell.
			if (!seenJids.contains(user.getJid())) {
				convertView.setBackgroundColor(context.getResources().getColor(
						R.color.teal));
			} else {
				convertView.setBackgroundColor(context.getResources().getColor(
						android.R.color.white));
			}

			// Reference views
			holder.profilePictureView = (ProfilePictureView) convertView
					.findViewById(R.id.profilePictureView);
			holder.textViewFriendName = (TextView) convertView
					.findViewById(R.id.textViewFriendName);
			holder.textViewProposalDescription = (TextView) convertView
					.findViewById(R.id.textViewProposalsCellDescription);
			holder.textViewProposalLocation = (TextView) convertView
					.findViewById(R.id.textViewProposalsCellLocation);
			holder.textViewProposalStartTime = (TextView) convertView
					.findViewById(R.id.textViewProposalsCellStartTime);
			holder.textViewProposalInterested = (TextView) convertView
					.findViewById(R.id.textViewProposalsCellInterested);

			// Get the fonts used in this cell.
			Typeface champagneLimousinesBold = Typeface.createFromAsset(
					context.getAssets(), Fonts.CHAMPAGNE_LIMOUSINES_BOLD);
			Typeface champagneLimousines = Typeface.createFromAsset(
					context.getAssets(), Fonts.CHAMPAGNE_LIMOUSINES);

			// Set the fonts of this cell's TextViews.
			holder.textViewFriendName.setTypeface(champagneLimousinesBold);
			holder.textViewProposalDescription
					.setTypeface(champagneLimousinesBold);
			holder.textViewProposalLocation.setTypeface(champagneLimousinesBold);
			holder.textViewProposalStartTime.setTypeface(champagneLimousines);
			holder.textViewProposalInterested.setTypeface(champagneLimousinesBold);

			// Save the newly generated convertView into the "holder"
			// object.
			convertView.setTag(holder);

			// Populate the Views with correct data.
			holder.profilePictureView.setProfileId(user.getJid());
			holder.textViewFriendName.setText(user.getFullName());
			holder.textViewProposalDescription.setText(user.getProposal()
					.getDescription());
			holder.textViewProposalLocation.setText(user.getProposal()
					.getLocation());
			holder.textViewProposalStartTime.setText(user.getProposal()
					.getStartTime().toString("h:mm aa"));

			// Construct the internationalized string for the number of users
			// interested in this Proposal. Then use it to populate the
			// TextView.
			final int numberOfUsersInterested = user.getProposal().getInterested()
					.size();
			final String interestedString = String.format(context.getResources()
					.getString(R.string.count_interested), numberOfUsersInterested);
			holder.textViewProposalInterested.setText(interestedString);

			return convertView;
		}

		class ViewHolder {
			ProfilePictureView profilePictureView;
			TextView textViewFriendName;
			TextView textViewProposalDescription;
			TextView textViewProposalLocation;
			TextView textViewProposalStartTime;
			TextView textViewProposalInterested;
		}
	}

	@Override
	public void onIncomingBroadcastsUpdate(List<User> incomingBroadcasts) {
		this.incomingBroadcasts.clear();

		if (incomingBroadcasts != null) {
			// Remove users who have no Proposal from the new List<User>.
			for (Iterator<User> iterator = incomingBroadcasts.iterator(); iterator
					.hasNext();) {
				User user = iterator.next();

				//
				if (user.getProposal() == null) {
				/*	Log.d("ProposalsFragment.onIncomingBroadcastsUpdate",
							"removing user " + user.getFirstName()
									+ " for null proposal");*/
					iterator.remove();
				} /*
					 * else if (!(user.getProposal().isActive())) {
					 * Log.d("ProposalsFragment.onIncomingBroadcastsUpdate",
					 * "removing user " + user.getFirstName() +
					 * " for expired proposal"); iterator.remove(); }
					 */
			}

			// // Convert Users to SeenUsers to give them seen "state" booleans
			// for (User user : incomingBroadcasts) {
			// UserWithSeenState seenUser = new UserWithSeenState(user, false);
			// this.incomingBroadcasts.add(seenUser);
			// }

			// Add the ones that are left to the internal List<User>.
			this.incomingBroadcasts.addAll(incomingBroadcasts);
			Collections.sort(this.incomingBroadcasts);
		}

		adapter.notifyDataSetChanged();
	}

	@Override
	public void onMySeenProposalsUpdate(List<String> seenJids) {
		this.seenProposals.clear();
		this.seenProposals.addAll(seenJids);

		adapter.notifyDataSetChanged();
	}

	public static void collapseView() {
		viewIsExpanded = expandableView.collapse(expandableView);
	}

//	public static void expandView() {
//		viewIsExpanded = expandableView.expand();
//	}

}
