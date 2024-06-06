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
import java.awt.event.*;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;
import java.util.Iterator;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;

import opendcs.dai.AlgorithmDAI;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import decodes.gui.PropertiesEditPanel;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.tsdb.DbAlgoParm;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbCompAlgorithmScript;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.ScriptType;
import decodes.tsdb.algo.AWAlgoType;
import decodes.tsdb.algo.AW_AlgorithmBase;
import decodes.tsdb.algo.RoleTypes;
import decodes.util.DynamicPropertiesOwner;
import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;
import decodes.db.Constants;

@SuppressWarnings("serial")
public class AlgorithmsEditPanel 
	extends EditPanel
	implements PropertiesOwner, DynamicPropertiesOwner
{
	private JPanel inputPanel = null;
	private JTextField nameText = new JTextField();
	private JTextField idText = new JTextField();
	private JTextField execClassField = new JTextField();
	private JPanel commentPanel = null; // @jve:decl-index=0:visual-constraint="657,111"
	private JScrollPane commentsScroll = null;
	private JScrollPane tableScroll = null;
	private JTextArea commentsText = null;
	private PropertiesEditPanel propertiesPanel = null;
	private JPanel Parameters = null;
	private JPanel parametersButtonPanel = null;
	private JButton deleteParamButton = null;
	private JButton addParamButton = null;
	private JButton editParamButton = null;
	private JTable algoParmTable = null;
	private JTextField numCompsText = new JTextField();
	private AlgoParmTableModel algoParmTableModel = null;
	private JButton changeNameButton = null;
	private DbCompAlgorithm editedObject;
	private Properties propCopy = null;
	
	private String algoNameLabelText;
	private String changeButtonText;
	private String algoIDText;
	private String execLabelText;
	private String numCompsLabelText;
	private String nameToolTip;
	private String idToolTip;
	private String numCompsToolTip;
	private String execToolTipText;
	private String cmntToolTip;
	private String parametersText;
	private String dialogText;
	private String commitErr1;
	private String commitErr2;
	private String commitErr3;
	private String saveChangesText;
	private String chngNameInputText;
	private String chngNameErr1;
	private String chngNameErr2;
	private String addParmErr1;
	private String deleteParamErr1;
	private String deleteParamPrompt1;
	
	private JButton pythonButton = new JButton("Python");
	private ExecClassSelectDialog execSelectDialog = null;
	private PythonAlgoEditDialog pythonDialog = null;
	private PropertySpec[] staticPropSpecs = new PropertySpec[0];
	private HashMap<String, PropertySpec> dynamicPropSpecs = new HashMap<String, PropertySpec>();

	public AlgorithmsEditPanel()
	{
		setLayout(new BorderLayout());
		fillLabels();
		this.add(makeCenterPanel(), java.awt.BorderLayout.CENTER);
		JPanel southButtonPanel = getButtonPanel();
		southButtonPanel.add(pythonButton, 
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(0, 6, 6, 4), 0, 0));
		pythonButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					pythonButtonPressed();
				}
			});
		pythonButton.setEnabled(false);
		this.add(southButtonPanel, java.awt.BorderLayout.SOUTH);
		execClassField.getDocument().addDocumentListener(
			new DocumentListener()
			{
				@Override
				public void insertUpdate(DocumentEvent e)
				{
					pythonButton.setEnabled(
						execClassField.getText().trim().toLowerCase().contains("python"));
				}

				@Override
				public void removeUpdate(DocumentEvent e)
				{
					pythonButton.setEnabled(
						execClassField.getText().trim().toLowerCase().contains("python"));
				}

				@Override
				public void changedUpdate(DocumentEvent e)
				{
					pythonButton.setEnabled(
						execClassField.getText().trim().toLowerCase().contains("python"));
				}
			});
		
		
		editedObject = null;
		setTopFrame(CAPEdit.instance().getFrame());
		execSelectDialog = new ExecClassSelectDialog(CAPEdit.instance().getFrame());
		try
		{
			execSelectDialog.load();
		}
		catch (NoSuchObjectException e1)
		{
			execSelectDialog = null;
		}
	}
	
	protected void pythonButtonPressed()
	{
		if (pythonDialog == null)
		{
			pythonDialog = new PythonAlgoEditDialog(CAPEdit.instance().getFrame());
			pythonDialog.setPythonAlgo(editedObject);
		}
		
		CAPEdit.instance().getFrame().launchDialog(pythonDialog);
	}

	private void fillLabels()
	{
		algoNameLabelText=CAPEdit.instance().compeditDescriptions.getString("AlgorithmsEditPanel.NameLabel");
		changeButtonText=CAPEdit.instance().compeditDescriptions.getString("AlgorithmsEditPanel.ChangeButton");
		algoIDText=CAPEdit.instance().compeditDescriptions.getString("AlgorithmsEditPanel.IDLabel");
		execLabelText=CAPEdit.instance().compeditDescriptions.getString("AlgorithmsEditPanel.ExecLabel");
		numCompsLabelText = CAPEdit.instance().compeditDescriptions.getString("AlgorithmsEditPanel.NumComps");
		nameToolTip = CAPEdit.instance().compeditDescriptions.getString("AlgorithmsEditPanel.NameToolTip");
		idToolTip = CAPEdit.instance().compeditDescriptions.getString("AlgorithmsEditPanel.IDToolTip");
		numCompsToolTip = CAPEdit.instance().compeditDescriptions.getString("AlgorithmsEditPanel.NumCompsToolTip");
		execToolTipText = CAPEdit.instance().compeditDescriptions.getString("AlgorithmsEditPanel.ExecToolTip");
		cmntToolTip = CAPEdit.instance().compeditDescriptions.getString("AlgorithmsEditPanel.CommentToolTip");
		parametersText=CAPEdit.instance().compeditDescriptions.getString("AlgorithmsEditPanel.Parameters");
		dialogText = CAPEdit.instance().compeditDescriptions.getString("AlgorithmsEditPanel.Dialog");
		commitErr1= CAPEdit.instance().compeditDescriptions.getString("AlgorithmsEditPanel.CommitError1");
		commitErr2= CAPEdit.instance().compeditDescriptions.getString("AlgorithmsEditPanel.CommitError2");
		commitErr3= CAPEdit.instance().compeditDescriptions.getString("AlgorithmsEditPanel.CommitError3");
		saveChangesText = CAPEdit.instance().compeditDescriptions.getString("AlgorithmsEditPanel.SaveChanges");
		chngNameInputText = CAPEdit.instance().compeditDescriptions.getString("AlgorithmsEditPanel.ChangeNameInputDialog");
		chngNameErr1 = CAPEdit.instance().compeditDescriptions.getString("AlgorithmsEditPanel.ChangeNameError1");
		chngNameErr1 = CAPEdit.instance().compeditDescriptions.getString("AlgorithmsEditPanel.ChangeNameError2");
		addParmErr1 = CAPEdit.instance().compeditDescriptions.getString("AlgorithmsEditPanel.AddParamError1");
		deleteParamErr1=CAPEdit.instance().compeditDescriptions.getString("AlgorithmsEditPanel.DeleteParamError1");
		deleteParamPrompt1=CAPEdit.instance().compeditDescriptions.getString("AlgorithmsEditPanel.DeleteParamPrompt1");
	}
	
	private JPanel makeCenterPanel() 
	{
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		centerPanel.add(getInputPanel(), null);
		centerPanel.add(getcommentPanel(), null);
		centerPanel.add(getParameters(), null);
		centerPanel.add(getPropertiesPanel(), null);
		return centerPanel;
	}

	public void setEditedObject(DbCompAlgorithm dca)
	{
		editedObject = dca;

		// fill in controls:
		nameText.setText(editedObject.getName());
		idText.setText("" + editedObject.getId());
		execClassField.setText(editedObject.getExecClass());
		commentsText.setText(editedObject.getComment());
		numCompsText.setText("" + editedObject.getNumCompsUsing());

		propCopy = new Properties();
		PropertiesUtil.copyProps(propCopy, editedObject.getProperties());
		propertiesPanel.setProperties(propCopy);
		algoParmTableModel.fill(editedObject);
		String clsName = dca.getExecClass();
		try
		{
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			Logger.instance().debug3("Instantiating new algo exec '"
				+ clsName + "'");
			Class<?> cls = cl.loadClass(clsName);
			DbAlgorithmExecutive executive = (DbAlgorithmExecutive)cls.newInstance();
			if (executive instanceof AW_AlgorithmBase)
			{
				// Algorithm type set in initAWAlgorithm, this is needed to get correct property specs.
				((AW_AlgorithmBase)executive).initForGUI();
				staticPropSpecs = ((PropertiesOwner) executive).getSupportedProps();
			}
		}
		catch(Exception ex)
		{
			Logger.instance().warning("Cannot instantiate algorithm class '" + clsName + "': " + ex);
		}
Logger.instance().debug1("AlgoPanel.setEditedObject algo has " + editedObject.getScripts().size() + " scripts.");

		// Python dynamic properties are not defined in the algorithm record.
		// Tooltips are stored in the python Init script.
		DbCompAlgorithmScript script = editedObject.getScript(ScriptType.ToolTip);
		if (script != null)
		{
			Properties initProps = new Properties();
			try { initProps.load(new StringReader(script.getText())); }
			catch (IOException e) {}
			for(Object key : initProps.keySet())
			{
				String propName = (String)key;
				int idx = propName.toLowerCase().indexOf(".tooltip");
				if (idx > 0)
				{
					String algoPropName = propName.substring(0, idx);
					PropertySpec ps = new PropertySpec(algoPropName, PropertySpec.STRING,
						initProps.getProperty(propName));
					ps.setDynamic(true);
					dynamicPropSpecs.put(algoPropName.toUpperCase(), ps);
//System.out.println("Made new dynamicPropSpec: name=" + ps.getName() + ", desc=" + ps.getDescription());
				}
			}
		}
		
		propertiesPanel.setPropertiesOwner(this);
	}

	public DbCompAlgorithm getEditedObject()
	{
		return editedObject;
	}


	private JPanel getInputPanel() 
	{
		if (inputPanel == null) 
		{
			changeNameButton = new JButton(changeButtonText);
			changeNameButton.addActionListener(
				new java.awt.event.ActionListener()
				{
					public void actionPerformed(ActionEvent e) 
					{
						changeNameButtonPressed();
					}
				});

			inputPanel = new JPanel();
			inputPanel.setLayout(new GridBagLayout());
			inputPanel.add(new JLabel(algoNameLabelText),
				new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 10, 3, 2), 0, 0));
			
			nameText.setEditable(false);
			nameText.setToolTipText(nameToolTip);
			inputPanel.add(nameText, 
				new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
					new Insets(6, 0, 3, 2), 0, 0));
			
			inputPanel.add(changeNameButton, 
				new GridBagConstraints(2, 0,  1, 1, 0.0, 0.0, 
					GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
					new Insets(6, 2, 3, 10), 0, 0));

				
			inputPanel.add(new JLabel(algoIDText),
				new GridBagConstraints(3, 0,  1, 1, 0.0, 0.0, 
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 5, 3, 2), 0, 0));

			idText.setEditable(false);
			idText.setToolTipText(idToolTip);

			inputPanel.add(idText, 
				new GridBagConstraints(4, 0,  1, 1, 0.0, 0.0, 
					GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
					new Insets(6, 0, 3, 10), 0, 0));
				
			inputPanel.add(new JLabel(execLabelText),
				new GridBagConstraints(0, 1,  1, 1, 0.0, 0.0, 
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(3, 10, 6, 2), 0, 0));
		
			execClassField.setToolTipText(execToolTipText);

			inputPanel.add(execClassField, 
				new GridBagConstraints(1, 1,  1, 1, 1.0, 0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 6, 2), 0, 0));

			JButton selectButton = new JButton("Select");
			selectButton.addActionListener(
				new ActionListener()
				{
					@Override
					public void actionPerformed(ActionEvent e)
					{
						selectExecClassPressed();
					}
				});
			inputPanel.add(selectButton,
				new GridBagConstraints(2, 1,  1, 1, 0.0, 0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(3, 2, 6, 10), 0, 0));
			
				
			inputPanel.add(new JLabel(numCompsLabelText),
				new GridBagConstraints(3, 1,  1, 1, 0.0, 0.0, 
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(3, 10, 6, 2), 0, 0));
	
			numCompsText.setEditable(false);
			setToolTipText(numCompsToolTip);
			inputPanel.add(numCompsText, 
				new GridBagConstraints(4, 1,  1, 1, 1.0, 0.0, 
					GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
					new Insets(3, 0, 6, 10), 0, 0));
		}
		return inputPanel;
	}
	
	protected void selectExecClassPressed()
	{
		// If couldn't load the algorithms.txt lists, then revert to JOptionPane.
		if (execSelectDialog == null)
		{
			String cn = JOptionPane.showInputDialog(null, "Enter full Java class name with package prefixes:");
			if (cn == null)
				return;
			execClassField.setText(cn);
			return;
		}
		execSelectDialog.setSelection(null);
		String cls = execClassField.getText().trim();
		if (cls != null)
			execSelectDialog.setSelection(cls);
		CAPEdit.instance().getFrame().launchDialog(execSelectDialog);
		if (!execSelectDialog.wasCancelled() && execSelectDialog.getSelection() != null)
		{
			final DbCompAlgorithm algo = execSelectDialog.getSelection();
			execClassField.setText(algo.getExecClass());
		}
	}

	/**
	 * This method initializes jScrollPane to contain the comments
	 * 
	 * @return javax.swing.JScrollPane
	 */
	private JScrollPane getCommentsScroll() 
	{
		if (commentsScroll == null) 
		{
			commentsScroll = new JScrollPane();
			commentsScroll.setHorizontalScrollBarPolicy(
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			commentsScroll.setViewportView(getCommentsText());
		}
		return commentsScroll;
	}

	/**
	 * This method initializes jTextArea for the comments
	 * 
	 * @return javax.swing.JTextArea
	 */
	private JTextArea getCommentsText() {
		if (commentsText == null) {
			commentsText = new JTextArea();
			commentsText.setSize(new java.awt.Dimension(37, 400));
			commentsText.setMaximumSize(new java.awt.Dimension(37, 400));
			commentsText.setWrapStyleWord(true);
			commentsText.setLineWrap(true);
			commentsText.setToolTipText(cmntToolTip);
		}
		return commentsText;
	}

	/**
	 * This method initializes jPanel2
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getcommentPanel() {
		if (commentPanel == null) {
			commentPanel = new JPanel();
			commentPanel.setLayout(new BorderLayout());
			commentPanel.setSize(new java.awt.Dimension(37, 400));
			commentPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(
					javax.swing.BorderFactory.createLineBorder(
							java.awt.Color.gray, 2), "Comments",
					javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
					javax.swing.border.TitledBorder.DEFAULT_POSITION,
					new java.awt.Font("Dialog", java.awt.Font.BOLD, 12),
					new java.awt.Color(51, 51, 51)));
			commentPanel.setPreferredSize(new java.awt.Dimension(10, 100));
			commentPanel.add(getCommentsScroll(), java.awt.BorderLayout.CENTER);
			commentPanel.setSize(new java.awt.Dimension(Short.MAX_VALUE, 400));
			commentPanel.setPreferredSize(new java.awt.Dimension(Short.MAX_VALUE, 400));
			commentPanel.setMaximumSize(new java.awt.Dimension(Short.MAX_VALUE, 400));
			
		}
		return commentPanel;
	}

	private JScrollPane getTableScroll() {
		if (tableScroll == null) {
			tableScroll = new JScrollPane();
			tableScroll.setViewportView(getAlgoParmTable());
		}
		return tableScroll;
	}
	
	protected JTable getAlgoParmTable() 
	{
		if (algoParmTableModel == null) 
		{
			algoParmTableModel = new AlgoParmTableModel();
			algoParmTable = 
				new SortingListTable(algoParmTableModel,
					AlgoParmTableModel.columnWidths);
			algoParmTable.addMouseListener(new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() == 2)
					{
						editParamButtonPressed();
					}
				}
			} );

		}
		return algoParmTable;
	}

	protected void doCommit() 
	{
		String nm = nameText.getText().trim();
		if (nm.length() == 0)
		{
			showError(commitErr1);
			return;
		}

		AlgorithmsListPanel lp = CAPEdit.instance().algorithmsListPanel;
		if (lp.algoListTableModel.existsInList(nm)
		 && editedObject.getId() == Constants.undefinedId)
		{
			showError(commitErr2);
			return;
		}

		saveToObject(editedObject);

		AlgorithmDAI algorithmDao = CAPEdit.instance().theDb.makeAlgorithmDAO();
		try 
		{
			algorithmDao.writeAlgorithm(editedObject); 
			idText.setText("" + editedObject.getId());
			lp.algoListTableModel.fill();
		}
		catch(DbIoException ex)
		{
			showError(commitErr3 + ex);
		}
		finally
		{
			algorithmDao.close();
		}
	}

	private void saveToObject(DbCompAlgorithm ob)
	{
		String nm = nameText.getText().trim();
		ob.setName(nm);
		ob.setComment(commentsText.getText());
		ob.setExecClass(execClassField.getText().trim());
		propertiesPanel.saveChanges();
		ob.getProperties().clear();
		PropertiesUtil.copyProps(ob.getProperties(), propCopy);
		algoParmTableModel.saveTo(ob);
		if (pythonButton.isEnabled())
		{
			if (pythonDialog != null)
				pythonDialog.saveToObject(ob);
			DbCompAlgorithmScript script = null;
			if (dynamicPropSpecs.values().size() > 0)
			{
				script = new DbCompAlgorithmScript(ob, ScriptType.ToolTip);
				ob.putScript(script);
				for(PropertySpec ps : dynamicPropSpecs.values())
					script.addToText(ps.getName()+".tooltip=" + ps.getDescription()	+ "\n");
//				System.out.println("AlgoEdit.save: ttscript:\n" + script.getText());
			}
		}
	}

	protected void doClose() 
	{
		DbCompAlgorithm testCopy = editedObject.copyNoId();
		saveToObject(testCopy);
		if (!editedObject.equalsNoId(testCopy))
		{
			int r = JOptionPane.showConfirmDialog(this, saveChangesText);
			if (r == JOptionPane.CANCEL_OPTION)
				return;
			else if (r == JOptionPane.YES_OPTION)
				doCommit();
		}
		JTabbedPane tabbedPane = CAPEdit.instance().getAlgorithmsTab();
		tabbedPane.remove(this);
	}

	protected JPanel getPropertiesPanel() 
	{
		if (propertiesPanel == null) {
			propertiesPanel = new PropertiesEditPanel(new Properties());
			propertiesPanel.setOwnerFrame(CAPEdit.instance().getFrame());
		}
		return propertiesPanel;
	}

	protected JPanel getParameters() {
		if (Parameters == null) {
			Parameters = new JPanel();
			Parameters.setLayout(new BorderLayout());
			Parameters.setSize(new java.awt.Dimension(60, 87));
			Parameters.setBorder(javax.swing.BorderFactory.createTitledBorder(
					javax.swing.BorderFactory.createLineBorder(
							java.awt.Color.gray, 2), parametersText,
					javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
					javax.swing.border.TitledBorder.DEFAULT_POSITION,
					new java.awt.Font(dialogText, java.awt.Font.BOLD, 12),
					new java.awt.Color(51, 51, 51)));
			Parameters.add(getParametersButtonPanel(), BorderLayout.EAST);
			Parameters.add(getTableScroll(), BorderLayout.CENTER);
		}
		return Parameters;
	}

	private JPanel getParametersButtonPanel() {
		if (parametersButtonPanel == null) {
			GridBagConstraints gridBagConstraints13 = new GridBagConstraints();
			gridBagConstraints13.gridx = 0;
			gridBagConstraints13.insets = new java.awt.Insets(0, 6, 6, 0);
			gridBagConstraints13.gridy = 1;
			gridBagConstraints13.weighty = 0;
			gridBagConstraints13.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints13.weightx = 1;
			GridBagConstraints gridBagConstraints12 = new GridBagConstraints();
			gridBagConstraints12.gridx = 0;
			gridBagConstraints12.insets = new java.awt.Insets(0, 6, 6, 0);
			gridBagConstraints12.gridy = 0;
			gridBagConstraints12.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints12.weightx = 1;
			GridBagConstraints gridBagConstraints14 = new GridBagConstraints();
			gridBagConstraints14.gridx = 0;
			gridBagConstraints14.insets = new java.awt.Insets(0, 6, 0, 0);
			gridBagConstraints14.gridy = 4;
			gridBagConstraints14.weighty = 1;
			gridBagConstraints14.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints14.anchor = GridBagConstraints.NORTH;
			parametersButtonPanel = new JPanel();
			parametersButtonPanel.setLayout(new GridBagLayout());
			parametersButtonPanel.add(getDeleteButton(), gridBagConstraints14);
			parametersButtonPanel.add(getAddButton(), gridBagConstraints12);
			parametersButtonPanel.add(getEditButton(), gridBagConstraints13);
		}
		return parametersButtonPanel;
	}

	private JButton getDeleteButton() {
		if (deleteParamButton == null) {
			deleteParamButton = new JButton();
			deleteParamButton.setText(CAPEdit.instance().genericDescriptions.getString("delete"));
			deleteParamButton.addActionListener(
				new java.awt.event.ActionListener()
				{
					public void actionPerformed(ActionEvent e) {
						deleteParamButtonPressed();
					}
				});
		}
		return deleteParamButton;
	}

	/**
	 * This method initializes jButton
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getAddButton() {
		if (addParamButton == null) 
		{
			addParamButton = new JButton();
			addParamButton.setText(CAPEdit.instance().genericDescriptions.getString("add"));
			addParamButton.addActionListener(
				new java.awt.event.ActionListener()
				{
					public void actionPerformed(ActionEvent e) {
						addParamButtonPressed();
					}
				});
		}
		return addParamButton;
	}

	/**
	 * This method initializes jButton1
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getEditButton() 
	{
		if (editParamButton == null) 
		{
			editParamButton = new JButton();
			editParamButton.setText(CAPEdit.instance().genericDescriptions.getString("edit"));
			editParamButton.addActionListener(
				new java.awt.event.ActionListener()
				{
					public void actionPerformed(ActionEvent e) 
					{
						editParamButtonPressed();
					}
				});
		}
		return editParamButton;
	}
	
	private void changeNameButtonPressed()
	{
	    String newName = JOptionPane.showInputDialog(
			chngNameInputText);
		if (newName == null)
			return; // cancel pressed.
		newName = newName.trim();
		if (newName.equals(editedObject.getName()))
			return;
		if (newName.length() == 0)
		{
			showError(chngNameErr1);
			return;
		}
		AlgorithmsListPanel lp = CAPEdit.instance().algorithmsListPanel;
		if (lp.algoListTableModel.existsInList(newName))
		{
			CAPEdit.instance().getFrame().showError(chngNameErr2);
			return;
		}
		nameText.setText(newName);
		JTabbedPane tab = CAPEdit.instance().algorithmsTab;
		int idx = tab.indexOfComponent(this);
		if (idx != -1)
			tab.setTitleAt(idx, newName);
	}

	private void addParamButtonPressed()
	{
		DbAlgoParm dap = new DbAlgoParm("", "");
		AlgoParmDialog dlg = new AlgoParmDialog(nameText.getText(), dap);
		CAPEdit.instance().getFrame().launchDialog(dlg);
		if (dlg.okPressed)
		{
			if (algoParmTableModel.getByName(dap.getRoleName(), -1) != null)
			{
				showError(addParmErr1);
				return;
			}
			algoParmTableModel.add(dap);
		}
	}

	private void editParamButtonPressed()
	{
		int r = algoParmTable.getSelectedRow();
		if (r == -1)
		{
			showError("Select row, then press Edit.");
			return;
		}
		DbAlgoParm dap = (DbAlgoParm)algoParmTableModel.getRowObject(r);
		DbAlgoParm dapcopy = new DbAlgoParm(dap);

		AlgoParmDialog dlg = new AlgoParmDialog(nameText.getText(), dapcopy);
		CAPEdit.instance().getFrame().launchDialog(dlg);
		if (dlg.okPressed)
		{
			if (algoParmTableModel.getByName(dapcopy.getRoleName(), r) != null)
			{
				showError("Parameter '" + dapcopy.getRoleName() + "' not found.");
				return;
			}
			dap.setRoleName(dapcopy.getRoleName());
			dap.setParmType(dapcopy.getParmType());
			algoParmTableModel.fireTableDataChanged();
		}
	}

	private void deleteParamButtonPressed()
	{
		int r = algoParmTable.getSelectedRow();
		if (r == -1)
		{
			showError(deleteParamErr1);
			return;
		}
		DbAlgoParm dap = (DbAlgoParm)algoParmTableModel.getRowObject(r);
		int ok = JOptionPane.showConfirmDialog(this,
			deleteParamPrompt1 + 
			dap.getRoleName()
			+ "'?");
		if (ok == JOptionPane.YES_OPTION)
		{
			algoParmTableModel.deleteAt(r);
		}
	}

	@Override
	public PropertySpec[] getSupportedProps()
	{
		return staticPropSpecs;
	}

	@Override
	public boolean additionalPropsAllowed()
	{
		return true;
	}

	@Override
	public boolean dynamicPropsAllowed()
	{
		return execClassField.getText().toLowerCase().contains("python");
	}

	@Override
	public Collection<PropertySpec> getDynamicPropSpecs()
	{
		return dynamicPropSpecs.values();
	}

	@Override
	public void setDynamicPropDescription(String propName, String description)
	{
		PropertySpec propSpec = dynamicPropSpecs.get(propName.toUpperCase());

//System.out.println("AlgorithmsEditPanel.setDynPropDesc: " + propName 
//+ " '" + description + "' propSpec is " + (propSpec==null?"new.":"existing."));
		if (propSpec != null)
		{
			if (description == null)
			{
				dynamicPropSpecs.remove(propName.toUpperCase());
				return;
			}
			else
				propSpec.setDescription(description);
		}
		else if (description != null)// this is a new property
		{
			PropertySpec ps = new PropertySpec(propName, PropertySpec.STRING, description);
			ps.setDynamic(true);
			String pnuc = propName.toUpperCase();
			dynamicPropSpecs.put(pnuc, ps);
//System.out.println("After put, there are " + dynamicPropSpecs.values().size() + " dynamic props.");
//for(String key : dynamicPropSpecs.keySet())
//	System.out.println("\t'" + key + "'");
		}
	}

	@Override
	public String getDynamicPropDescription(String propName)
	{
		return null;
	}
}

@SuppressWarnings("serial")
class AlgoParmTableModel extends AbstractTableModel implements
		SortingListTableModel 
{
	Vector<DbAlgoParm> myvector = new Vector<DbAlgoParm>();

	static String columnNames[] = { 
		CAPEdit.instance().compeditDescriptions.getString("AlgorithmsEditPanel.RoleName"),
		CAPEdit.instance().compeditDescriptions.getString("AlgorithmsEditPanel.TypeCode")
		};
	static int columnWidths[] = { 50, 50 };

	@SuppressWarnings("unchecked")
	public void sortByColumn(int c) {
		Collections.sort(myvector, new AlgorithmsEditComparator(c));
		fireTableDataChanged();
	}

	public Object getRowObject(int arg0) {
		return myvector.get(arg0);
	}

	public void deleteAt(int r)
	{
		myvector.remove(r);
		fireTableDataChanged();
	}

	public void add(DbAlgoParm dap)
	{
		myvector.add(dap);
		fireTableDataChanged();
	}

	public DbAlgoParm getByName(String roleName, int otherThanIdx)
	{
		for(int i=0; i<myvector.size(); i++)
		{
			if (i == otherThanIdx)
				continue;
			DbAlgoParm dap = myvector.get(i);
			if (dap.getRoleName().equalsIgnoreCase(roleName))
				return dap;
		}
		return null;
	}

	public int getRowCount() 
	{
		return myvector.size();
	}

	public int getColumnCount() 
	{
		return columnNames.length;
	}

	public void fill(DbCompAlgorithm dca) 
	{
		for(Iterator<DbAlgoParm> pit = dca.getParms(); pit.hasNext(); )
		{
			DbAlgoParm dap = pit.next();
			myvector.add(new DbAlgoParm(dap));
		}
		fireTableDataChanged();
	}

	public void saveTo(DbCompAlgorithm dca)
	{
		dca.clearParms();
		for(DbAlgoParm parm : myvector)
			dca.addParm(parm);
	}

	public String getColumnName(int col) 
	{
		return columnNames[col];

	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		if (myvector.get(rowIndex) != null)
			return getNlColumn(myvector.get(rowIndex), columnIndex);
		else
			return "";
	}

	public static String getNlColumn(DbAlgoParm obj, int columnIndex) {
		switch (columnIndex) {
		case 0:
			return obj.getRoleName();
		case 1:
			int pti = RoleTypes.getIndex(obj.getParmType());
			return pti >= 0 ? RoleTypes.getRoleType(pti) : obj.getParmType();
		default:
			return "";
		}
	}

	private static String getFirstLine(String tmp) {
		int length = tmp.length();
		int cut = tmp.indexOf("\n");
		if (cut < 60 && cut != -1) {
			return tmp.substring(0, cut - 1);
		} else if (length > 60) {
			return tmp.substring(0, 60);
		} else {
			return tmp;
		}
	}
}

class AlgorithmsEditComparator implements Comparator {
	int column;

	public AlgorithmsEditComparator(int column) {
		this.column = column;
	}

	/**
	 * Compare the eqMod names of the specified type.
	 */
	public int compare(Object ob1, Object ob2) {
		if (ob1 == ob2)
			return 0;
		DbAlgoParm ds1 = (DbAlgoParm) ob1;
		DbAlgoParm ds2 = (DbAlgoParm) ob2;

		String s1 = AlgoParmTableModel.getNlColumn(ds1, column);
		String s2 = AlgoParmTableModel.getNlColumn(ds2, column);

		return s1.compareToIgnoreCase(s2);
	}

	public boolean equals(Object ob) {
		return false;
	}
}
