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

import ilex.util.PropertiesUtil;

import java.util.ArrayList;
import java.util.Properties;

import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.tsdb.alarm.AlarmGroup;
import decodes.tsdb.alarm.EmailAddr;

public class AlarmMailer
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	protected static String module = "AlarmMailer";
	protected Session session = null;
	protected String fromAddr = null;
	protected String fromName = null;
	protected Properties mailProps = null;
	
	/**
	 * The following properties should be supplied:
	 * <ul>
	 *   <li>mail.smtp.auth - true/false</li>
	 *   <li>mail.smtp.starttls.enable - true/false</li>
	 *   <li>mail.smtp.host - hostname or IP addr of mail server</li>
	 *   <li>mail.smtp.port - default=587</li>
	 *   <li>smtp.username</li>
	 *   <li>smtp.password</li>
	 *   <li>fromAddr - email address for the header FROM field</li>
	 *   <li>fromName</li>
	 *  </ul> 
	 *   
	 * @throws MailerException
	 */
	public AlarmMailer()
	{
	}
	
	/**
	 * 
	 * @param props
	 * @throws MailerException on an unrecoverable config error.
	 */
	public synchronized void configure(Properties props)
		throws MailerException
	{
		// Set defaults for missing props.
		String s = PropertiesUtil.getIgnoreCase(props, "mail.smtp.auth");
		if (s == null)
			props.put("mail.smtp.auth", "false");
		
		s = PropertiesUtil.getIgnoreCase(props, "mail.smtp.starttls.enable");
		if (s == null)
			props.put("mail.smtp.starttls.enable", "false");
		
		s = PropertiesUtil.getIgnoreCase(props, "mail.smtp.host");
		if (s == null)
			throw new MailerException("Missing required mail.smtp.host property.");
		
		s = PropertiesUtil.getIgnoreCase(props, "mail.smtp.port");
		if (s == null)
			props.put("mail.smtp.port", "587");
		
		
		fromAddr = PropertiesUtil.getIgnoreCase(props, "fromAddr");
		if (fromAddr == null)
			throw new MailerException("Missing required fromAddr property.");

		fromName = PropertiesUtil.getIgnoreCase(props, "fromName");
		if (fromName == null)
			throw new MailerException("Missing required fromName property.");
		
		// Save props and force next call to send() to re-establish the session.
		mailProps = props;
		session = null;
	}
	
	protected void makeSession()
	{
		final String username = PropertiesUtil.getIgnoreCase(mailProps, "smtp.username");
		final String password = PropertiesUtil.getIgnoreCase(mailProps, "smtp.password");
		
		if (username != null && password != null)
		{
			session = Session.getInstance(mailProps,
				new jakarta.mail.Authenticator() 
				{
					protected PasswordAuthentication getPasswordAuthentication()
					{
						return new PasswordAuthentication(username, password);
					}
				});
			log.debug("Created AlarmMailer with props: '{}' and username='{}'",
					  PropertiesUtil.props2string(mailProps), username);
		}
		else
		{
			Session.getDefaultInstance(mailProps);
			log.info("Created unauthenticated mailer with props: '{}'", PropertiesUtil.props2string(mailProps));
		}
	}

	public synchronized void send(AlarmGroup group, ArrayList<String> messages)
		throws MailerException
	{
		if (session == null)
			makeSession();
		
		if (group.getEmailAddrs().size() == 0)
		{
			log.warn(" Cannot send alarms for group {} -- email list empty.", group.getName());
			return;
		}
		
		try 
		{
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(fromAddr, fromName));
			
			InternetAddress addrs[] = new InternetAddress[group.getEmailAddrs().size()];
			int i=0;
			for(EmailAddr addr : group.getEmailAddrs())
				addrs[i++] = new InternetAddress(addr.getAddr());
				
			message.setRecipients(Message.RecipientType.TO, addrs);
			
			message.setSubject("Automated Alarms for group " + group.getName());
			
			StringBuilder sb = new StringBuilder();
			sb.append("The following alarms have been generated for group " + group.getName()
				+ "\n\n");
			for(String msg : messages)
				sb.append(msg + "\n\n");
			String emailText = sb.toString();
			message.setText(emailText);
			Transport.send(message);
			if (log.isTraceEnabled())
			{
				log.info("Sent email with text: '{}'", emailText);
			}
			else
			{
				log.info("Sent email.");
			}
 
		}
		catch (Exception ex) 
		{
			throw new MailerException("Error sending mail", ex);
		}
	}
}
