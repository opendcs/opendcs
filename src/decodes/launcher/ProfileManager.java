package decodes.launcher;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import decodes.tsdb.TsdbAppTemplate;
import ilex.util.Logger;
import lrgs.gui.DecodesInterface;

public class ProfileManager 
	extends TsdbAppTemplate
{
	public static String module = "ProfileManager";
	private ProfileManagerFrame pmFrame = null;
	private boolean exitOnClose = true;


	public ProfileManager()
	{
		super("gui.log");
		this.noExitAfterRunApp = true;
	}

	@Override
	protected void runApp() throws Exception
	{
		pmFrame = new ProfileManagerFrame();
		pmFrame.load();
		pmFrame.setVisible(true);
		pmFrame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				close();
			}
		});

	}
	
	public void setExitOnClose(boolean exitOnClose)
	{
		this.exitOnClose = exitOnClose;
	}


	protected void close()
	{
		if (pmFrame != null)
			pmFrame.dispose();
		if (exitOnClose)
			System.exit(0);
	}

	public static void main(String[] args)
	{
		DecodesInterface.setGUI(true);
		ProfileManager guiApp = new ProfileManager();
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

	public ProfileManagerFrame getFrame()
	{
		return pmFrame;
	}
	
	/** Have to overload execute() -- we don't want to connect to DB. */
	public void execute(String args[])
		throws Exception
	{
		addCustomArgs(cmdLineArgs);
		parseArgs(args);
		runApp();
	}


}
