/*
* Copyright 2014 Cove Software, LLC
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
package decodes.cwms.rating;

import ilex.util.LoadResourceBundle;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ResourceBundle;

import javax.swing.JPanel;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.gui.TopFrame;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.groupedit.TsDbGrpEditorTabbedPane;
import decodes.util.DecodesSettings;

/**
 * This is the main frame for the Time Series Database Group Editor.
 * The frame can contain the Time Series Groups Tab, or may include
 * the Time Series Data Descriptor Tab and the Alarms Tab.
 */
@SuppressWarnings("serial")
public class CwmsRatingGuiFrame extends TopFrame
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private JPanel contentPane;
	private JPanel tsGroupTab;
	private JPanel tsGroupListTab;
	private TsDbGrpEditorTabbedPane tsGroupListTabbedPane;
	private CwmsRatingListPanel ratingListPanel;
	private String frameTitle;
	private String listTabLabel;
	private TimeSeriesDb theDb = null;

	/**
	 * Construct the frame
	 */
	public CwmsRatingGuiFrame(TimeSeriesDb theDb)
	{
		this.theDb = theDb;
		ResourceBundle groupResources = LoadResourceBundle.getLabelDescriptions(
			"decodes/resources/groupedit",
			DecodesSettings.instance().language);
		ResourceBundle genericResources = LoadResourceBundle.getLabelDescriptions(
			"decodes/resources/generic",
			DecodesSettings.instance().language);

		frameTitle = "CWMS Rating List";
		listTabLabel = genericResources.getString("list");

		try
		{
			jbInit();
		}
		catch (Exception ex)
		{
			log.atError().setCause(ex).log("Unable to initialize GUI elements.");
		}
		trackChanges("CwmsRatingFrame");
	}

	/**
	 * Returns the time series DB object.
	 */
	public TimeSeriesDb getTsDb()
	{
		return theDb;
	}

	/** @return the tabbed pane for TS Groups */
	@Override
	public TsDbGrpEditorTabbedPane getTsGroupsListTabbedPane()
	{
		return tsGroupListTabbedPane;
	}

	/**
	 * Component initialization
	 * @throws Exception
	 */
	private void jbInit() throws Exception
	{
		//Initialize the components
		tsGroupTab = new JPanel();
		tsGroupListTab = new JPanel();
		tsGroupListTabbedPane = new TsDbGrpEditorTabbedPane();
		ratingListPanel = new CwmsRatingListPanel(this, theDb);

		//Set up the frame dimension
		this.setSize(new Dimension(863, 768));//763, 803 763, 760
		this.setTitle(frameTitle);
		contentPane = (JPanel) this.getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(tsGroupTab, BorderLayout.CENTER);
		tsGroupTab.setLayout(new BorderLayout());
		tsGroupTab.add(tsGroupListTabbedPane, BorderLayout.CENTER);
		tsGroupListTab.setLayout(new BorderLayout());
		tsGroupListTabbedPane.add(tsGroupListTab, listTabLabel);
		tsGroupListTab.add(ratingListPanel, BorderLayout.CENTER);
	}
}
