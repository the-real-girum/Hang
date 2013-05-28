package com.hangapp.newandroid.activity;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.hangapp.newandroid.R;
import com.hangapp.newandroid.database.Database;
import com.hangapp.newandroid.network.xmpp.XMPP;
import com.hangapp.newandroid.util.BaseFragmentActivity;
import com.hangapp.newandroid.util.Keys;

public class ChatActivity extends BaseFragmentActivity implements
		PacketListener {

	private EditText editTextChatMessage;
	private ListView listViewChatCells;
	private MessageAdapter adapter;

	private String mucName;
	private List<Message> messages = new ArrayList<Message>();

	private Database database;
	private XMPP xmpp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);

		// Enable the "Up" button.
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// Instantiate dependencies
		database = Database.getInstance();
		xmpp = XMPP.getInstance();

		mucName = getIntent().getStringExtra(Keys.HOST_JID);

		// Join the Muc.
		String myJid = database.getMyJid();
		xmpp.joinMuc(mucName, myJid, this);

		// Reference Views.
		editTextChatMessage = (EditText) findViewById(R.id.editTextChatMessage);
		listViewChatCells = (ListView) findViewById(R.id.listViewChatCells);

		// Setup adapter.
		adapter = new MessageAdapter(this, R.id.listViewChatCells, messages);
		listViewChatCells.setAdapter(adapter);
	}

	@Override
	public void processPacket(Packet packet) {
		if (packet instanceof Message) {
			Message message = (Message) packet;
			Log.i("ChatActivity.processPacket",
					"Got message: " + message.getBody());

			messages.add(message);
		}
	}

	public void sendMessage(View v) {
		xmpp.sendMessage(mucName, editTextChatMessage.getText().toString());
		editTextChatMessage.setText("");
	}

	static class MessageAdapter extends ArrayAdapter<Message> {

		public MessageAdapter(Context context, int textViewResourceId,
				List<Message> messages) {
			super(context, textViewResourceId, messages);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Message message = getItem(position);

			// TODO: Inflate the correct Type of cell, since it could be either
			// an incoming message or outgoing message.
			if (convertView == null) {
				convertView = LayoutInflater.from(getContext()).inflate(
						R.layout.cell_incoming_message, null);
			}

			// Reference Views.
			TextView textViewMessageBody = (TextView) convertView
					.findViewById(R.id.textViewMessageBody);
			TextView textViewMessageFrom = (TextView) convertView
					.findViewById(R.id.textViewMessageFrom);

			// Populate Views.
			textViewMessageBody.setText(message.getBody());
			textViewMessageFrom.setText(message.getFrom());

			return convertView;
		}
	}
}
