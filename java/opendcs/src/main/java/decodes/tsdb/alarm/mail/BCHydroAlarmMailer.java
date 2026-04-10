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
package decodes.tsdb.alarm.mail;

import java.util.ArrayList;

import jakarta.mail.Message;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.tsdb.alarm.AlarmGroup;
import decodes.tsdb.alarm.EmailAddr;

/**
 * BC Hydro requires a particular alarm message format for their automated system
 * to ingest. The subject line contains the dynamic alarm info. The body is always
 * constant. The priority of the message is set depending on whether the string
 * contains "FAILURE" or "FATAL"=priority 1. Otherwise priority is set to 3.
 * @author mmaloney
 *
 */
public class BCHydroAlarmMailer extends AlarmMailer
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	public BCHydroAlarmMailer()
	{
		module = "BCHydroAlarmMailer";
	}
	
	public synchronized void send(AlarmGroup group, ArrayList<String> messages)
			throws MailerException
	{
		if (session == null)
			makeSession();
		
		if (group.getEmailAddrs().size() == 0)
		{
			log.warn("Cannot send alarms for group {} -- email list empty.", group.getName());
			return;
		}
		
		try 
		{
			InternetAddress addrs[] = new InternetAddress[group.getEmailAddrs().size()];
			int i=0;
			for(EmailAddr addr : group.getEmailAddrs())
				addrs[i++] = new InternetAddress(addr.getAddr());				
			
			String msgBody = "Configuration Item: Hydroclimate\n"
					+ "Occurrence Category: Degradation\n"
					+ "Occurrence Type: Availability\n"
					+ "Assignment group: AS3 – GSO Support\n";
			int n = 0;
			for(String msg : messages)
			{
				Message message = new MimeMessage(session);
				message.setFrom(new InternetAddress(fromAddr, fromName));
				message.setRecipients(Message.RecipientType.TO, addrs);
				
				message.setSubject(msg);
				message.setText(msgBody);
				if (msg.contains("FAILURE") || msg.contains("FATAL"))
					message.setHeader("X-Priority", "3");
				else // either a WARNING or INFO = priority 1
					message.setHeader("X-Priority", "1");
				Transport.send(message);
				n++;
			}
			log.info("sent {} alarm messages to group {}.", n, group.getName());
 
		}
		catch (Exception ex) 
		{
			throw new MailerException("Error sending mail.", ex);
		}
	}
}
