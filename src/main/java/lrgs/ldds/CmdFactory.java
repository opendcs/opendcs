/*
*  $Id$
*/
package lrgs.ldds;

import java.io.OutputStream;
import java.io.IOException;

import ilex.util.ArrayUtil;

import lrgs.common.*;

/**
This class contains the factory method used to construct the DDS command 
objects on the server side.
*/
public class CmdFactory
{
	/** Constructor. */
	public CmdFactory()
	{
	}

	/**
	  Factory method: Pass it a message read from the client. This will
	  construct the appropriate concrete command subclass.
	  @param msg the message received from the client
	  @return an LddsCommand object that can be executed, or null if the
	   command type is unrecognized.
	*/
	public LddsCommand makeCommand(LddsMessage msg)
	{
		int i = 0;
		String fname;
		switch(msg.MsgId)
		{
		case LddsMessage.IdHello:
			return new CmdHello(msg.MsgData);

		case LddsMessage.IdGoodbye:
			return new CmdGoodbye();

		// STATUS command never implemented in legacy code
		case LddsMessage.IdStatus:
			return new CmdGetStatus();

		// START was intended to start push operations. It was never
		// implemented in the legacy code.
		//case LddsMessage.IdStart:
			//return new CmdNoop();
			//break;

		case LddsMessage.IdStop:
			return new CmdEcho(msg, LddsMessage.IdStop);

		case LddsMessage.IdDcp:
			return new CmdGetNextDcpMsg();

		case LddsMessage.IdCriteria:
			if (msg.MsgLength == 0 || (char)msg.MsgData[0] == '?')
				return new CmdSendSearchCrit();
			else if (msg.MsgLength <= 50)
				return new CmdRecvSearchCrit();
			else
				return new CmdRecvSearchCrit(
					ArrayUtil.getField(msg.MsgData, 50, msg.MsgLength-50));

		case LddsMessage.IdPutNetlist:
			for(i = 0; i < 64 && i < msg.MsgLength 
				&& (int)msg.MsgData[i] != 0; i++);
			fname = new String(msg.MsgData, 0, i);
			return new CmdRecvNetList(fname, 
				msg.MsgLength < 64 ? null 
					: ArrayUtil.getField(msg.MsgData, 64, msg.MsgLength - 64));

		case LddsMessage.IdGetNetlist:
			for(i = 0; i < 64 && i < msg.MsgLength 
				&& (int)msg.MsgData[i] != 0; i++);
			fname = new String(msg.MsgData, 0, i);
			return new CmdSendNetList(fname);

		case LddsMessage.IdIdle:
			return new CmdEcho(msg, LddsMessage.IdIdle);

		case LddsMessage.IdAuthHello:
			return new CmdAuthHello(msg.MsgData);

		case LddsMessage.IdDcpBlock:
			return new CmdGetDcpMsgBlock();

		case LddsMessage.IdEvents:
			return new CmdGetEvents();

		case LddsMessage.IdUser:
			return new CmdUser(msg.MsgData);

		case LddsMessage.IdRetConfig:
			return new CmdReturnConfig(msg.MsgData);

		case LddsMessage.IdInstConfig:
			return new CmdInstallConfig(msg.MsgData);

		case LddsMessage.IdDcpBlockExt:
			return new CmdGetMsgBlockExt();

		case LddsMessage.IdGetOutages:
			return new CmdGetOutages(msg.MsgData);

		case LddsMessage.IdAssertOutages:
			return new CmdAssertOutages(msg.MsgData);

		default:
			return null;
		}
	}
}

