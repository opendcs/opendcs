/*
 * $Id$
 * 
 * $Log$
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.2  2012/10/30 15:46:37  mmaloney
 * dev
 *
 * Revision 1.1  2012/10/30 01:59:27  mmaloney
 * First cut of rating GUI.
 *
 * This software was written by Cove Software, LLC ("COVE") under contract 
 * to the United States Government. 
 * 
 * No warranty is provided or implied other than specific contractual terms
 * between COVE and the U.S. Government
 * 
 * Copyright 2016 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
 */
package decodes.cwms.rating;

import ilex.util.LoadResourceBundle;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ResourceBundle;

import javax.swing.JPanel;

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
		catch (Exception e)
		{
			e.printStackTrace();
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
