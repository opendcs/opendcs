/*
* $Id$
*/
package lrgs.rtstat;

import ilex.util.LoadResourceBundle;

import java.util.ResourceBundle;

import javax.swing.*;

import decodes.util.DecodesSettings;

/**
Main class for the Real Time Summary Status Applications.
*/
public class RtSummaryStat
{
	private static ResourceBundle labels = null;
	private static ResourceBundle genericLabels = null;

	boolean packFrame = false;

	RtStatCmdLineArgs cmdLineArgs = new RtStatCmdLineArgs();

	//Construct the application
	public RtSummaryStat(String args[])
	{
		cmdLineArgs.scan_arg.setDefaultValue(new Integer(30));
		cmdLineArgs.parseArgs(args);
		getMyLabelDescriptions();
		final RtSummaryStatFrame frame = new RtSummaryStatFrame(
			cmdLineArgs.getScanPeriod(), cmdLineArgs.getLrgsMonUrl());

		if (packFrame)
		{
			frame.pack();
		}
		else
		{
			frame.validate();
		}
		frame.startThread();
		frame.setVisible(true);
	}

	public static void getMyLabelDescriptions()
	{
		DecodesSettings settings = DecodesSettings.instance();
		//Load the generic properties file - includes labels that are used
		//in multiple screens
		genericLabels = LoadResourceBundle.getLabelDescriptions(
				"decodes/resources/generic",
				settings.language);
		//Return the main label descriptions for Rt summary App
		//it uses the rtstat properties file
		labels = LoadResourceBundle.getLabelDescriptions(
				"decodes/resources/rtstat",
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
		new RtSummaryStat(args);
	}
}
