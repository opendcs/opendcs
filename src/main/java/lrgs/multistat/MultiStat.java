package lrgs.multistat;

import javax.swing.*;

import org.slf4j.LoggerFactory;
import static org.slf4j.helpers.Util.getCallingClass;

import java.io.IOException;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.FileLogger;
import ilex.util.QueueLogger;
import ilex.util.TeeLogger;
import ilex.util.IndexRangeException;

public class MultiStat
{
	private static org.slf4j.Logger logger = LoggerFactory.getLogger(getCallingClass());

	boolean packFrame = false;
	MultiStatFrame msFrame = null;
	public static final String module = "GUI";
	public static final int EVT_CANT_CONNECT_LRGS1 = 1;
	public static final int EVT_CANT_CONNECT_LRGS2 = 2;
	public static final int EVT_CANT_CONNECT_LRGS3 = 3;
	public static final int EVT_CANT_CONNECT_LRIT = 4;
	
	public static MultiStat _instance = null;

	//Construct the application
	public MultiStat()
	{
		msFrame = new MultiStatFrame();
		if (packFrame)
			msFrame.pack();
		else
			msFrame.validate();

		//Make the frame pretty much full screen.
//		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
//		msFrame.setBounds(20, 20, d.width-40, d.height-80);
		msFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		_instance = this;
	}

	public void run()
	{
		msFrame.setVisible(true);

		// Set up a log queue and a monitoring thread.
		Logger defLogger;
		try
		{
			defLogger = new FileLogger("multistat", "multistat.log",Logger.E_INFORMATION);
		}
		catch(IOException ex)
		{
			logger.error("Cannot create log file 'multistat.log'. Sending logs to default log.");
			defLogger = Logger.instance();
		}
		QueueLogger qLogger = new QueueLogger("local",defLogger.getMinLogPriority());
		TeeLogger tLogger = new TeeLogger("local", defLogger, qLogger);
		Logger.setLogger(tLogger);
		LogMonitor logMon = new LogMonitor(msFrame, qLogger);
		logMon.start();

		msFrame.startup();
		long lastAlarmPurge = System.currentTimeMillis();
		msFrame.alarmMaskList.load();
		
		ConfigCheckerThread cct = new ConfigCheckerThread();
		cct.start();
		
		while(true)
		{
			long now = System.currentTimeMillis();
			try { Thread.sleep(2000L); }
			catch(InterruptedException ex) {}

			if (now - lastAlarmPurge > 3600000L) // every hour
				msFrame.alarmList.cancelledAlarmList.purgeOld();

			if (msFrame.muteCheck.isSelected()
			 && now - msFrame.mutedAt > 30 * 60 * 1000L) // 30 min.
			{
				SwingUtilities.invokeLater(
					new Runnable()
					{
						public void run()
						{
							msFrame.muteCheck.setSelected(false);
							msFrame.muteCheck_actionPerformed(null);
						}
					});
			}

			msFrame.alarmMaskList.check();
		}
	}

	//Main method
	public static void main(String[] args)
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		
		MultiStatCmdLineArgs mscla = new MultiStatCmdLineArgs();
		mscla.parseArgs(args);
		
		String cfgName = EnvExpander.expand(mscla.getConfigFileName());
		MultiStatConfig.instance().setConfigFileName(cfgName);

		Logger.instance().info("Debug Min Priority (info=3) = " 
			+ Logger.instance().getMinLogPriority());
		MultiStat ms = new MultiStat();
		ms.run();
	}
}

class ConfigCheckerThread extends Thread
{
	private boolean isShutdown = false;
	
	

	public void run()
	{
		long lastConfigCheck = 0L;
		try { sleep(3000L); } catch(Exception ex) {}
		
		while(!isShutdown)
		{
			if (System.currentTimeMillis() - lastConfigCheck > 10000L)
			{
				lastConfigCheck = System.currentTimeMillis();
				if (MultiStatConfig.instance().checkConfig())
					MultiStat._instance.msFrame.restart();
			}
			try { sleep(1000L); }
			catch(InterruptedException ex) {}
		}
	}

	public void shutdown()
	{
		isShutdown = true;
	}
}

class LogMonitor extends Thread
{
	private boolean isShutdown = false;
	private MultiStatFrame msframe;
	QueueLogger qlog;

	public LogMonitor(MultiStatFrame msframe, QueueLogger qlog)
	{
		this.msframe = msframe;
		this.qlog = qlog;
		setName("LogMonitor");
	}

	public void run()
	{
		int idx = qlog.getStartIdx();
		while(!isShutdown)
		{
			try { sleep(1000L); }
			catch(InterruptedException ex) {}

			String msg;
			try
			{
				while((msg = qlog.getMsg(idx)) != null)
				{
					msframe.addEvent(msg, "local");
					idx++;
				}
			}
			catch(IndexRangeException ex)
			{
				idx = qlog.getStartIdx();
			}
		}
	}

	public void shutdown()
	{
		isShutdown = true;
	}
}
