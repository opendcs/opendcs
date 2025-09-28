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
package decodes.tsdb.comprungui;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.Vector;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.sql.DbKey;
import ilex.cmdline.StringToken;
import lrgs.gui.DecodesInterface;

import ilex.cmdline.BooleanToken;
import ilex.cmdline.TokenOptions;
import ilex.util.LoadResourceBundle;
import decodes.tsdb.*;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesSettings;
import opendcs.dai.ComputationDAI;

/**
 * This class is the main for the Test Computations Frame.
 */
public class RunComputationsFrameTester extends TsdbAppTemplate
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private static ResourceBundle labels = null;
	private static ResourceBundle genericLabels = null;

	private String dateSpec = "yyyy/MM/dd-HH:mm:ss";
	private Vector<DbComputation> specifiedComps = new Vector<DbComputation>();
	private Date since = null, until = null;
	private SimpleDateFormat sdf = new SimpleDateFormat(dateSpec);

	static BooleanToken NoCompFilterToken
		= new BooleanToken("L", "Disable Computation List filter (default=on)",
			"", TokenOptions.optSwitch, false);
	private StringToken compIdToken = new StringToken("C", "Computation ID(s)", "",
			TokenOptions.optSwitch|TokenOptions.optMultiple, null);
	private StringToken sinceToken = new StringToken("S", "Since Time in " + dateSpec, "",
			TokenOptions.optSwitch, null);
	private StringToken untilToken = new StringToken("U", "Until Time in " + dateSpec, "",
			TokenOptions.optSwitch, null);
	CompRunGuiFrame myframe;

	/** Constructor */
	public RunComputationsFrameTester()
	{
		super(null);


	}

	/** Runs the GUI */
	public void runApp()
	{
		getMyLabelDescriptions();
		for(int idx = 0; idx < compIdToken.NumberOfValues(); idx++)
			if (compIdToken.getValue(idx) != null) {
                try {
                    loadComp(compIdToken.getValue(idx));
                } catch (DbIoException e) {
                    throw new RuntimeException(e);
                }
            }
		if (sinceToken.getValue() != null)
		{
			try { since = sdf.parse(sinceToken.getValue()); }
			catch(ParseException ex)
			{
				log.atError()
				   .setCause(ex)
				   .log("Invalid Since Argument '{}' -- format must be '{}' UTC", sinceToken, dateSpec);
				System.exit(1);
			}
		}
		if (untilToken.getValue() != null)
		{
			try { until = sdf.parse(untilToken.getValue()); }
			catch(ParseException ex)
			{
				log.atError()
				   .setCause(ex)
				   .log("Invalid Until Argument '{}' -- format must be '{}' UTC", untilToken, dateSpec);
				System.exit(1);
			}
		}

		myframe = new CompRunGuiFrame(true, specifiedComps, since, until);
		myframe.setRunCompFrametester(this);
		myframe.setVisible(true);
		myframe.setDb(this.theDb);

		noExitAfterRunApp = true;
	}

	private void loadComp(String nameOrId)
			throws DbIoException
	{
		ComputationDAI computationDAO = theDb.makeComputationDAO();
		try
		{
			DbKey compId = DbKey.createDbKey(Long.parseLong(nameOrId.trim()));
			specifiedComps.add(computationDAO.getComputationById(compId));
		}
		catch(Exception ex)
		{
			try {
				specifiedComps.add(computationDAO.getComputationByName(nameOrId.trim()));
			} catch (NoSuchObjectException ex2) {
				ex2.addSuppressed(ex);
				log.atError().setCause(ex).log("No matching computation ID or name for '{}'", nameOrId);
				System.exit(1);
				;
			}
		}
		finally
		{
			computationDAO.close();
		}

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
		cmdLineArgs.addToken(compIdToken);
		cmdLineArgs.addToken(sinceToken);
		cmdLineArgs.addToken(untilToken);
		appNameArg.setDefaultValue("compedit");
	}

	/**
	 * Get the RunComputationsFrame
	 * @return runComputationFrame
	 */
	public CompRunGuiFrame getFrame()
	{
		return myframe;
	}

	/** Main method. Used when running from the rumcomp script */
	public static void main(String[] args)
	{
		DecodesInterface.setGUI(true);
		RunComputationsFrameTester mytester = new RunComputationsFrameTester();
		try{mytester.execute(args);}
		catch(Exception ex)
		{
			log.atError().setCause(ex).log("Unable to execute application.");
		}
	}


	//returns NoCompFilter token value from cmdLineArgums .
	public boolean getNoCompFilterToken()
	{

		return NoCompFilterToken.getValue();
	}

}