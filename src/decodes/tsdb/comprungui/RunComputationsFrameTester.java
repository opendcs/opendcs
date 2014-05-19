package decodes.tsdb.comprungui;

import java.util.ResourceBundle;

import lrgs.gui.DecodesInterface;

import ilex.cmdline.BooleanToken;
import ilex.cmdline.TokenOptions;
import ilex.util.LoadResourceBundle;
import decodes.tsdb.*;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesSettings;

/**
 * This class is the main for the Test Computations Frame. 
 */
public class RunComputationsFrameTester extends TsdbAppTemplate
{
	private static ResourceBundle labels = null;
	private static ResourceBundle genericLabels = null;
	
	
	static BooleanToken NoCompFilterToken
		= new BooleanToken("L", "Disable Computation List filter (default=on)",
			"", TokenOptions.optSwitch, false);
	RunComputationsFrame myframe;
	
	/** Constructor */
	public RunComputationsFrameTester()
	{
		super(null);
	
		
	}
	
	/** Runs the GUI */
	public void runApp()
	{
		getMyLabelDescriptions();
		
		myframe = new RunComputationsFrame(true);
		myframe.setRunCompFrametester(this);
		myframe.setVisible(true);
		myframe.setDb(this.theDb);
	}
	
	public static void getMyLabelDescriptions()
	{
		DecodesSettings settings = DecodesSettings.instance();
		//Load the generic properties file - includes labels that are used
		//in multiple screens
		genericLabels = LoadResourceBundle.getLabelDescriptions(
				"decodes/resources/generic",
				settings.language);
		//Return the main label descriptions for Run Computation App
		labels = LoadResourceBundle.getLabelDescriptions(
				"decodes/resources/comprun",
				settings.language);	
	}
	
	public static ResourceBundle getLabels() 
	{
		if (labels == null)
			getMyLabelDescriptions();
		return labels;
	}

	public static ResourceBundle getGenericLabels() 
	{
		if (genericLabels == null)
			getMyLabelDescriptions();
		return genericLabels;
	}
	
	/**
	 * This method adds a command line argument to allow
	 * the user to turn off the Db Computations list filter.
	 * By default is on.
	 */
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(NoCompFilterToken);
		appNameArg.setDefaultValue("runcomp");
	}
	
	/**
	 * Get the RunComputationsFrame
	 * @return runComputationFrame
	 */
	public RunComputationsFrame getFrame() 
	{ 
		return myframe; 
	}
	
	/** Main method. Used when running from the rumcomp script */
	public static void main(String[] args)
	{
		DecodesInterface.setGUI(true);
		RunComputationsFrameTester mytester = new RunComputationsFrameTester();
		try{mytester.execute(args);}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
	}
	
	
	//returns NoCompFilter token value from cmdLineArgums . 
	public boolean getNoCompFilterToken()
	{
	
		return NoCompFilterToken.getValue();
	}
	
}
