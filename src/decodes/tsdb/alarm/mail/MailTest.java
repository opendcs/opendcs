/*
 * $Id$
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log$
 * Revision 1.2  2017/05/17 20:37:12  mmaloney
 * First working version.
 *
 */
package decodes.tsdb.alarm.mail;

import java.io.Console;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailTest
{

	public MailTest()
	{
	}
	
	public static void main(String args[])
		throws Exception
	{
//		final String username = "mjmilex@gmail.com";
		
		Console console = System.console();
		final String username = console.readLine("Enter user name: ");
		char pwchars[] = console.readPassword("Enter password: ");
		final String password = new String(pwchars);
		
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");
	
 
		Session session = Session.getInstance(props,
			new javax.mail.Authenticator() 
			{
				protected PasswordAuthentication getPasswordAuthentication()
				{
					return new PasswordAuthentication(username, password);
				}
			});
 
		try 
		{
 
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress("mjmilex@gmail.com","Mike Maloney"));
			message.setRecipients(Message.RecipientType.TO,
				InternetAddress.parse("mike@covesw.com"));
			message.setRecipients(Message.RecipientType.CC,
					InternetAddress.parse("mjmaloney@ilexeng.com"));
			message.setSubject("Testing Subject");
			message.setText("Dear Mike,"
				+ "\n\n No spam to my email, please!");
			Transport.send(message);
 
		} catch (Exception ex) 
		{
			throw new Exception(ex);
		}
	}

}
