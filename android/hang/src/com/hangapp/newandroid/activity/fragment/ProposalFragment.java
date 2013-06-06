package com.hangapp.newandroid.activity.fragment;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.actionbarsherlock.app.SherlockFragment;
import com.facebook.widget.ProfilePictureView;
import com.hangapp.newandroid.R;
import com.hangapp.newandroid.activity.ChatActivity;
import com.hangapp.newandroid.database.Database;
import com.hangapp.newandroid.model.User;
import com.hangapp.newandroid.model.callback.IncomingBroadcastsListener;
import com.hangapp.newandroid.network.rest.RestClient;
import com.hangapp.newandroid.network.rest.RestClientImpl;
import com.hangapp.newandroid.util.Keys;

public final class ProposalFragment extends SherlockFragment implements
		IncomingBroadcastsListener {

	private ScrollView scrollViewProposal;
	private RelativeLayout emptyView;

	private TextView textViewProposalTitle;
	private TextView textViewProposalDescription;
	private TextView textViewProposalLocation;
	private TextView textViewProposalStartTime;

	private ListView listViewInterested;
	private ListView listViewConfirmed;
	private List<String> listInterestedJids = new ArrayList<String>();
	private List<String> listConfirmedJids = new ArrayList<String>();
	private IntConfAdapter intAdapter;
	private IntConfAdapter confAdapter;

	private ImageView buttonChat;
	private ImageView buttonDeleteProposal;

	private ToggleButton toggleInterested;
	private ToggleButton toggleConfirmed;

	private String hostJid;
	private User host;

	private Database database;
	private RestClient restClient;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Instantiate dependencies.
		database = Database.getInstance();
		restClient = new RestClientImpl(database, getActivity()
				.getApplicationContext());

		// Setup listener
		database.addIncomingBroadcastsListener(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_proposal2, container,
				false);

		// Reference views.
		textViewProposalTitle = (TextView) view
				.findViewById(R.id.textViewProposalTitle);
		textViewProposalDescription = (TextView) view
				.findViewById(R.id.textViewProposalDescription);
		textViewProposalLocation = (TextView) view
				.findViewById(R.id.textViewProposalLocation);
		textViewProposalStartTime = (TextView) view
				.findViewById(R.id.textViewProposalStartTime);
		scrollViewProposal = (ScrollView) view
				.findViewById(R.id.scrollViewProposal);
		emptyView = (RelativeLayout) view.findViewById(android.R.id.empty);

		listViewInterested = (ListView) view
				.findViewById(R.id.horizontalScrollView1ListView);
		listViewConfirmed = (ListView) view
				.findViewById(R.id.horizontalScrollView2ListView);

		// Set up the Adapters.
		intAdapter = new IntConfAdapter(getActivity(), listInterestedJids);
		confAdapter = new IntConfAdapter(getActivity(), listConfirmedJids);
		listViewInterested.setAdapter(intAdapter);
		listViewConfirmed.setAdapter(confAdapter);

		buttonChat = (ImageView) view.findViewById(R.id.buttonChat);
		buttonDeleteProposal = (ImageView) view
				.findViewById(R.id.buttonDeleteProposal);

		toggleInterested = (ToggleButton) view
				.findViewById(R.id.interestedToggle);
		toggleConfirmed = (ToggleButton) view.findViewById(R.id.confirmedToggle);

		// Set Toggle ClickListeners
		toggleInterested
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked) {
							Toast.makeText(getActivity(), "I'm interested...",
									Toast.LENGTH_SHORT).show();
							addMeToHostInterestedList();
						} else {
							Toast.makeText(getActivity(),
									"Not so interested anymore...", Toast.LENGTH_SHORT)
									.show();
							if (!toggleConfirmed.isChecked())
									removeMeFromHostInterestedList();
						}

					}
				});
		toggleConfirmed.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					Toast.makeText(getActivity(), "I'm confirming!",
							Toast.LENGTH_SHORT).show();
					toggleInterested.setChecked(false);
					toggleInterested.setEnabled(false);
					addMeToHostConfirmedList();

				} else {
					Toast.makeText(getActivity(), "I'm a flake :(",
							Toast.LENGTH_SHORT).show();
					toggleInterested.setEnabled(true);
					removeMeFromHostConfirmedList();
				}

			}
		});

		// Hide the "delete" button, since this isn't your own Proposal.
		buttonDeleteProposal.setVisibility(View.GONE);

		// Populate member datum
		hostJid = getActivity().getIntent().getStringExtra(Keys.HOST_JID);

		buttonChat.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Start the ChatActivity.
				Intent chatActivityIntent = new Intent(getActivity(),
						ChatActivity.class);
				chatActivityIntent.putExtra(Keys.HOST_JID, hostJid);
				startActivity(chatActivityIntent);
			}
		});

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();

		// Reload the host (and his Proposal) from the database.
		host = database.getIncomingUser(hostJid);

		if (host.getProposal() != null) {
			// Turn off the Empty View.
			scrollViewProposal.setVisibility(View.VISIBLE);
			emptyView.setVisibility(View.INVISIBLE);

			// Populate the Views.
			String proposalTitle = String.format(
					getString(R.string.someones_proposal), host.getFirstName());
			textViewProposalTitle.setText(proposalTitle);
			textViewProposalDescription.setText(host.getProposal()
					.getDescription());
			textViewProposalLocation.setText(host.getProposal().getLocation());
			textViewProposalStartTime.setText(host.getProposal().getStartTime()
					.toString("h aa"));

		} else {
			// Turn on the Empty View.
			scrollViewProposal.setVisibility(View.INVISIBLE);
			emptyView.setVisibility(View.VISIBLE);
		}

		// If User is Interested/Confirmed, check the appropriate ToggleButton
		if (host.getProposal().getInterested() != null) {
			if (host.getProposal().getInterested().contains(database.getMyJid()))
				toggleInterested.setChecked(true);
		} else
			Log.i(ProposalFragment.class.getSimpleName(), "None interested.");
		if (host.getProposal().getConfirmed() != null) {
			if (host.getProposal().getConfirmed().contains(database.getMyJid()))
				toggleConfirmed.setChecked(true);
		} else
			Log.i(ProposalFragment.class.getSimpleName(), "None confirmed.");

	}

	private void addMeToHostInterestedList() {
		removeMeFromHostConfirmedList();
		Log.v("addMeToHostInterestedList", "added to host interested");
		restClient.setInterested(host.getJid());
	}

	private void removeMeFromHostInterestedList() {
		restClient.deleteInterested(host.getJid());
	}

	private void addMeToHostConfirmedList() {
		toggleInterested.setChecked(false);
		restClient.setConfirmed(host.getJid());
	}

	private void removeMeFromHostConfirmedList() {
		restClient.deleteConfirmed(host.getJid());
	}

	@Override
	public void onIncomingBroadcastsUpdate(List<User> incomingBroadcasts) {
		// Find out if User's Interested was updated
		if (!database.getIncomingUser(host.getJid()).getProposal()
				.getInterested().equals(listInterestedJids)) {
			listInterestedJids.clear();
			listInterestedJids.addAll(database.getIncomingUser(host.getJid())
					.getProposal().getInterested());
			intAdapter.notifyDataSetChanged();
		}
		
		// Find out if User's Confirmed was updated
		if (!database.getIncomingUser(host.getJid()).getProposal().getConfirmed()
				.equals(listConfirmedJids)) {
			listConfirmedJids.clear();
			listConfirmedJids.addAll(database.getIncomingUser(host.getJid())
					.getProposal().getConfirmed());
			confAdapter.notifyDataSetChanged();
		}

	}

	private class IntConfAdapter extends BaseAdapter {

		private List<String> intJids;
		private Context context;

		@Override
		public int getCount() {
			return intJids.size();
		}

		@Override
		public Object getItem(int position) {
			return intJids.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		public IntConfAdapter(Context context, List<String> intJids) {
			this.context = context;
			this.intJids = intJids;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			final String jid = intJids.get(position);

			// Inflate the View if necessary
			if (convertView == null) {

				ListView.LayoutParams params = new ListView.LayoutParams(
						ListView.LayoutParams.WRAP_CONTENT,
						ListView.LayoutParams.MATCH_PARENT);
				convertView = new TextView(context);
				((TextView) convertView).setText(jid);
				((TextView) convertView).setTypeface(Typeface.createFromAsset(
						context.getAssets(), "fonts/Harabara.ttf"));
				((TextView) convertView).setTextSize(30);
				((TextView) convertView).setLayoutParams(params);
				
				
				// Try to get User
				User user = database.getIncomingUser(jid);
				
				if (database.getIncomingUser(jid) != null) {
					Toast.makeText(context, "You know this guy!", Toast.LENGTH_SHORT).show();
					((TextView) convertView).setText(user.getFullName());
				} else if (jid.equals(database.getMyJid())) {
					((TextView) convertView).setText(database.getMyFullName() + " (me)");
				}

			}

			return convertView;
		}

	}

}
