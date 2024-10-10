package decodes.platstat;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import lrgs.gui.DecodesInterface;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TsdbAppTemplate;

public class PlatformMonitor extends TsdbAppTemplate
{
	public static final String module = "PlatformMonitor";
	private boolean exitOnClose = true;
	private PlatformMonitorFrame frame = null;
	private DbPollThread dbPollThread = null;


	public PlatformMonitor()
	{
		super("platmon.log");
	}

	@Override
	protected void runApp() throws Exception
	{
		frame = new PlatformMonitorFrame(this);
		dbPollThread = new DbPollThread(this);
		ImageIcon titleIcon = new ImageIcon(
				EnvExpander.expand("$DECODES_INSTALL_DIR/icons/toolkit24x24.gif"));
		frame.setIconImage(titleIcon.getImage());
		frame.setVisible(true);
		
		frame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				close();
			}
		});
		frame.setDefaults();
		
		noExitAfterRunApp = true;
		dbPollThread.start();
	}
	
	public void setExitOnClose(boolean exitOnClose)
	{
		this.exitOnClose = exitOnClose;
	}

	public TimeSeriesDb getTsdb() { return theDb; }

	public void close()
	{
		dbPollThread.shutdown();
		if (frame != null)
			frame.dispose();
		if (exitOnClose)
			System.exit(0);
	}


	public static void main(String[] args)
	{
		DecodesInterface.setGUI(true);
		PlatformMonitor guiApp = new PlatformMonitor();
		try
		{
			guiApp.setExitOnClose(true);
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

	public PlatformMonitorFrame getFrame()
	{
		return frame;
	}
	
	@Override
	public void createDatabase()
	{
		// PlatformMonitor must work with XML DECODES databases too.
		// So don't create the TSDB interface.
	}
	
	@Override
	public void tryConnect()
	{
		// Likewise, don't connect to the TSDB. Just DECODES DB.
	}
}
