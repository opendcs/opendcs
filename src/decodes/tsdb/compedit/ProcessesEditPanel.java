/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package decodes.tsdb.compedit;

import java.awt.*;
import java.util.Properties;
import javax.swing.*;
import javax.swing.border.*;

import opendcs.dai.LoadingAppDAI;

import ilex.util.PropertiesUtil;
import decodes.db.Constants;
import decodes.sql.DbKey;
import decodes.tsdb.*;
import decodes.gui.EnumComboBox;
import decodes.gui.PropertiesEditPanel;

public class ProcessesEditPanel extends EditPanel 
{
	private JPanel mainPanel = null;
	private JPanel paramPanel = null;
	private JLabel processNameLabel = null;
	private JTextField nameField = null;
	private JLabel processIdLabel = null;
	private JTextField idField = null;
	private JPanel commentsPanel = null;
	private JScrollPane commentsScrollPane = null;
	private JTextArea commentsText = null;
	private CompAppInfo editedObject;
	private Properties panelProps = new Properties();
	private PropertiesEditPanel propsPanel;
	private JCheckBox manualEditCheck = new JCheckBox(CAPEdit.instance().compeditDescriptions
			.getString("ProcessEditPanel.ManualCheckBox"), false);
	private EnumComboBox processTypeCombo = new EnumComboBox("ApplicationType", "");
	
	public ProcessesEditPanel()
	{
		setLayout(new BorderLayout());
		this.add(getButtonPanel(), java.awt.BorderLayout.SOUTH);
		this.add(getParamPanel(), java.awt.BorderLayout.CENTER);
		editedObject = null;
	}
	
	public void setEditedObject(CompAppInfo cai)
	{
		editedObject = cai;

		// Fill in controls.
		nameField.setText(cai.getAppName());
		DbKey id = cai.getAppId();
		if (id.isNull())
			idField.setText(CAPEdit.instance().compeditDescriptions
					.getString("ProcessEditPanel.NA"));
		else
			idField.setText("" + id);
		commentsText.setText(cai.getComment());
		manualEditCheck.setSelected(cai.getManualEditApp());
		String s = cai.getProperty("appType");
		if (s != null)
			processTypeCombo.setSelectedItem(s);
		else
			processTypeCombo.setSelectedIndex(0);
		panelProps.clear();
		PropertiesUtil.copyProps(panelProps, cai.getProperties());
		panelProps.remove("appType");
		propsPanel.setPropertiesOwner(cai);
		propsPanel.setProperties(panelProps);
	}

	public CompAppInfo getEditedObject()
	{
		return editedObject;
	}

	/**
	 * This method initializes the top panel containing the editable fields
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getParamPanel() 
	{
		paramPanel = new JPanel();
		paramPanel.setLayout(new GridBagLayout());

		processNameLabel = new JLabel(CAPEdit.instance().compeditDescriptions
				.getString("ProcessEditPanel.ProcNameLabel"));
		paramPanel.add(processNameLabel,
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(20, 20, 5, 2), 0, 0));

		nameField = new JTextField();
		nameField.setToolTipText(CAPEdit.instance().compeditDescriptions
				.getString("ProcessEditPanel.ProcNameLabelTT"));
		paramPanel.add(nameField,
			new GridBagConstraints(1, 0, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(20, 0, 5, 10), 60, 0));

		processIdLabel = new JLabel(CAPEdit.instance().compeditDescriptions
				.getString("ProcessEditPanel.ProcIDLabel"));
		paramPanel.add(processIdLabel,
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 20, 5, 2), 0, 0));

		idField = new JTextField();
		idField.setEditable(false);
		idField.setToolTipText(CAPEdit.instance().compeditDescriptions
				.getString("ProcessEditPanel.ProcIDLabelTT"));
		paramPanel.add(idField,
			new GridBagConstraints(1, 1, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 0, 5, 10), 30, 0));

		paramPanel.add(new JLabel(
			CAPEdit.instance().compeditDescriptions.getString("ProcessEditPanel.ProcessType")),
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 20, 5, 2), 0, 0));
		paramPanel.add(processTypeCombo,
			new GridBagConstraints(1, 2, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 0, 5, 10), 30, 0));
		
		manualEditCheck.setToolTipText(
			CAPEdit.instance().compeditDescriptions.getString(
				"ProcessEditPanel.ManualCheckBoxTT"));
		paramPanel.add(manualEditCheck,
			new GridBagConstraints(0, 3, 2, 1, 0.0, 0.5,
				GridBagConstraints.NORTH, GridBagConstraints.NONE,
				new Insets(5, 20, 5, 20), 0, 0));

		propsPanel = new PropertiesEditPanel(panelProps);
		propsPanel.setTitle(" "+CAPEdit.instance().compeditDescriptions
				.getString("ProcessEditPanel.PropsPanelTitle")+" ");
		propsPanel.setOwnerFrame(CAPEdit.instance().getFrame());
		paramPanel.add(propsPanel,
			new GridBagConstraints(2, 0, 1, 4, 0.5, 0.5,
				GridBagConstraints.WEST, GridBagConstraints.BOTH,
				new Insets(5, 0, 5, 10), 40, 0));

		paramPanel.add(getComments(),
			new GridBagConstraints(0, 4, 3, 1, 0.5, 0.5,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(10, 10, 10, 10), 0, 0));
			
		return paramPanel;
	}

	/**
	 * This method initializes jScrollPane to contain the comments
	 * 
	 * @return javax.swing.JScrollPane
	 */
	private JScrollPane getCommentsScrollPane() {
		if (commentsScrollPane == null) {
			commentsScrollPane = new JScrollPane();
			commentsScrollPane.setHorizontalScrollBarPolicy(commentsScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			commentsScrollPane.setViewportView(getCommentsText());
			commentsScrollPane.setToolTipText(
					CAPEdit.instance().compeditDescriptions
					.getString("ProcessEditPanel.ScrollPaneTT"));
		}
		return commentsScrollPane;
	}

	/**
	 * This method initializes jTextArea for the comments
	 * 
	 * @return javax.swing.JTextArea
	 */
	private JTextArea getCommentsText() {
		if (commentsText == null) {
			commentsText = new JTextArea();
			commentsText.setWrapStyleWord(true);
			commentsText.setLineWrap(true);
			commentsText.setToolTipText(
					CAPEdit.instance().compeditDescriptions
					.getString("ProcessEditPanel.CommentTextTT"));
		}
		return commentsText;
	}
	
	/**
	 * This method initializes the comments panel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getComments() 
	{
		if (commentsPanel == null) {
			commentsPanel = new JPanel();
			commentsPanel.setLayout(new BorderLayout());
			commentsPanel.setSize(new Dimension(37,51));
			commentsPanel.setBorder(
				BorderFactory.createTitledBorder(
					BorderFactory.createLineBorder(Color.gray,2), 
					CAPEdit.instance().compeditDescriptions
					.getString("ProcessEditPanel.CommentPanelBorder1"), TitledBorder.DEFAULT_JUSTIFICATION, 
					TitledBorder.DEFAULT_POSITION, 
					new Font(CAPEdit.instance().compeditDescriptions
							.getString("ProcessEditPanel.CommentPanelBorder2"), Font.BOLD, 12), 
					new Color(51,51,51)));
			commentsPanel.setPreferredSize(new Dimension(10,100));
			commentsPanel.add(getCommentsScrollPane(), BorderLayout.CENTER);
		}
		return commentsPanel;
	}

	protected void doCommit()
	{
		String nm = nameField.getText().trim();
		if (nm.length() == 0)
		{
			showError(CAPEdit.instance().compeditDescriptions
					.getString("ProcessEditPanel.CommitError1"));
			return;
		}

		CompAppInfo existingProc = 
			CAPEdit.instance().processesListPanel.procTableModel.findByName(nm);
		if (existingProc != null
		 && editedObject.getAppId() == Constants.undefinedId)
		{
			showError(CAPEdit.instance().compeditDescriptions
					.getString("ProcessEditPanel.CommitError2"));
			return;
		}

		saveToObject(editedObject);
		
		LoadingAppDAI loadingAppDao = CAPEdit.theDb.makeLoadingAppDAO();
		try 
		{
			loadingAppDao.writeComputationApp(editedObject); 
			idField.setText("" + editedObject.getAppId());
			CAPEdit.instance().processesListPanel.procTableModel.fill();
		}
		catch(DbIoException ex)
		{
			showError(CAPEdit.instance().compeditDescriptions
					.getString("ProcessEditPanel.CommitError3")+" " + ex);
		}
		finally
		{
			loadingAppDao.close();
		}

	}

	private void saveToObject(CompAppInfo cai)
	{
		cai.setAppName(nameField.getText());
		cai.setComment(commentsText.getText());
		cai.setManualEditApp(manualEditCheck.isSelected());
		propsPanel.saveChanges();
		cai.setProperties(panelProps);
		String s = processTypeCombo.getSelection().trim();
		if (s.length() > 0)
			cai.setProperty("appType", s);
	}

	protected void doClose()
	{
		CompAppInfo testCopy = editedObject.copyNoId();
		saveToObject(testCopy);
		if (!editedObject.equalsNoId(testCopy))
		{
			int r = JOptionPane.showConfirmDialog(this, CAPEdit.instance().compeditDescriptions
					.getString("ProcessEditPanel.CloseConfirm"));
			if (r == JOptionPane.CANCEL_OPTION)
				return;
			else if (r == JOptionPane.YES_OPTION)
				doCommit();
		}
		JTabbedPane tabbedPane = CAPEdit.instance().getProcessesTab();
		tabbedPane.remove(this);
	}
}
