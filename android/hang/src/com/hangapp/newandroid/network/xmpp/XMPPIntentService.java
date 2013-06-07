package com.hangapp.newandroid.network.xmpp;

import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.ConfigureProviderManager;
import org.jivesoftware.smackx.muc.MultiUserChat;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.hangapp.newandroid.util.BaseApplication;
import com.hangapp.newandroid.util.Keys;

/**
 * This helper class is an {@link IntentService}, and it handles all of our
 * background XMPP logic for us. You should interface with this class via the
 * {@link XMPP} class. <br />
 * <br />
 * It is driven by a constant queue of Intent messages sent from either
 * <ol>
 * the app (e.g. {@link BaseApplication}) or from
 * </ol>
 * <ol>
 * itself (e.g. once you connect, immediately send a new Intent to attempt to
 * login).
 * </ol>
 */
public final class XMPPIntentService extends IntentService {
	private static final String XMPP_SERVER_URL = "ec2-184-72-81-86.compute-1.amazonaws.com";

	/**
	 * Maintain a single, static {@link XMPPConnection} through the lifecycle of
	 * the whole app. <br />
	 * <br />
	 * We don't have to worry about concurrent access to the same variable
	 * because {@link IntentService} guarantees that onHandleIntent() handles a
	 * single {@link Intent} at a time, in a queue.
	 */
	private static XMPPConnection xmppConnection;

	/**
	 * Hold a reference to the front-facing XMPP object.
	 */
	private static XMPP xmpp;

	/**
	 * Share a single {@link MyConnectionListener} object for the
	 * {@link XMPPConnection}, instead of instantiating a new one every time you
	 * connect to XMPP.
	 */
	private static MyConnectionListener mConnectionListener = new MyConnectionListener();

	private static Map<String, MultiUserChat> mucs = new HashMap<String, MultiUserChat>();

	public XMPPIntentService() {
		super("XMPPIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		// Instantiate your dependency on the front-facing XMPP object
		xmpp = XMPP.getInstance();

		// Figure out which message the Intent is sending.
		int message = intent.getIntExtra(Keys.MESSAGE, 0);
		Log.d("TAG", "XMPPIntentService started with intent message: "
				+ message);

		// Pull out all extras. Sanity check them later, when you need them.
		String myJid = intent.getStringExtra(Keys.JID);
		String mucName = intent.getStringExtra(Keys.MUC_NAME);
		String mucMessage = intent.getStringExtra(Keys.MUC_MESSAGE);

		// Start the correct method based on which message was passed into the
		// Intent.
		switch (message) {
		case Keys.XMPP_CONNECT:

			// Sanity check.
			if (myJid == null) {
				Log.e("XMPPIntentService", "Can't connect to XMPP: Null myJid");
				return;
			}

			connect(myJid);
			return;
		case Keys.XMPP_REGISTER:

			// Sanity check.
			if (myJid == null) {
				Log.e("XMPPIntentService", "Can't register to XMPP: Null myJid");
				return;
			}

			register(myJid);
			return;
		case Keys.XMPP_LOGIN:

			// Sanity check.
			if (myJid == null) {
				Log.e("XMPPIntentService", "Can't login to XMPP: Null myJid");
				return;
			}

			login(myJid);

			return;
		case Keys.XMPP_LOGOUT:
			logout();
			return;
		case Keys.XMPP_JOIN_MUC:

			// Sanity check.
			if (myJid == null) {
				Log.e("XMPPIntentService", "Can't join muc: Null myJid");
				return;
			}

			// Sanity check.
			if (mucName == null) {
				Log.e("XMPPIntentService",
						"Can't join muc: Intent was passed a null MUC name");
				return;
			}

			joinMuc(mucName, myJid);
			return;
		case Keys.XMPP_LEAVE_MUC:
			// Sanity check.
			if (mucName == null) {
				Log.e("XMPPIntentService",
						"Can't leave XMPP muc: Intent was passed a null muc name");
				return;
			}

			leaveMuc(mucName);
			return;
		case Keys.XMPP_SEND_MUC_MESSAGE:

			// Sanity check.
			if (myJid == null) {
				Log.e("XMPPIntentService", "Can't join muc: Null myJid");
				return;
			}

			// Sanity check.
			if (mucName == null) {
				Log.e("XMPPIntentService",
						"Can't send XMPP muc message: mucName was null");
				return;
			}

			// Sanity check.
			if (mucMessage == null) {
				Log.e("XMPPIntentService",
						"Can't send XMPP muc message: mucMessage was null");
				return;
			}

			sendMucMessage(myJid, mucName, mucMessage);
			return;
		default:
			Log.e("XMPPPIntentService.onHandleIntent",
					"Unknown intent message code: " + message);
			return;
		}
	}

	protected void connect(String myJid) {
		// If this is the first time running, then initialize the XMPP
		// connection.
		if (xmppConnection == null) {
			// Setup aSmack.
			SmackAndroid.init(getApplicationContext());
			ConfigureProviderManager.configureProviderManager();

			// Initialize the XMPPConnection itself. Point it to our EC2 server.
			xmppConnection = new XMPPConnection(XMPP_SERVER_URL);
		}

		// If you're already connected, then just attempt to login.
		if (xmppConnection.isConnected()) {
			Log.e("XMPPIntentService.connect()", "Already connected");

			// Already connected, so attempt to login.
			xmpp.login(myJid, getApplicationContext());
			return;
		}

		// Otherwise, attempt to connect to XMPP.
		try {
			SASLAuthentication.supportSASLMechanism("PLAIN", 0);
			xmppConnection.connect();
		} catch (XMPPException e) {
			Log.e("XMPPIntentService.connect()", e.getMessage());

			// Attempt to connect again?
			xmpp.connect(myJid, getApplicationContext());
			return;
		}

		if (xmppConnection.isConnected()) {
			Log.d("XMPPIntentService.connect()", "Connected to XMPP");
			// If the Connection succeeded, then add your
			// ConnectionListener.
			xmppConnection.removeConnectionListener(mConnectionListener);
			xmppConnection.addConnectionListener(mConnectionListener);

			// Attempt to login.
			xmpp.login(myJid, getApplicationContext());
			return;
		} else {
			// If XMPPConnection isn't connected even after connect() passed,
			// then attempt to connect again.
			xmpp.connect(myJid, getApplicationContext());
			return;
		}
	}

	protected void register(String myJid) {
		// If you try to login without being connected, then try to connect
		// again.
		if (!xmppConnection.isConnected()) {
			Log.e("XMPPIntentService.register()", "Can't login: not connected");

			// Not connected, so attempt to connect.
			xmpp.connect(myJid, getApplicationContext());
			return;
		}

		// If you try to register when you're already authenticated, then you're
		// done -- you don't have to register because you're already logged in.
		if (xmppConnection.isAuthenticated()) {
			Log.e("XMPPIntentService.register()",
					"Can't register: already authenticated");
			// Do nothing.
			return;
		}

		try {
			AccountManager accountManager = xmppConnection.getAccountManager();
			accountManager.createAccount(myJid, myJid);
		} catch (XMPPException e) {
			Log.e("XMPPIntentService.register()",
					"Can't register new XMPP user: " + e.getMessage());
			xmpp.connect(myJid, getApplicationContext());
			return;
		}

	}

	protected void login(String myJid) {
		// If you try to login without being connected, then try to connect
		// again.
		if (!xmppConnection.isConnected()) {
			Log.e("XMPPIntentService.login()", "Can't login: not connected");

			// Not connected, so attempt to connect.
			xmpp.connect(myJid, getApplicationContext());
			return;
		}

		if (xmppConnection.isAuthenticated()) {
			Log.e("XMPPIntentService.login()", "XMPP already authenticated");
			return;
		}

		try {
			xmppConnection.login(myJid, myJid);
		} catch (XMPPException e) {
			// Show the error, but don't do anything about it yet.
			Log.e("XMPPIntentService.login()", "Can't login: " + e.getMessage());
			return;
		}

		if (xmppConnection.isAuthenticated()) {
			Log.d("XMPPIntentService.login()", "Logged into XMPP");
			// If login succeeded, then we're done. Let this IntentService die.
			// Do nothing.
		} else {
			// If login failed, then start over from connect().
			xmpp.connect(myJid, getApplicationContext());
		}
	}

	protected void logout() {

	}

	protected void joinMuc(String mucName, String myJid) {
		MultiUserChat muc = mucs.get(mucName);

		if (!xmppConnection.isConnected()) {
			Log.e("XMPPPIntentService.joinMuc()",
					"Failed to join muc: not connected");
			return;
		}

		if (!xmppConnection.isAuthenticated()) {
			Log.e("XMPPPIntentService.joinMuc()",
					"Failed to join muc: not authenticated");
			return;
		}

		if (muc == null) {
			muc = new MultiUserChat(xmppConnection, mucName + "@conference."
					+ XMPP_SERVER_URL);
			mucs.put(mucName, muc);
		}

		try {
			if (muc.isJoined()) {
				muc.leave();
			}
			muc.join(myJid);
		} catch (XMPPException e) {
			Log.e("XMPPPIntentService.joinMuc()",
					"Failed to join muc: " + e.getMessage());
			return;
		}

		// Do this in the UI thread by broadcasting an intent to
		// MucBroadcastReceiver.
		Intent joinRoomBroadcastIntent = new Intent();
		joinRoomBroadcastIntent
				.setAction(MucBroadcastReceiver.PROCESS_RESPONSE);
		joinRoomBroadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		joinRoomBroadcastIntent.putExtra(Keys.MESSAGE, Keys.MUC_JOIN_ROOM);
		joinRoomBroadcastIntent.putExtra(Keys.MUC_NAME, mucName);
		sendBroadcast(joinRoomBroadcastIntent);
	}

	protected void leaveMuc(String mucName) {
		MultiUserChat muc = mucs.get(mucName);

		if (muc == null) {
			muc = new MultiUserChat(xmppConnection, mucName + "@conference."
					+ XMPP_SERVER_URL);
			mucs.put(mucName, muc);
		}

		muc.leave();
	}

	protected void sendMucMessage(String myJid, String mucName, String message) {
		MultiUserChat muc = mucs.get(mucName);

		if (muc == null) {
			joinMuc(mucName, myJid);
		}

		muc = mucs.get(mucName);

		try {
			muc.sendMessage(message);

			Intent sendMessageBroadcastIntent = new Intent();
			sendMessageBroadcastIntent
					.setAction(MucBroadcastReceiver.PROCESS_RESPONSE);
			sendMessageBroadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
			sendMessageBroadcastIntent.putExtra(Keys.MESSAGE,
					Keys.MUC_SEND_MESSAGE);
			sendMessageBroadcastIntent.putExtra(Keys.MUC_MESSAGE, message);
			sendBroadcast(sendMessageBroadcastIntent);

		} catch (XMPPException e) {
			Log.e("XMPPIntentService.sendMucMessage()",
					"Couldn't send XMPP muc message: " + e.getMessage());
		}
	}
}
