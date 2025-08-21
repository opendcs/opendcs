/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
package decodes.decoder;

import java.util.Date;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

/**
TimeTruncateOperation operation:
t(m) means to truncate to previous minute.
t(m30) means to truncate to previous 30-minute interval
t(m15) means to truncate to previous 15-minute, interval, etc.
t(h) means to truncate to the previous hour.
*/
class TimeTruncateOperation extends DecodesOperation
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private char truncArg = 'm';     // Truncate minutes by default.
	private int count = 1;

	public char getType() { return 't'; }

	/**
	  Constructor.
	  @param  args complete string inside the parens
	  @throws ScriptFormatException if syntax error detected in arguments
	*/
	public TimeTruncateOperation(String args)
		throws ScriptFormatException
	{
		super(1);

		if (args == null || args.trim().length() == 0)
			truncArg = 'm';
		else
		{
			args = args.toLowerCase().trim();
			char c = args.charAt(0);
			if (c == 'm' || c == 'h')
				truncArg = c;
			else
				throw new ScriptFormatException(
					"Unrecognized argument time-truncate operation '"
					+ args + "'");
			if (args.length() > 1 && Character.isDigit(args.charAt(1)))
			{
				try { count = Integer.parseInt(args.substring(1)); }
				catch(NumberFormatException ex)
				{
					log.atWarn()
					   .setCause(ex)
					   .log("Bad count in time-truncate operator arg '{}' -- ignored.", args);
					count = 1;
				}
			}
		}
	}

	/**
	  Truncates the current time in the decoded message by the specified
	  amount.
	  The very first call within a given message will truncate both the
	  message time and the current data-time stamp. Subsequent calls will
	  only truncate the data time.
	  @param dd holds the raw data and context.
	  @param msg store decoded values here.
	  @throws DecoderException or subclass if error detected.
	*/
	public void execute(DataOperations dd, DecodedMessage msg) 
		throws DecoderException
	{
		Date messageTime = msg.getMessageTime();
		RecordedTimeStamp rts = msg.getTimer();
		log.trace("Executing Time Trunc({}{}) messageTime={}, dataTime={}",
				  truncArg, (count==1?"":""+count), messageTime, new Date(rts.getMsec()));
		if (!msg.timeWasTruncated)
		{
			long mmsec = messageTime.getTime();
			long sub = (truncArg == 'h')
				? (mmsec % 3600000L) : (mmsec % (60000L*count));
			mmsec -= sub;
			msg.truncateTime(new Date(mmsec));
			log.trace("Message time truncated to {}", messageTime);
		}
		rts.setSecond(0);
		if (truncArg == 'h')
			rts.setMinute(0);
		else if (count > 1)
			rts.setMinute(rts.getMinute() - (rts.getMinute()%count));
		log.trace("Data Time Stamp truncated to {}", new Date(rts.getMsec()));
	}
}
