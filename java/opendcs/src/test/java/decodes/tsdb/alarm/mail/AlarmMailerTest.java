package decodes.tsdb.alarm.mail;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Properties;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import decodes.tsdb.alarm.AlarmGroup;
import decodes.tsdb.alarm.EmailAddr;
import decodes.sql.DbKey;


public class AlarmMailerTest {

	private static GreenMail smtpServer;
	private AlarmMailer alarmMailer;
	private Properties props;
    private final String fromAddr = "karl@example.org";
    private final static String fromName = "Karl";
    private final String toAddr = "mike@example.org";

	@BeforeAll
	public static void startSmtpServer() {
		smtpServer = new GreenMail(new ServerSetup(3025, null, "smtp"));
		smtpServer.start();
	}

	@AfterAll
	public static void stopSmtpServer() {
		smtpServer.stop();
	}

	@BeforeEach
	public void setUp() {
		alarmMailer = new AlarmMailer();
		props = new Properties();
		props.put("mail.smtp.auth", "false");
		props.put("mail.smtp.starttls.enable", "false");
		props.put("mail.smtp.host", "localhost");
		props.put("mail.smtp.port", "3025");
		props.put("fromAddr", fromAddr);
		props.put("fromName", fromName);
	}


	@Test
	public void testSend() throws Exception {
		alarmMailer.configure(props);

		AlarmGroup group = new AlarmGroup(DbKey.NullKey);
		group.setName("Test Group");
		ArrayList<EmailAddr> emailAddrs = new ArrayList<>();
		emailAddrs.add(new EmailAddr(toAddr));
		group.setEmailAddrs(emailAddrs);

		ArrayList<String> messages = new ArrayList<>();
		messages.add("Test message 1");
		messages.add("Test message 2");

		alarmMailer.send(group, messages);

		MimeMessage[] receivedMessages = smtpServer.getReceivedMessages();
		assertEquals(1, receivedMessages.length, "Should have received one email");

		MimeMessage email = receivedMessages[0];
		assertEquals("Automated Alarms for group Test Group", email.getSubject());
		assertEquals(fromName+" <"+fromAddr+">", email.getFrom()[0].toString());
		assertEquals(toAddr, email.getAllRecipients()[0].toString());

		String expectedBody = "The following alarms have been generated for group Test Group\n\nTest message 1\n\nTest message 2";
        String actualBody = email.getContent().toString().trim().replaceAll("\r", "");
        assertEquals(expectedBody.length(), actualBody.length()," length of strings");
		assertEquals(expectedBody, actualBody);
	}
}