package decodes.tsdb.alarm.mail;

import ilex.util.Logger;
import ilex.util.PropertiesUtil;

import java.util.ArrayList;
import java.util.Properties;



import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Authenticator;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import decodes.tsdb.alarm.AlarmGroup;
import decodes.tsdb.alarm.EmailAddr;

public class AlarmMailer
{
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
	 * @param props
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
				new Authenticator() 
				{
					protected PasswordAuthentication getPasswordAuthentication()
					{
						return new PasswordAuthentication(username, password);
					}
				});
			Logger.instance().debug1(module + " Created AlarmMailer with props: '" 
				+ PropertiesUtil.props2string(mailProps)
				+ "' and username=" + username);
		}
		else
		{
			session = Session.getDefaultInstance(mailProps);
			Logger.instance().info(module + " Created unauthenticaed mailer with props: '"
				+ PropertiesUtil.props2string(mailProps) + "'");
		}
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
			Logger.instance().info(module + " Sent email with text: " + emailText);
 
		}
		catch (Exception ex) 
		{
			throw new MailerException("Error sending mail: " + ex.toString());
		}
	}
}
