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
package decodes.util;

import java.io.File;
import java.io.IOException;

import javax.swing.JButton;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.common.DcpMsgFlag;
import ilex.util.EnvExpander;

/**
 * @deprecated Do not add additional elements to this class unless we cannot find another way to handled the situation.
 */
@Deprecated
public class ResourceFactory
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private static ResourceFactory _instance = null;

	protected ResourceFactory()
	{
	}

	public static ResourceFactory instance()
	{
		if (_instance == null)
		{
			_instance = new decodes.util.ResourceFactory();
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

	// public JDialog getAboutDialog(JFrame parent, String appAbbr, String appName)
	// {
	// 	return new decodes.gui.AboutBox(parent, appAbbr, appName);
	// }

	public String startTag()
	{
		return DecodesVersion.startupTag();
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
}