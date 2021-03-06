package com.hangapp.android.activity;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.crashlytics.android.Crashlytics;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.flurry.android.FlurryAgent;
import com.google.analytics.tracking.android.EasyTracker;
import com.hangapp.android.R;
import com.hangapp.android.activity.fragment.FeedFragment;
import com.hangapp.android.activity.fragment.MyProposalFragment;
import com.hangapp.android.activity.fragment.ProposalsFragment;
import com.hangapp.android.activity.fragment.YouFragment;
import com.hangapp.android.activity.fragment.YouFragment.ProposalChangedListener;
import com.hangapp.android.database.Database;
import com.hangapp.android.model.Proposal;
import com.hangapp.android.model.User;
import com.hangapp.android.network.rest.RestClient;
import com.hangapp.android.network.rest.RestClientImpl;
import com.hangapp.android.util.BaseActivity;
import com.hangapp.android.util.Fonts;
import com.hangapp.android.util.Keys;
import com.hangapp.android.util.NoSlideViewPager;
import com.hangapp.android.util.TabsAdapter;

/**
 * This is our main Activity. This Activity manages two possible "fragments".
 * First, it is responsible for holding the main tabs of the app (we currently
 * use {@link FeedFragment} and {@link YouFragment} as our tabs). <br />
 * <br />
 * Second, it uses Facebook's {@link UiLifecycleHelper} class to "listen" to
 * changes in Facebook session state (it listens to whether or not you're logged
 * into Facebook). It handles changes in onSessionStateChange(). Several
 * activities "listen" to Facebook session state this way. This particular
 * Activity listens to state changes and hides / shows its tabs based on that.
 * If the user is logged out of Facebook, it shows {@link LoginFragment} and
 * hides its tabs.
 */
public final class HomeActivity extends BaseActivity implements
		ProposalChangedListener {

    private static final String TAG = HomeActivity.class.getSimpleName();

	// UI stuff.
	private NoSlideViewPager mViewPager;
	private static TabsAdapter mTabsAdapter;

	// Dependencies.
	private SharedPreferences prefs;
	private Database database;
	private RestClient restClient;

	// Facebook SDK member variables.
	private UiLifecycleHelper uiHelper;
	private Session.StatusCallback callback = new Session.StatusCallback() {
		@Override
		public void call(Session session, SessionState state, Exception exception) {
			onSessionStateChange(session, state, exception);
		}
	};

	public static TabsAdapter getTabsAdapter() {
		return mTabsAdapter;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Crashlytics.start(this);
		try {

			PackageInfo info = getPackageManager().getPackageInfo(
					"com.hangapp.android.activity", PackageManager.GET_SIGNATURES);
			for (Signature signature : info.signatures) {
				MessageDigest md = MessageDigest.getInstance("SHA");
				md.update(signature.toByteArray());
				Log.d("KeyHash:",
						"Keyhash: "
								+ Base64.encodeToString(md.digest(), Base64.DEFAULT));
			}
		} catch (NameNotFoundException e) {

		} catch (NoSuchAlgorithmException e) {

		}

		// Initialize dependencies.
		prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		database = Database.getInstance();
		restClient = new RestClientImpl(database, getApplicationContext());
		LayoutInflater inflater = LayoutInflater.from(this);

		// Initialize the ViewPager and set it to be the ContentView of this
		// Activity.
		mViewPager = new NoSlideViewPager(this);
		mViewPager.setId(R.id.viewpager);
		setContentView(mViewPager);

		// Throw the three tabs into the ActionBar.
		final ActionBar bar = getSupportActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		mTabsAdapter = new TabsAdapter(this, mViewPager);
		mTabsAdapter.addTab(bar.newTab(), FeedFragment.class, null);
		// mTabsAdapter.addTab(bar.newTab(), YouFragment.class, null);
		mTabsAdapter.addTab(bar.newTab(), ProposalsFragment.class, null);

		// Style the Action Bar tabs.
		String[] tabNames = { "FEED", /* "YOU", */"PROPOSALS"/* , "FEED2" */};
		Typeface champagneLimousinesFont = Typeface.createFromAsset(
				getApplicationContext().getAssets(),
				Fonts.CHAMPAGNE_LIMOUSINES_BOLD);
		for (int i = 0; i < bar.getTabCount(); i++) {
			View customView = inflater.inflate(R.layout.tab_title, null);

			TextView titleTV = (TextView) customView
					.findViewById(R.id.action_custom_title);
			titleTV.setText(tabNames[i]);
			titleTV.setTypeface(champagneLimousinesFont);

			bar.getTabAt(i).setCustomView(customView);
		}

		// Here are the tab listeners
		ActionBar.TabListener tabListener = new ActionBar.TabListener() {

			@Override
			public void onTabSelected(Tab tab, FragmentTransaction ft) {
//				Toast.makeText(getApplicationContext(), "tab selected",
//						Toast.LENGTH_SHORT).show();

				// Check if the tab is this one
				Object tag = tab.getTag();
				for (int i = 0; i < bar.getTabCount(); i++) {
					if (TabsAdapter.getTabInfo().get(i) == tag) {
						mViewPager.setCurrentItem(i);
                        Log.d(TAG, "calling setupMyFragment from TabListener");
						ProposalsFragment.setupMyFragment(database, getSupportFragmentManager());
					}
				}
			}

			@Override
			public void onTabUnselected(Tab tab, FragmentTransaction ft) {
				// TODO I can potentially conserve resources

			}

			@Override
			public void onTabReselected(Tab tab, FragmentTransaction ft) {
			}
		};

		// Add TabListeners to tabs
		for (int i = 0; i < bar.getTabCount(); i++) {
			bar.getTabAt(i).setTabListener(tabListener);
		}
		
		// Setup Facebook SDK.
		uiHelper = new UiLifecycleHelper(this, callback);
		uiHelper.onCreate(savedInstanceState);

		// Reload the tab you had selected from savedInstanceState, if it was
		// saved.
		if (savedInstanceState != null) {
			bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		FlurryAgent.onStartSession(this, Keys.FLURRY_KEY);
		EasyTracker.getInstance(this).activityStart(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onResume() {
		super.onResume();
		uiHelper.onResume();

		// First thing's first: check to see if the user has Google Play
		// services installed. If he doesn't, let the Google SDK show
		// the Dialog that redirects hito install
		// it.
		checkPlayServices();

		boolean userIsRegistered = prefs.getBoolean(Keys.REGISTERED, false);

		// If the user hasn't registered yet, then show the LoginFragment.
		if (!userIsRegistered) {
			setContentView(R.layout.login);
			// Just to be funny
			((TextView) findViewById(R.id.textViewToughLuck))
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							Intent browserIntent = new Intent(
									Intent.ACTION_VIEW,
									Uri.parse("http://www.urbandictionary.com/define.php?term=tough+luck"));
							startActivity(browserIntent);
						}
					});

			getSupportActionBar().hide();
		}
		// Otherwise, show the regular tabbed ActionBar.
		else {
			setContentView(mViewPager);
			getSupportActionBar().show();

			restClient.getMyData();
		}

		// (Facebook SDK) For scenarios where the main activity is launched and
		// user session is not null, the session state change notification
		// may not be triggered. Trigger it if it's open/closed.
		Session session = Session.getActiveSession();
		if (session != null && (session.isOpened() || session.isClosed())) {
			onSessionStateChange(session, session.getState(), null);
		}

		// Request "my" user data from Facebook and save the results.
		Request.executeMeRequestAsync(session, new Request.GraphUserCallback() {
			@Override
			public void onCompleted(GraphUser graphUser, Response response) {
				if (graphUser != null) {
					Log.v("HomeActivity.onResume",
							"Retrieved Facebook MeRequest, saving data internally"
									+ " for Facebook user " + graphUser.getName());

					// You've officially "registered."
					SharedPreferences.Editor editor = prefs.edit();
					editor.putBoolean(Keys.REGISTERED, true);
					// This stores the current User's j.
					editor.putString(Keys.JID, graphUser.getId());
					editor.commit();

					User me = new User(graphUser.getId(), graphUser.getFirstName(),
							graphUser.getLastName());

					// Save the "me" User object into the database.
					database.setMyUserData(me.getJid(), me.getFirstName(),
							me.getLastName());

					// Register the "me" User object into our server.
					restClient.registerNewUser(me);
				}
			}
		});

		// Check if a certain tab should be opened (especially from a
		// notification)
		int initialTab = getIntent().getIntExtra(Keys.TAB_INTENT, -1);
		if (initialTab != -1)
			getSupportActionBar().setSelectedNavigationItem(initialTab);

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		// Save which tab we had selected into savedInstanceState.
		outState
				.putInt("tab", getSupportActionBar().getSelectedNavigationIndex());
		uiHelper.onSaveInstanceState(outState);
	}

	/**
	 * Check the device to make sure it has the Google Play Services APK. If it
	 * doesn't, display a dialog that allows users to download the APK from the
	 * Google Play Store or enable it in the device's system settings.
	 */
	private boolean checkPlayServices() {
		// final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
		//
		// int resultCode = GooglePlayServicesUtil
		// .isGooglePlayServicesAvailable(this);
		// if (resultCode != ConnectionResult.SUCCESS) {
		// if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
		// GooglePlayServicesUtil.getErrorDialog(resultCode, this,
		// PLAY_SERVICES_RESOLUTION_REQUEST).show();
		// } else {
		// Log.i("onResume", "This device is not supported.");
		// finish();
		// }
		// return false;
		// }
		return true;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		uiHelper.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onPause() {
		super.onPause();
		uiHelper.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_home, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_refresh:
			restClient.getMyData();

			return true;
		case R.id.menu_settings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		default:
			Log.e("HomeActivity.onOptionsItemSelected", "Unknown item selected: "
					+ item.getAlphabeticShortcut());
			return true;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		uiHelper.onDestroy();
	}

	/**
	 * HomeActivity "watches" for Facebook session validity in order to show the
	 * LoginFragment if a logout occurs for any reason.
	 */
	private void onSessionStateChange(Session session, SessionState state,
			Exception exception) {
		if (state.isOpened()) {
			Log.i("HomeActivity.onSessionStateChange", "Logged in to Facebook...");
			// Since Facebook successfully logged in, show the regular tabbed
			// ActionBar.
			// TODO: Use real fragment transactions instead of this ghetto
			// double-call of setContentView().
			setContentView(mViewPager);
			getSupportActionBar().show();
		} else if (state.isClosed()) {
			Log.i("HomeActivity.onSessionStateChange", "Logged out of Facebook...");
			// Since Facebook isn't logged in, show the LoginFragment.
			setContentView(R.layout.login);
			getSupportActionBar().hide();

			if (exception != null) {
				Log.e("HomeActivity.onSessionStateChange", "Facebook exception: "
						+ exception.getMessage());
			} else {
				Log.i("HomeActivity.onSessionStateChange",
						"Facebook logged out without exception");
			}
		}
	}

	/**
	 * XML onClickListener for the Empty View "Add Outgoing Broadcasts" button.
	 * This appears here, even though FeedFragment manages the Empty View
	 * itself... TODO we should change this so that FeedFragment explicitly
	 * implements this OnClickListener for the button.
	 */
	public void addMoreOutgoingBroadcasts(View v) {
		Intent intent = new Intent();
		intent.setData(AddOutgoingBroadcastActivity.FRIEND_PICKER);
		intent.setClass(this, AddOutgoingBroadcastActivity.class);
		startActivityForResult(intent, RESULT_OK);
	}

	public void notifyAboutProposalChange(Proposal proposal) {

		Log.d("HomeActivity.onProposalChangedListenerNotified", "" + proposal);
		if (proposal != null)
			Log.d("proposal desc", proposal.getDescription());
		else
			Log.d("proposal desc", "proposal == NULL");
		MyProposalFragment myProposalFragment = (MyProposalFragment) getSupportFragmentManager()
				.findFragmentByTag(Keys.MY_PROPOSAL_FRAGMENT_TAG);

		if (myProposalFragment == null) {
			Log.i("HomeActivity.onProposalChangedListenerNotified",
					"MyProposalFragment was null");
		} else if (myProposalFragment.isVisible()) {
			Log.i("HomeActivity.onProposalChangedListenerNotified",
					"MyProposalFragment is visible");

			Log.i("HomeActivity.onProposalChangedListenerNotified",
					"going to pass prop desc of " + proposal.getDescription());
			myProposalFragment.updateProposal(proposal);
		} else {
			Log.i("HomeActivity.onProposalChangedListenerNotified",
					"MyProposalFragment is there but not visible");
		}
	}
}
