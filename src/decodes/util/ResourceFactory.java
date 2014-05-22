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
import javax.swing.JDialog;
import javax.swing.JFrame;

import decodes.decoder.FunctionList;
import decodes.gui.TopFrame;
import decodes.tsdb.groupedit.TsDbGrpEditor;
import decodes.util.DecodesVersion;

import ilex.util.EnvExpander;
import ilex.util.Logger;


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
			String clsname = System.getProperty("ResourceFactory");
			if (clsname == null)
			{
				_instance = new decodes.util.ResourceFactory();
				Logger.instance().debug1("Using OpenSource Resource Factory.");
			}
			else
			{
				try
				{
					ClassLoader cl = 
						Thread.currentThread().getContextClassLoader();
					Class<?> cls = cl.loadClass(clsname);
					_instance = (ResourceFactory)cls.newInstance();
					Logger.instance().debug1("Using Resource Factory: " + clsname);
				}
				catch(Exception ex)
				{
					_instance = new decodes.util.ResourceFactory();
					Logger.instance().debug1("Using OpenSource Resource Factory.");
				}
			}
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

}
