package decodes.tsdb.procmonitor;

import ilex.util.EnvExpander;
import ilex.util.Logger;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;

import lrgs.gui.DecodesInterface;

import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TsdbAppTemplate;

public class ProcessMonitor 
	extends TsdbAppTemplate
{
	private static String module = "procmon";
	private ProcessMonitorFrame pmFrame = null;
	private boolean exitOnClose = true;
	private DbPollThread dbPollThread = null;

	public ProcessMonitor()
	{
		super(module + ".log");
		dbPollThread = new DbPollThread(this);
	}

	@Override
	protected void runApp() throws Exception
	{
		pmFrame = new ProcessMonitorFrame();
		ImageIcon titleIcon = new ImageIcon(
				EnvExpander.expand("$DECODES_INSTALL_DIR/icons/toolkit24x24.gif"));
		pmFrame.setIconImage(titleIcon.getImage());
//		pmFrame.centerOnScreen();
		
//System.out.println("starting dbPollThread");
		dbPollThread.start();
		pmFrame.setVisible(true);
		
		pmFrame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				close();
			}
		});
		noExitAfterRunApp = true;
	}
	
	private void close()
	{
		dbPollThread.shutdown();
		if (pmFrame != null)
			pmFrame.dispose();
		if (exitOnClose)
			System.exit(0);
	}


	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		DecodesInterface.setGUI(true);
		ProcessMonitor guiApp = new ProcessMonitor();
		try
		{			
			guiApp.execute(args);
		} 
		catch (Exception ex)
		{
			String msg = module + " Can not initialize. " + ex.getMessage();
			Logger.instance().failure(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
	}

	public void setExitOnClose(boolean exitOnClose)
	{
		this.exitOnClose = exitOnClose;
	}

	public TimeSeriesDb getTsdb() { return theDb; }
	
	public ProcessMonitorFrame getFrame() { return pmFrame; }

	@Override
	public void createDatabase()
	{
		// ProcessMonitor must work with XML DECODES databases too.
		// So don't create the TSDB interface.
	}
	
	@Override
	public void tryConnect()
	{
		// Likewise, don't connect to the TSDB. Just DECODES DB.
	}
}
