/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
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
		cmdLineArgs.scan_arg.setDefaultValue(Integer.valueOf(30));
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
	public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		new RtSummaryStat(args);
	}
}
