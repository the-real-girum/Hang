package com.hangapp.android.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.widget.ProfilePictureView;
import com.hangapp.android.R;
import com.hangapp.android.database.Database;
import com.hangapp.android.model.User;
import com.hangapp.android.network.rest.RestClient;
import com.hangapp.android.network.rest.RestClientImpl;
import com.hangapp.android.util.BaseActivity;
import com.hangapp.android.util.Keys;

public final class ProfileActivity extends BaseActivity {

	private ProfilePictureView profilePictureView;
	private TextView textViewFriendName;

	private Database database;
	private RestClient restClient;

	private User friend;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_profile);

		// Instantiate dependencies
		database = Database.getInstance();
		restClient = new RestClientImpl(database, getApplicationContext());

		// Set who the friend is.
		String hostJid = getIntent().getStringExtra(Keys.HOST_JID);
		friend = database.getIncomingUser(hostJid);

		// Friend not in Database sanity check.
		if (friend == null) {
			Log.e("ProfileActivity.onCreate", "Host with jid: " + hostJid
					+ " was null in the Database.");
			finish();
		}

		// Enable the "Up" button.
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// Reference Views.
		profilePictureView = (ProfilePictureView) findViewById(R.id.profilePictureView);
		textViewFriendName = (TextView) findViewById(R.id.textViewFriendName);

		// Populate Views.
		profilePictureView.setProfileId(friend.getJid());
		textViewFriendName.setText(friend.getFullName());

		// Set OnClickListeners.
		profilePictureView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(getApplicationContext(),
						"Nudging " + friend.getFirstName(), Toast.LENGTH_SHORT)
						.show();
				restClient.sendNudge(friend.getJid());
			}
		});
	}
}