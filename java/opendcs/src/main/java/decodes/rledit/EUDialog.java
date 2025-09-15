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
package decodes.rledit;

import java.awt.*;
import javax.swing.*;

import org.opendcs.gui.GuiHelpers;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.awt.event.*;
import java.util.*;

import decodes.db.*;

/**
This dialog is for editing and adding a new Engineering Unit definition.
*/
public class EUDialog extends JDialog
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private static ResourceBundle genericLabels = RefListEditor.getGenericLabels();
	private static ResourceBundle labels = RefListEditor.getLabels();
	private JPanel panel1 = new JPanel();
	private BorderLayout borderLayout1 = new BorderLayout();
	private JPanel jPanel1 = new JPanel();
	private JLabel jLabel1 = new JLabel();
	private FlowLayout flowLayout1 = new FlowLayout();
	private JPanel jPanel2 = new JPanel();
	private JButton okButton = new JButton();
	private FlowLayout flowLayout2 = new FlowLayout();
	private JButton cancelButton = new JButton();
	private JPanel jPanel3 = new JPanel();
	private JLabel jLabel2 = new JLabel();
	private JLabel jLabel3 = new JLabel();
	private JLabel jLabel4 = new JLabel();
	private JLabel jLabel5 = new JLabel();
	private JTextField abbrField = new JTextField();
	private JTextField nameField = new JTextField();
	private JLabel jLabel6 = new JLabel();
	private JComboBox<String> familyCombo = new JComboBox<>(
		new String[] { "English", "Metric", "Univ"});
	private JComboBox<String> measuresCombo = new JComboBox<>();
	private GridBagLayout gridBagLayout1 = new GridBagLayout();

	//===========================================================
	private boolean isOK = false;
	private String initAbbr, initFamily, initName, initMeasures;

	/**
	 * Constructor.
	 * @param frame the owner
	 * @param title the dialog title
	 * @param modal true if modal
	 */
	public EUDialog(Frame frame, String title, boolean modal)
	{
		super(frame, title, modal);
		try
		{
			jbInit();
			pack();
			buildMeasuresCombo();
		}
		catch (Exception ex)
		{
			GuiHelpers.logGuiComponentInit(log, ex);
		}
		isOK = false;
		initAbbr = initFamily = initName = initMeasures = "";
	}

	/**
	 * Fills the dialog according to the values in an EU definition.
	 * @param eu the EngineeringUnit
	 */
	public void fillValues(EngineeringUnit eu)
	{
		abbrField.setText(initAbbr = eu.abbr);
		nameField.setText(initName = eu.getName());
		familyCombo.setSelectedItem(initFamily = eu.family);
		measuresCombo.setSelectedItem(initMeasures = eu.measures);
	}

	/**
	 * Builds a combo box by collecting all the unique values in the
	 * "measures" attribute for existing EUs.
	 */
	void buildMeasuresCombo()
	{
		EngineeringUnitList eul = Database.getDb().engineeringUnitList;
		Vector v = new Vector();
		for(Iterator it = eul.iterator(); it.hasNext(); )
		{
			EngineeringUnit eu = (EngineeringUnit)it.next();
			String m = eu.measures;
			if (!v.contains(m))
				v.add(m);
		}
		Collections.sort(v);
		for(Iterator it = v.iterator(); it.hasNext(); )
		{
			String s = (String)it.next();
			measuresCombo.addItem(s);
		}
	}

	/**
	 * No args constructor for JBuilder.
	 */
	public EUDialog()
	{
		this(null, "", false);
	}

	/**
	 * Constructor.
	 * @param parent the parent.
	 */
	public EUDialog(Frame parent)
	{
		this(parent, "", false);
	}

	/** Initializes GUI components. */
	private void jbInit() throws Exception {
		panel1.setLayout(borderLayout1);
		this.setTitle(labels.getString("EUDialog.frameTitle"));
		jLabel1.setText(labels.getString("EUDialog.title"));
		jPanel1.setLayout(flowLayout1);
		flowLayout1.setHgap(5);
		flowLayout1.setVgap(10);
		okButton.setMinimumSize(new Dimension(9047, 23));
		okButton.setPreferredSize(new Dimension(90, 23));
		okButton.setText(genericLabels.getString("OK"));
		okButton.addActionListener(new EUDialog_okButton_actionAdapter(this));
		jPanel2.setLayout(flowLayout2);
		flowLayout2.setHgap(15);
		flowLayout2.setVgap(10);
		cancelButton.setMinimumSize(new Dimension(90, 23));
		cancelButton.setPreferredSize(new Dimension(90, 23));
		cancelButton.setText(genericLabels.getString("cancel"));
		cancelButton.addActionListener(new EUDialog_cancelButton_actionAdapter(this));
		jPanel3.setLayout(gridBagLayout1);
		jLabel2.setText(labels.getString("EUDialog.abbreviation"));
		jLabel3.setText(labels.getString("EUDialog.fullName")+":");
		jLabel4.setText(labels.getString("EUDialog.family")+":");
		jLabel5.setText(labels.getString("EUDialog.measures")+":");
		abbrField.setText("");
		jLabel6.setText(labels.getString("EUDialog.noEmbeddedSpaces"));
		nameField.setText("");
		measuresCombo.setEditable(true);
		getContentPane().add(panel1);
		panel1.add(jPanel1, BorderLayout.NORTH);
		jPanel1.add(jLabel1, null);
		panel1.add(jPanel2, BorderLayout.SOUTH);
		jPanel2.add(okButton, null);
		jPanel2.add(cancelButton, null);
		panel1.add(jPanel3, BorderLayout.CENTER);
		jPanel3.add(jLabel2,		new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
						,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 30, 5, 2), 0, 0));
		jPanel3.add(jLabel3,	 new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
						,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 15, 5, 2), 0, 0));
		jPanel3.add(jLabel4,	 new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
						,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 15, 5, 2), 0, 0));
		jPanel3.add(jLabel5,	 new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
						,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 15, 10, 2), 0, 0));
		jPanel3.add(abbrField,	 new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 0, 5, 0), 138, 0));
		jPanel3.add(nameField,	 new GridBagConstraints(1, 1, 2, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 5, 20), 0, 0));
		jPanel3.add(measuresCombo,	 new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 10, 0), 0, 0));
		jPanel3.add(familyCombo,	 new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 5, 0), 0, 0));
		jPanel3.add(jLabel6,	 new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 12, 5, 15), 0, 0));
	}

	/**
	 * Called when OK button is pressed.
	 * @param e ignored.
	 */
	void okButton_actionPerformed(ActionEvent e)
	{
		isOK = true;
		closeDlg();
	}

	/**
	 * Called when cancel button is pressed.
	 * @param e ignored.
	 */
	void cancelButton_actionPerformed(ActionEvent e)
	{
		isOK = false;
		closeDlg();
	}

	/** Closes the dialog. */
	private void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/** @return true if values were changed. */
	public boolean wasChanged()
	{
		return isOK
			&& (!getAbbr().equals(initAbbr)
			||  !getEUName().equals(initName)
			||  !getFamily().equals(initFamily)
			||  !getMeasures().equals(initMeasures));
	}

	/** @return the EU abbreviation. */
	public String getAbbr()
	{
		return abbrField.getText().trim();
	}

	/** @return the EU name. */
	public String getEUName()
	{
		return nameField.getText().trim();
	}

	/** @return the EU family (e.g. "standard", "english"). */
	public String getFamily()
	{
		String ret = (String)familyCombo.getSelectedItem();
		return ret.trim();
	}

	/** @return the EU "measures" field (e.g. "length", "volumn"). */
	public String getMeasures()
	{
		String ret = (String)measuresCombo.getSelectedItem();
		return ret.trim();
	}
}

class EUDialog_okButton_actionAdapter implements java.awt.event.ActionListener {
	EUDialog adaptee;

	EUDialog_okButton_actionAdapter(EUDialog adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.okButton_actionPerformed(e);
	}
}

class EUDialog_cancelButton_actionAdapter implements java.awt.event.ActionListener {
	EUDialog adaptee;

	EUDialog_cancelButton_actionAdapter(EUDialog adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.cancelButton_actionPerformed(e);
	}
}
