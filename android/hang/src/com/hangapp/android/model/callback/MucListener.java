package com.hangapp.android.model.callback;

import java.util.List;

import org.jivesoftware.smack.packet.Message;

public interface MucListener {

	public void onMucMessageUpdate(String mucName, List<Message> messages);
}