package decodes.polling;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;

/**
 * For use as a simulator in TestPollScriptServer
 * This class just dumps some simulated data at the client.
 */
public class PollScriptDataSim extends PollScriptCommand
{
	public PollScriptDataSim(PollScriptProtocol owner, String cmdLine)
	{
		super(owner, cmdLine);
	}
	
	@Override
	public void execute() throws ProtocolException
	{
		try
		{
			owner.getIoPort().getOut().write("TIMEZONE=UTC\r\n".getBytes());
			owner.getIoPort().getOut().write(":STAGE \r\n".getBytes());
			Random random = new Random();
			double stage = random.nextDouble() * 100.;
			Calendar cal = Calendar.getInstance();
			cal.setTimeZone(TimeZone.getTimeZone("UTC"));
			NumberFormat nf = NumberFormat.getNumberInstance();
			nf.setGroupingUsed(false);
			nf.setMaximumFractionDigits(3);
			long now = System.currentTimeMillis();
			now = (now / (30*60000L)) * (30*60000L);
			cal.setTime(new Date(now));
			cal.add(Calendar.MINUTE, -30*7);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:00");
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			for(int i=0; i<8; i++)
			{
				String line = sdf.format(cal.getTime()) + " " + nf.format(stage) + "\r\n";
				owner.getIoPort().getOut().write(line.getBytes());
				cal.add(Calendar.MINUTE, 30);
				stage = stage + (random.nextDouble() * .5) - .25;
			}
			
			owner.getIoPort().getOut().write(":PRECIP \r\n".getBytes());
			double precip = random.nextDouble() * 200.;
			cal.setTime(new Date(now));
			cal.add(Calendar.MINUTE, -30*7);
			for(int i=0; i<8; i++)
			{
				String line = sdf.format(cal.getTime()) + " " + nf.format(precip) + "\r\n";
				owner.getIoPort().getOut().write(line.getBytes());
				cal.add(Calendar.MINUTE, 30);
				precip = precip + (random.nextDouble() * .4);
			}

			owner.getIoPort().getOut().write(":BATTERY \r\n".getBytes());
			double battery = 12 + random.nextDouble() * 3.;
			cal.setTime(new Date(now));
			String line = sdf.format(cal.getTime()) + " " + nf.format(battery) + "\r\n";
			owner.getIoPort().getOut().write(line.getBytes());
		}
		catch (IOException e)
		{
			throw new ProtocolException(e.toString());
		}
	}

}
