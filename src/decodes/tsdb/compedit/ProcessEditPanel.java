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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.swing.*;
import javax.swing.border.*;

import opendcs.dai.LoadingAppDAI;
import ilex.util.LoadResourceBundle;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import decodes.db.Constants;
import decodes.db.EnumValue;
import decodes.sql.DbKey;
import decodes.tsdb.*;
import decodes.tsdb.procmonitor.ProcessEditDialog;
import decodes.util.DecodesSettings;
import decodes.util.PropertiesOwner;
import decodes.gui.EnumComboBox;
import decodes.gui.PropertiesEditPanel;

@SuppressWarnings("serial")
public class ProcessEditPanel extends EditPanel 
{
	private JPanel paramPanel = null;
	private JLabel processNameLabel = null;
	private JTextField nameField = null;
	private JTextField idField = null;
	private JPanel commentsPanel = null;
	private JScrollPane commentsScrollPane = null;
	private JTextArea commentsText = null;
	private CompAppInfo editedObject;
	private Properties panelProps = new Properties();
	private PropertiesEditPanel propsPanel;
	private JCheckBox manualEditCheck = null;
	private EnumComboBox processTypeCombo = new EnumComboBox("ApplicationType", "");
	private ResourceBundle genericDescriptions = null, compeditDescriptions = null;
	
	/** Will be set for CAPEdit, but not for Process Status GUI */
	private ProcessesListPanel listPanel = null;
	private ProcessEditDialog parentDialog = null;

	public ProcessEditPanel(ProcessesListPanel listPanel)
	{
		this.listPanel = listPanel;
		DecodesSettings settings = DecodesSettings.instance();
		genericDescriptions = LoadResourceBundle.getLabelDescriptions(
				"decodes/resources/generic", settings.language);
		compeditDescriptions =  LoadResourceBundle.getLabelDescriptions(
				"decodes/resources/compedit", settings.language);

		manualEditCheck = new JCheckBox(compeditDescriptions.getString("ProcessEditPanel.ManualCheckBox"), false);
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
			idField.setText(compeditDescriptions.getString("ProcessEditPanel.NA"));
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
		processTypeSelected();
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
		paramPanel = new JPanel(new GridBagLayout());

		processNameLabel = new JLabel(compeditDescriptions.getString("ProcessEditPanel.ProcNameLabel"));
		paramPanel.add(processNameLabel,
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(20, 20, 5, 2), 0, 0));

		nameField = new JTextField();
		nameField.setToolTipText(compeditDescriptions.getString("ProcessEditPanel.ProcNameLabelTT"));
		paramPanel.add(nameField,
			new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(20, 0, 5, 10), 60, 0));

		paramPanel.add(new JLabel(compeditDescriptions.getString("ProcessEditPanel.ProcIDLabel")),
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 20, 5, 2), 0, 0));

		idField = new JTextField();
		idField.setEditable(false);
		idField.setToolTipText(compeditDescriptions.getString("ProcessEditPanel.ProcIDLabelTT"));
		paramPanel.add(idField,
			new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 0, 5, 10), 30, 0));

		paramPanel.add(new JLabel(compeditDescriptions.getString("ProcessEditPanel.ProcessType")),
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 20, 5, 2), 0, 0));
		paramPanel.add(processTypeCombo,
			new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 0, 5, 10), 30, 0));
		
		manualEditCheck.setToolTipText(
			compeditDescriptions.getString("ProcessEditPanel.ManualCheckBoxTT"));
		paramPanel.add(manualEditCheck,
			new GridBagConstraints(0, 3, 2, 1, 0.0, 0.5,
				GridBagConstraints.NORTH, GridBagConstraints.NONE,
				new Insets(5, 20, 5, 20), 0, 0));

		propsPanel = new PropertiesEditPanel(panelProps);
		propsPanel.setTitle(" "+compeditDescriptions.getString("ProcessEditPanel.PropsPanelTitle")+" ");
		
		if (listPanel != null)
			propsPanel.setOwnerFrame(CAPEdit.instance().getFrame());
		else if (parentDialog != null)
			propsPanel.setOwnerDialog(parentDialog);

		paramPanel.add(propsPanel,
			new GridBagConstraints(2, 0, 1, 4, 1.0, 0.5,
				GridBagConstraints.WEST, GridBagConstraints.BOTH,
				new Insets(5, 0, 5, 10), 40, 0));

		paramPanel.add(getComments(),
			new GridBagConstraints(0, 4, 3, 1, 0.5, 0.5,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(10, 10, 10, 10), 0, 0));
		
		processTypeCombo.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					processTypeSelected();
				}
			});
			
		return paramPanel;
	}

	protected void processTypeSelected()
	{
		String pt = processTypeCombo.getSelection();
		if (pt == null || pt.trim().length() == 0)
		{
			propsPanel.setPropertiesOwner(null);
			return;
		}

		EnumValue procType = processTypeCombo.getSelectedEnumValue();
		String execClassName = procType.getExecClassName();
		PropertiesOwner propOwner = null;
		try
		{
			Class<?> execClass = null;
			if (execClassName != null && execClassName.trim().length() > 0)
			{
				execClass = procType.getExecClass();
				propOwner = (PropertiesOwner)execClass.newInstance();
				propsPanel.setPropertiesOwner(propOwner);
			}
		}
		catch(ClassCastException ex)
		{
			// ClassCastException if exec class doesn't implement PropertiesOwner
		}
		catch (Exception ex)
		{
			String msg = "Cannot instantiate PropertiesOwner class for '" + pt + "': " + ex;
			Logger.instance().warning(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
		propsPanel.setPropertiesOwner(propOwner);
	}

	/**
	 * This method initializes jScrollPane to contain the comments
	 * 
	 * @return javax.swing.JScrollPane
	 */
	private JScrollPane getCommentsScrollPane() {
		if (commentsScrollPane == null) {
			commentsScrollPane = new JScrollPane();
			commentsScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			commentsScrollPane.setViewportView(getCommentsText());
			commentsScrollPane.setToolTipText(compeditDescriptions.getString("ProcessEditPanel.ScrollPaneTT"));
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
			commentsText.setToolTipText(compeditDescriptions.getString("ProcessEditPanel.CommentTextTT"));
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
					new Font(compeditDescriptions.getString("ProcessEditPanel.CommentPanelBorder2"), Font.BOLD, 12), 
					new Color(51,51,51)));
			commentsPanel.setPreferredSize(new Dimension(10,100));
			commentsPanel.add(getCommentsScrollPane(), BorderLayout.CENTER);
		}
		return commentsPanel;
	}

	public void doCommit()
	{
		String nm = nameField.getText().trim();
		if (nm.length() == 0)
		{
			showError(compeditDescriptions.getString("ProcessEditPanel.CommitError1"));
			return;
		}

		String oldNm = editedObject == null ? null : editedObject.getAppName();
		LoadingAppDAI loadingAppDao = decodes.db.Database.getDb().getDbIo().makeLoadingAppDAO();
		try 
		{
			if (oldNm != null && TextUtil.strCompareIgnoreCase(nm, oldNm) != 0)
			{
				// The name has changed. Make sure the new name doesn't clash with an existing proc.
				try 
				{
					if (loadingAppDao.getComputationApp(nm) != null)
					{
						showError(compeditDescriptions.getString("ProcessEditPanel.CommitError2"));
						return;
					}
				}
				catch (NoSuchObjectException e)
				{
					// This is ok -- it means there is no existing proc with that name -- no clash.
				}
			}
	
			saveToObject(editedObject);
		
			loadingAppDao.writeComputationApp(editedObject); 
			idField.setText("" + editedObject.getAppId());
			if (listPanel != null)
				listPanel.procTableModel.fill();
		}
		catch(DbIoException ex)
		{
			showError(compeditDescriptions.getString("ProcessEditPanel.CommitError3")+" " + ex);
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

	public void doClose()
	{
		CompAppInfo testCopy = editedObject.copyNoId();
		saveToObject(testCopy);
		if (!editedObject.equalsNoId(testCopy))
		{
			int r = JOptionPane.showConfirmDialog(this, compeditDescriptions.getString("ProcessEditPanel.CloseConfirm"));
			if (r == JOptionPane.CANCEL_OPTION)
				return;
			else if (r == JOptionPane.YES_OPTION)
				doCommit();
		}
		if (listPanel != null)
		{
			// This will be true in CAPEdit but not in Process Status GUI
			JTabbedPane tabbedPane = CAPEdit.instance().getProcessesTab();
			tabbedPane.remove(this);
		}
		else if (parentDialog != null)
			parentDialog.closeDlg();
	}

	public ResourceBundle getGenericDescriptions()
	{
		return genericDescriptions;
	}

	public void setParentDialog(ProcessEditDialog parentDialog)
	{
		this.parentDialog = parentDialog;
	}
}
