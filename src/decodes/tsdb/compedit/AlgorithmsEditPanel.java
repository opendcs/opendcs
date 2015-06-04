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

import java.util.Collections;
import java.util.Comparator;
import java.util.Properties;
import java.util.Vector;
import java.util.Iterator;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import opendcs.dai.AlgorithmDAI;

import ilex.util.PropertiesUtil;
import decodes.gui.PropertiesEditPanel;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.tsdb.DbAlgoParm;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbIoException;
import decodes.db.Constants;

public class AlgorithmsEditPanel extends EditPanel 
{
	private JPanel jContentPane = null;
	private JPanel inputPanel = null;
	private JLabel algoNameLabel = null;
	private JLabel execClassLabel = null;
	private JTextField nameText = null;
	private JLabel algoIdLabel = null;
	private JTextField idText = null;
	private JTextField execText = null;
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
	
	private JLabel numCompsLabel = null;

	private JTextField numCompsText = null;
	
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
	private String editParamErr1;
	private String editParamErr2;

	

	public AlgorithmsEditPanel()
	{
		setLayout(new BorderLayout());
		fillLabels();
		this.add(getButtonPanel(), java.awt.BorderLayout.SOUTH);
		this.add(getTopPanel(), java.awt.BorderLayout.CENTER);
		editedObject = null;
		setTopFrame(CAPEdit.instance().getFrame());
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
	
	private JPanel getTopPanel() 
	{
		if (jContentPane == null) 
		{
			jContentPane = new JPanel();
			jContentPane.setLayout(new BoxLayout(getTopPanel(),
					BoxLayout.Y_AXIS));
			jContentPane.add(getInputPanel(), null);
			jContentPane.add(getcommentPanel(), null);
			jContentPane.add(getParameters(), null);
			jContentPane.add(getPropertiesPanel(), null);
		}
		return jContentPane;
	}

	public void setEditedObject(DbCompAlgorithm dca)
	{
		editedObject = dca;

		// fill in controls:
		nameText.setText(editedObject.getName());
		idText.setText("" + editedObject.getId());
		execText.setText(editedObject.getExecClass());
		commentsText.setText(editedObject.getComment());
		numCompsText.setText("" + editedObject.getNumCompsUsing());

		propCopy = new Properties();
		PropertiesUtil.copyProps(propCopy, editedObject.getProperties());
		propertiesPanel.setProperties(propCopy);
		algoParmTableModel.fill(editedObject);
	}

	public DbCompAlgorithm getEditedObject()
	{
		return editedObject;
	}


	private JPanel getInputPanel() 
	{
		if (inputPanel == null) 
		{
			GridBagConstraints algoNameLabConstraints =new GridBagConstraints();
			algoNameLabConstraints.gridx = 0;
			algoNameLabConstraints.insets = new java.awt.Insets(10, 10, 4, 2);
			algoNameLabConstraints.gridy = 0;
			algoNameLabel = new JLabel();
			algoNameLabel.setText(algoNameLabelText);

			GridBagConstraints nameTextConstraints = new GridBagConstraints();
			nameTextConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			nameTextConstraints.gridy = 0;
			nameTextConstraints.weightx = 1.0;
			nameTextConstraints.insets = new java.awt.Insets(10, 0, 4, 0);
			nameTextConstraints.gridx = 1;

			GridBagConstraints changeNameConstraints = 
				new GridBagConstraints(2, 0,  1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
				new Insets(10, 5, 4, 5), 0, 0);
			changeNameButton = new JButton(changeButtonText);
			changeNameButton.addActionListener(
				new java.awt.event.ActionListener()
				{
					public void actionPerformed(ActionEvent e) 
					{
						changeNameButtonPressed();
					}
				});

			GridBagConstraints algoIdLabConstraints = new GridBagConstraints();
			algoIdLabConstraints.gridx = 3;
			algoIdLabConstraints.insets = new java.awt.Insets(10, 10, 4, 5);
			algoIdLabConstraints.gridy = 0;
			algoIdLabel = new JLabel();
			algoIdLabel.setText(algoIDText);

			GridBagConstraints idTextConstraints = new GridBagConstraints();
			idTextConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			idTextConstraints.gridy = 0;
			idTextConstraints.weightx = 0.3;
			idTextConstraints.insets = new java.awt.Insets(10, 0, 4, 5);
			idTextConstraints.gridx = 4;

			GridBagConstraints execClassLabConstraints=new GridBagConstraints();
			execClassLabConstraints.gridx = 0;
			execClassLabConstraints.insets = new java.awt.Insets(4, 10, 10, 2);
			execClassLabConstraints.gridy = 1;
			execClassLabConstraints.anchor= java.awt.GridBagConstraints.EAST;
			execClassLabel = new JLabel();
			execClassLabel.setText(execLabelText);

			GridBagConstraints execTextConstraints =
				new GridBagConstraints(1, 1,  2, 1, 1.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
				new Insets(4, 0, 10, 0), 0, 0);

			GridBagConstraints numCompsLabConstraints = new GridBagConstraints();
			numCompsLabConstraints.insets = new java.awt.Insets(4, 10, 10, 2);
			numCompsLabConstraints.gridx = 3;
			numCompsLabConstraints.gridy = 1;
			numCompsLabel = new JLabel();
			numCompsLabel.setText(numCompsLabelText);

			GridBagConstraints numCompsConstraints = new GridBagConstraints();
			numCompsConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			numCompsConstraints.gridy = 1;
			numCompsConstraints.insets = new java.awt.Insets(4, 0, 10, 5);
			numCompsConstraints.weightx = 0.3;
			numCompsConstraints.gridx = 4;

			inputPanel = new JPanel();
			inputPanel.setLayout(new GridBagLayout());
			inputPanel.add(algoNameLabel, algoNameLabConstraints);
			inputPanel.add(getNameText(), nameTextConstraints);
			inputPanel.add(changeNameButton, changeNameConstraints);
			inputPanel.add(algoIdLabel, algoIdLabConstraints);
			inputPanel.add(getIdText(), idTextConstraints);
			inputPanel.add(execClassLabel, execClassLabConstraints);
			inputPanel.add(getExecText(), execTextConstraints);
			inputPanel.add(numCompsLabel, numCompsLabConstraints);
			inputPanel.add(getNumCompsText(), numCompsConstraints);
		}
		return inputPanel;
	}
	/**
	 * This method initializes jTextField
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getNameText() 
	{
		if (nameText == null) 
		{
			nameText = new JTextField();
			nameText.setEditable(false);
			nameText.setToolTipText(nameToolTip);
		}
		return nameText;
	}

	/**
	 * This method initializes jTextField1
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getIdText() 
	{
		if (idText == null) 
		{
			idText = new JTextField();
			idText.setEditable(false);
			idText.setToolTipText(idToolTip);
		}
		return idText;
	}

	/**
	 * This method initializes jTextField
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getExecText() 
	{
		if (execText == null) 
		{
			execText = new JTextField();
			execText.setToolTipText(execToolTipText);
		}
		return execText;
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
			commentsScroll.setHorizontalScrollBarPolicy(commentsScroll.HORIZONTAL_SCROLLBAR_NEVER);
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
		if (algoParmTableModel == null) {
			algoParmTableModel = new AlgoParmTableModel();
			algoParmTable = 
				new SortingListTable(algoParmTableModel,
					algoParmTableModel.columnWidths);
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
		ob.setExecClass(execText.getText().trim());
		propertiesPanel.saveChanges();
		ob.getProperties().clear();
		PropertiesUtil.copyProps(ob.getProperties(), propCopy);
		algoParmTableModel.saveTo(ob);
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
	
	/**
	 * This method initializes jTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getNumCompsText() {
		if (numCompsText == null) {
			numCompsText = new JTextField();
			numCompsText.setEditable(false);
			setToolTipText(numCompsToolTip);
		}
		return numCompsText;
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
			showError(editParamErr1);
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
				showError(editParamErr2);
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
}

class AlgoParmTableModel extends AbstractTableModel implements
		SortingListTableModel 
{
	Vector<DbAlgoParm> myvector = new Vector<DbAlgoParm>();

	static String columnNames[] = { 
		CAPEdit.instance().compeditDescriptions.getString("AlgorithmsEditPanel.RoleName"),
		CAPEdit.instance().compeditDescriptions.getString("AlgorithmsEditPanel.TypeCode")
		};
	static int columnWidths[] = { 50, 50 };

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
			return obj.getParmType();
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
