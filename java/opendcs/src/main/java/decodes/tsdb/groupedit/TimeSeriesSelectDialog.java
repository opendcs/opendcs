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
package decodes.tsdb.groupedit;

import ilex.util.LoadResourceBundle;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JPanel;

import decodes.gui.GuiDialog;
import decodes.gui.TopFrame;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.util.DecodesSettings;


/**
Dialog for selecting one or more Data Descriptor.
Used by the TsGroupDefinitionPanel.
*/
@SuppressWarnings("serial")
public class TimeSeriesSelectDialog extends GuiDialog
{
	private JPanel panel1 = new JPanel();
	private JPanel jPanel1 = new JPanel();
	private FlowLayout flowLayout1 = new FlowLayout();
	private JButton selectButton = new JButton();
	private JButton cancelButton = new JButton();

	private BorderLayout borderLayout1 = new BorderLayout();
	private JPanel jPanel2 = new JPanel();
	private BorderLayout borderLayout2 = new BorderLayout();
	private TsListSelectPanel ddSelectPanel;
	private TimeSeriesIdentifier dd;
	private boolean cancelled;

	//Labels for internationalization
	private String panelTitle;

	/** Constructs new TsDataDescriptorSelectDialog
	 * @param parent TODO*/
	public TimeSeriesSelectDialog(TimeSeriesDb tsdbIn, boolean fillAll, TopFrame parent)
	{
	  	super(parent, "", true);
	  	init(tsdbIn, fillAll);
	  	super.trackChanges("TimeSeriesSelectDialog");
	}

	public TimeSeriesSelectDialog(TimeSeriesDb tsdbIn, boolean fillAll, GuiDialog parent)
	{
		super(parent, "", true);
	  	init(tsdbIn, fillAll);
	  	super.trackChanges("TimeSeriesSelectDialog");
	}

	public void setTimeSeriesList(Collection<TimeSeriesIdentifier> ddsIn)
	{
		ddSelectPanel.setTimeSeriesList(ddsIn);
	}

	private void init(TimeSeriesDb tsDb, boolean fillAll)
	{
		dd = null;
		setAllLabels();
		ddSelectPanel = new TsListSelectPanel(tsDb, true, fillAll);
		jbInit();
		getRootPane().setDefaultButton(selectButton);
		pack();

		cancelled = false;
	}

	private void setAllLabels()
	{
		ResourceBundle groupResources = LoadResourceBundle.getLabelDescriptions(
			"decodes/resources/groupedit",
			DecodesSettings.instance().language);
		ResourceBundle genericResources = LoadResourceBundle.getLabelDescriptions(
			"decodes/resources/generic",
			DecodesSettings.instance().language);

		panelTitle =
			groupResources.getString("TsDataDescriptorSelectDialog.panelTitle");
		selectButton.setText(genericResources.getString("select"));
		cancelButton.setText(genericResources.getString("cancel"));
	}

	/** Initialize GUI components. */
	void jbInit()
	{
        panel1.setLayout(borderLayout1);
        jPanel1.setLayout(flowLayout1);
        selectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectButton_actionPerformed(e);
            }
        });
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancelButton_actionPerformed(e);
            }
        });
        flowLayout1.setHgap(35);
        flowLayout1.setVgap(10);
        this.setModal(true);
        this.setTitle(panelTitle);
        jPanel2.setLayout(borderLayout2);
        getContentPane().add(panel1);
        panel1.add(jPanel1, BorderLayout.SOUTH);
        jPanel1.add(selectButton, null);
        jPanel1.add(cancelButton, null);
        panel1.add(jPanel2, BorderLayout.CENTER);
        jPanel2.add(ddSelectPanel, BorderLayout.CENTER);
    }

	/**
	  Called when Select button is pressed.
	  @param e ignored
	*/
    void selectButton_actionPerformed(ActionEvent e)
	{
    	dd = ddSelectPanel.getSelectedTSID();
		closeDlg();
    }

	/** Closes dialog. */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/**
	  Called when Cancel button is pressed.
	  @param e ignored
	*/
    void cancelButton_actionPerformed(ActionEvent e)
	{
		dd = null;
		cancelled = true;
		closeDlg();
    }

	/** @return selected (single) data descriptor, or null if Cancel was pressed. */
	public TimeSeriesIdentifier getSelectedDD()
	{
		// Will return null if none selected
		return dd;
	}

	/** @return selected (multiple) data descriptor, or empty array if none. */
	public TimeSeriesIdentifier[] getSelectedDataDescriptors()
	{
		if (cancelled)
			return new TimeSeriesIdentifier[0];
		return ddSelectPanel.getSelectedTSIDs();
	}

	public void setSelectedTS(TimeSeriesIdentifier tsid)
	{
		ddSelectPanel.setSelection(tsid);
	}

	/**
	  Called with true if multiple selection is to be allowed.
	  @param ok true if multiple selection is to be allowed.
	*/
	public void setMultipleSelection(boolean ok)
	{
		ddSelectPanel.setMultipleSelection(ok);
	}

	public void refresh()
	{
		ddSelectPanel.refreshTSIDList();
	}

	public void clearSelection()
	{
		ddSelectPanel.clearSelection();
	}
}