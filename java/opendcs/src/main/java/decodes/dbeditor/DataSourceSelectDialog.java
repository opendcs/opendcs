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
package decodes.dbeditor;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.ResourceBundle;

import org.opendcs.gui.GuiHelpers;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.DataSource;

/**
Dialog class in which to select a data source.
*/
public class DataSourceSelectDialog extends JDialog 
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	JPanel panel1 = new JPanel();
	BorderLayout borderLayout1 = new BorderLayout();
	JPanel jPanel1 = new JPanel();
	JButton okButton = new JButton();
	JButton cancelButton = new JButton();
	FlowLayout flowLayout1 = new FlowLayout();
	JPanel jPanel2 = new JPanel();
	FlowLayout flowLayout2 = new FlowLayout();
	JLabel jLabel1 = new JLabel();
	DataSourceCombo dataSourceCombo = new DataSourceCombo(false);
	JPanel jPanel3 = new JPanel();
	JLabel jLabel2 = new JLabel();

	boolean _isOK = false;

	/** 
	  Constructs new Data Source Select Dialog. 
	  @param frame the parent frame
	  @param title the Title of this dialog
	  @param modal true if this is a modal dialog
	*/
	public DataSourceSelectDialog(Frame frame, String title, boolean modal) 
	{
		super(frame, title, modal);
		try
		{
			jbInit();
			pack();
		}
		catch(Exception ex) 
		{
			GuiHelpers.logGuiComponentInit(log, ex);
		}
	}

	/** No args constructor for JBuilder */
	public DataSourceSelectDialog() 
	{
		this(null, "", false);
	}

	/** JBuilder-generated method to initialize the GUI components */
	private void jbInit() throws Exception {
		panel1.setLayout(borderLayout1);
		okButton.setText(genericLabels.getString("OK"));
		okButton.addActionListener(new DataSourceSelectDialog_okButton_actionAdapter(this));
		cancelButton.setText(genericLabels.getString("cancel"));
		cancelButton.addActionListener(new DataSourceSelectDialog_cancelButton_actionAdapter(this));
		jPanel1.setLayout(flowLayout1);
		flowLayout1.setHgap(20);
		jPanel2.setLayout(flowLayout2);
		jLabel1.setText(
			dbeditLabels.getString("DataSourceSelectDialog.selectDataSource"));
		dataSourceCombo.setPreferredSize(new Dimension(180, 19));
		jLabel2.setText("  ");
		this.setModal(true);
		this.setTitle(
			dbeditLabels.getString("DataSourceSelectDialog.title"));
		getContentPane().add(panel1);
		panel1.add(jPanel1, BorderLayout.SOUTH);
		jPanel1.add(okButton, null);
		jPanel1.add(cancelButton, null);
		panel1.add(jPanel2, BorderLayout.CENTER);
		jPanel2.add(jLabel1, null);
		jPanel2.add(dataSourceCombo, null);
		panel1.add(jPanel3, BorderLayout.NORTH);
		jPanel3.add(jLabel2, null);
	}

	/** 
	  Called when OK button is pressed. 
	  @param e ignored
	*/
	void okButton_actionPerformed(ActionEvent e) 
	{
		_isOK = true;
		closeDlg();
	}

	/** 
	  Called when Cancel button is pressed. 
	  @param e ignored
	*/
	void cancelButton_actionPerformed(ActionEvent e) 
	{
		_isOK = false;
		closeDlg();
	}

	/** Closes the dialog */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/** @return true if dialog OK button was pressed. */
	public boolean okPressed() { return _isOK; }

	/** @return the selected data source */
	public DataSource getSelection()
	{
		return dataSourceCombo.getSelection();
	}

	/** 
	  Called prior to showing dialog to exclude names already selected. 
	  @param dsName a name to exclude
	*/
	public void exclude(String dsName)
	{
		dataSourceCombo.exclude(dsName);
	}

	/** @return the number of data sources in the combo. */
	public int count()
	{
		return dataSourceCombo.getItemCount();
	}
}

class DataSourceSelectDialog_okButton_actionAdapter implements java.awt.event.ActionListener {
	DataSourceSelectDialog adaptee;

	DataSourceSelectDialog_okButton_actionAdapter(DataSourceSelectDialog adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.okButton_actionPerformed(e);
	}
}

class DataSourceSelectDialog_cancelButton_actionAdapter implements java.awt.event.ActionListener {
	DataSourceSelectDialog adaptee;

	DataSourceSelectDialog_cancelButton_actionAdapter(DataSourceSelectDialog adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.cancelButton_actionPerformed(e);
	}
}
