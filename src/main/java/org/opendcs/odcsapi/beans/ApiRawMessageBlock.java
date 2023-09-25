package org.opendcs.odcsapi.beans;

import java.util.ArrayList;

public class ApiRawMessageBlock
{
	private ArrayList<ApiRawMessage> messages = new ArrayList<ApiRawMessage>();
	
	private boolean moreToFollow = true;

	public ArrayList<ApiRawMessage> getMessages()
	{
		return messages;
	}

	public void setMessages(ArrayList<ApiRawMessage> messages)
	{
		this.messages = messages;
	}

	public boolean isMoreToFollow()
	{
		return moreToFollow;
	}

	public void setMoreToFollow(boolean moreToFollow)
	{
		this.moreToFollow = moreToFollow;
	}
}
