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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import decodes.gui.GuiDialog;
import decodes.gui.TopFrame;
import decodes.tsdb.TsGroup;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.DecodesSettings;
import ilex.util.LoadResourceBundle;

/**
Dialog for selecting one or more groups.
Used by the TsGroupDefinitionPanel.
*/
public class TsGroupSelectDialog extends GuiDialog implements GroupSelector
{
	private JPanel panel1 = new JPanel();
	private JPanel southButtonPanel = new JPanel();
	private FlowLayout flowLayout1 = new FlowLayout();
	private JButton selectButton = new JButton();
	private JButton cancelButton = new JButton();

	private BorderLayout borderLayout1 = new BorderLayout();
	private JPanel jPanel2 = new JPanel();
	private BorderLayout borderLayout2 = new BorderLayout();
	private TitledBorder titledBorder1;
	private Border border1;
	private TsGroupListPanel groupsSelectPanel;
	private TsGroup group;
	private boolean cancelled;

	private String panelTitle;
	private String dialogTitle;
	private String selectLabel;
	private String cancelLabel;

	private boolean includeClearButton = false;

	/** Constructs new TsGroupSelectDialog */
	public TsGroupSelectDialog(TopFrame ownerFrame)
	{
		super(ownerFrame, "", true);
		init();
	}

	/** Constructs new TsGroupSelectDialog */
	public TsGroupSelectDialog(TopFrame ownerFrame, boolean includeClearButton)
	{
		super(ownerFrame, "", true);
		this.includeClearButton = includeClearButton;
		init();
	}

	private void init()
	{
		ResourceBundle groupResources = LoadResourceBundle.getLabelDescriptions(
			"decodes/resources/groupedit",
			DecodesSettings.instance().language);
		ResourceBundle genericResources = LoadResourceBundle.getLabelDescriptions(
			"decodes/resources/generic",
			DecodesSettings.instance().language);

		group = null;
		groupsSelectPanel = new TsGroupListPanel(TsdbAppTemplate.theDb, this, this);

		groupsSelectPanel.setTsGroupListFromDb();

		panelTitle = groupResources.getString(
			"TsGroupSelectDialog.panelTitle");
		dialogTitle = groupResources.getString(
			"TsGroupSelectDialog.dialogTitle");
		selectLabel = genericResources.getString("select");
		cancelLabel = genericResources.getString("cancel");


    	jbInit();
    	getRootPane().setDefaultButton(selectButton);
    	pack();

		cancelled = false;
	}


	/** Initialize GUI components. */
	void jbInit()
	{
		ResourceBundle genericResources = LoadResourceBundle.getLabelDescriptions(
			"decodes/resources/generic",
			DecodesSettings.instance().language);


		titledBorder1 =
        	new TitledBorder(BorderFactory.createLineBorder(
        			new Color(153, 153, 153),2),panelTitle);
        border1 = BorderFactory.createCompoundBorder(titledBorder1,
        			BorderFactory.createEmptyBorder(5,5,5,5));
        panel1.setLayout(borderLayout1);
        southButtonPanel.setLayout(flowLayout1);
        selectButton.setText(selectLabel);
        selectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectPressed();
            }
        });
        cancelButton.setText(cancelLabel);
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancelButton_actionPerformed(e);
            }
        });
        JButton clearButton = null;
        if (includeClearButton)
        {
        	clearButton = new JButton(genericResources.getString("clear"));
            cancelButton.addActionListener(
            	new java.awt.event.ActionListener()
            	{
            		public void actionPerformed(ActionEvent e)
            		{
            			clearButtonPressed();
            		}
            	});
        }

        flowLayout1.setHgap(35);
        flowLayout1.setVgap(10);
        this.setModal(true);
        this.setTitle(dialogTitle);
        jPanel2.setLayout(borderLayout2);
        jPanel2.setBorder(border1);
        getContentPane().add(panel1);
        panel1.add(southButtonPanel, BorderLayout.SOUTH);
        southButtonPanel.add(selectButton, null);
        if (clearButton != null)
        	southButtonPanel.add(clearButton, null);
        southButtonPanel.add(cancelButton, null);
        panel1.add(jPanel2, BorderLayout.CENTER);
        jPanel2.add(groupsSelectPanel, BorderLayout.CENTER);
    }

	protected void clearButtonPressed()
	{
		groupsSelectPanel.clearSelection();
		group = null;
		closeDlg();
	}

	/**
	  Called when Select button is pressed.
	  @param e ignored
	*/
    void selectPressed()
	{
    	group = groupsSelectPanel.getSelectedTsGroup();
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
		group = null;
		cancelled = true;
		closeDlg();
    }

	/** @return selected (single) group, or null if Cancel was pressed. */
	public TsGroup getSelectedGroup()
	{
		// Will return null if none selected
		return group;
	}

	/** @return selected (multiple) groups, or empty array if none. */
	public TsGroup[] getSelectedTsGroups()
	{
		if (cancelled)
			return new TsGroup[0];
		return groupsSelectPanel.getSelectedTsGroups();
	}

	/**
	  Called with true if multiple selection is to be allowed.
	  @param ok true if multiple selection is to be allowed.
	*/
	public void setMultipleSelection(boolean ok)
	{
		groupsSelectPanel.setMultipleSelection(ok);
	}

	@Override
	public void groupSelected()
	{
		selectPressed();
	}
}
