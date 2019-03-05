/*
 * $Id$
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log$
 * Revision 1.4  2017/10/03 19:10:35  mmaloney
 * bug fix
 *
 * Revision 1.3  2017/10/03 12:27:43  mmaloney
 * Allow unauthenticated connections to SMTP
 *
 * Revision 1.2  2017/05/17 20:37:12  mmaloney
 * First working version.
 *
 */
package decodes.tsdb.alarm.mail;

import ilex.util.Logger;
import ilex.util.PropertiesUtil;

import java.util.ArrayList;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import decodes.tsdb.CompAppInfo;
import decodes.tsdb.alarm.AlarmGroup;
import decodes.tsdb.alarm.AlarmMonitor;
import decodes.tsdb.alarm.EmailAddr;

public class AlarmMailer
{
	private Session session = null;
	private String fromAddr = null;
	private String fromName = null;
	private AlarmMonitor parent = null;
	
	public AlarmMailer(AlarmMonitor parent)
		throws MailerException 
	{
		this.parent = parent;
		Properties props = new Properties();
		CompAppInfo appInfo = parent.getAppInfo();
		
		String s = appInfo.getProperty("mail.smtp.auth");
		props.put("mail.smtp.auth", s != null ? s : "true");
		
		s = appInfo.getProperty("mail.smtp.starttls.enable");
		props.put("mail.smtp.starttls.enable", s != null ? s : "true");
		
		s = appInfo.getProperty("mail.smtp.host");
		if (s == null)
			throw new MailerException("Application " + appInfo.getAppName() 
				+ " missing required mail.smtp.host property.");
		props.put("mail.smtp.host", s);
		
		s = appInfo.getProperty("mail.smtp.port");
		props.put("mail.smtp.port", s != null ? s : "587");
		
		final String username = appInfo.getProperty("smtp.username");
//		if (username == null)
//			throw new MailerException("Application " + appInfo.getAppName() 
//				+ " missing required smtp.username property.");
		
		final String password = appInfo.getProperty("smtp.password");
//		if (password == null)
//			throw new MailerException("Application " + appInfo.getAppName() 
//				+ " missing required smtp.password property.");
		
		fromAddr = appInfo.getProperty("fromAddr");
		if (fromAddr == null)
			throw new MailerException("Application " + appInfo.getAppName() 
				+ " missing required fromAddr property.");

		fromName = appInfo.getProperty("fromName");
		if (fromName == null)
			throw new MailerException("Application " + appInfo.getAppName() 
				+ " missing required fromName property.");

		if (username != null && password != null)
		{
			session = Session.getInstance(props,
				new javax.mail.Authenticator() 
				{
					protected PasswordAuthentication getPasswordAuthentication()
					{
						return new PasswordAuthentication(username, password);
					}
				});
			parent.info("Created AlarmMailer with props: '" 
				+ PropertiesUtil.props2string(props)
				+ "' and username=" + username);
		}
		else
		{
			session = Session.getDefaultInstance(props);
			parent.info("Created unauthenticaed mailer with props: '"
				+ PropertiesUtil.props2string(props) + "'");
		}
	}

	public void send(AlarmGroup group, ArrayList<String> messages)
		throws MailerException
	{
		if (group.getEmailAddrs().size() == 0)
		{
			Logger.instance().warning("Cannot send alarms for group "
				+ group.getName() + " -- email list empty.");
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
			parent.info("Sent email with text: " + emailText);
 
		}
		catch (Exception ex) 
		{
			throw new MailerException("Error sending mail: " + ex.toString());
		}
	}
}
