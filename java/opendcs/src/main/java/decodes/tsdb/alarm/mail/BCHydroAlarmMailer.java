package decodes.tsdb.alarm.mail;

import java.util.ArrayList;

import jakarta.mail.Message;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import decodes.tsdb.alarm.AlarmGroup;
import decodes.tsdb.alarm.EmailAddr;
import ilex.util.Logger;

/**
 * BC Hydro requires a particular alarm message format for their automated system
 * to ingest. The subject line contains the dynamic alarm info. The body is always
 * constant. The priority of the message is set depending on whether the string
 * contains "FAILURE" or "FATAL"=priority 1. Otherwise priority is set to 3.
 * @author mmaloney
 *
 */
public class BCHydroAlarmMailer 
	extends AlarmMailer
{

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
			Logger.instance().warning(module + " Cannot send alarms for group "
				+ group.getName() + " -- email list empty.");
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
			Logger.instance().info(module + " sent " + n + " alarm messages to group "
				+ group.getName());
 
		}
		catch (Exception ex) 
		{
			throw new MailerException("Error sending mail: " + ex.toString());
		}
	}
}
