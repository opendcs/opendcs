/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. 
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package decodes.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;

import lrgs.common.DcpMsgFlag;
import lrgs.ldds.PasswordChecker;
import decodes.decoder.FunctionList;
import decodes.gui.TopFrame;
import ilex.util.Logger;
import decodes.launcher.AlarmEditLauncherAction;
import decodes.launcher.EventMonLauncherAction;
import decodes.launcher.LauncherAction;
import decodes.tsdb.groupedit.TsDbGrpEditor;
import decodes.util.DecodesVersion;
import ilex.util.EnvExpander;

public class ResourceFactory
{
	private static ResourceFactory _instance = null;
	
	protected ResourceFactory()
	{
	}

	public static ResourceFactory instance()
	{
		if (_instance == null)
		{
			_instance = new decodes.util.ResourceFactory();
			_instance.initializeFunctionList();
		}
		return _instance;
	}

	public lrgs.gui.SearchCriteriaEditorIF getSearchCriteriaEditor(File f)
		throws IOException
	{
		if (f == null)
			return new lrgs.gui.SearchCriteriaEditFrame();
		else
			return new lrgs.gui.SearchCriteriaEditFrame(f);
	}
	
	public int getFlagRev()
	{
		return DcpMsgFlag.myFlagRev;
	}

	public String getDdsVersionSuffix()
	{
		return "";
	}

	/**
	 * Initializes the decoder's function list with canned functions.
	 */
	public void initializeFunctionList()
	{
		FunctionList.addFunction(new decodes.decoder.CsvFunction());
		FunctionList.addFunction(new decodes.decoder.Nos6Min());
		FunctionList.addFunction(new decodes.decoder.NosHourly());
		FunctionList.addFunction(new decodes.decoder.ShefProcess());
		FunctionList.addFunction(new decodes.decoder.SetMissingFunction());
	}

	public JDialog getAboutDialog(JFrame parent, String appAbbr, String appName)
	{
		return new decodes.gui.AboutBox(parent, appAbbr, appName);
	}

	public String startTag()
	{
		return DecodesVersion.startupTag();
	}

	public TopFrame getTsdbEditorFrame(String myArgs[])
		throws Exception
	{
		TsDbGrpEditor tsGrpEditor = new TsDbGrpEditor();
		tsGrpEditor.setExitOnClose(false);
		tsGrpEditor.execute(myArgs);
		return tsGrpEditor.getFrame();
	}

	public String getIconPath()
	{
		return EnvExpander.expand("$DCSTOOL_HOME/icons/setup48x48.gif");
	}
	
	public JButton[] additionalSetupButtons()
	{
		return new JButton[0];
	}
	
	public void initDbResources()
		throws decodes.db.DatabaseException
	{
		DcpMsgFlag.setFlagRev(0x4b);
	}
	
	public PasswordChecker getPasswordChecker()
	{
		return null;
	}
	
	public ArrayList<LauncherAction> getDacqLauncherActions()
	{
Logger.instance().info("getDacqLauncherActions");
		ArrayList<LauncherAction> ret = new ArrayList<LauncherAction>();
		if (DecodesSettings.instance().showEventMonitor)
			ret.add(new EventMonLauncherAction());
		if (DecodesSettings.instance().showAlarmEditor)
			ret.add(new AlarmEditLauncherAction());

		return ret;
	}
}
