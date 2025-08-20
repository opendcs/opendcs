/*
*	$Id$
*
*	$Log$
*	Revision 1.1  2008/04/04 18:21:04  cvs
*	Added legacy code to repository
*	
*	Revision 1.6  2008/02/10 20:17:33  mmaloney
*	dev
*	
*	Revision 1.2  2008/02/01 15:20:40  cvs
*	modified files for internationalization
*	
*	Revision 1.5  2006/05/11 18:26:42  mmaloney
*	dev
*	
*	Revision 1.4  2004/12/21 14:46:04  mjmaloney
*	Added javadocs
*	
*	Revision 1.3  2004/04/20 20:08:18  mjmaloney
*	Working reference list editor, required several mods to SQL code.
*	
*	Revision 1.2  2004/04/12 21:30:32  mjmaloney
*	dev
*	
*/
package decodes.rledit;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.ResourceBundle;

import decodes.db.*;
import ilex.util.*;

/**
DTEDialog is a pop-up in which the user edits a single data-type-equivalence
entry. It's used when the user hits the Add or Edit button. on the DTE panel.
*/
public class DTEDialog extends JDialog
{
	private static ResourceBundle genericLabels = 
		RefListEditor.getGenericLabels();
	private static ResourceBundle labels = RefListEditor.getLabels();
	private JPanel panel1 = new JPanel();
	private BorderLayout borderLayout1 = new BorderLayout();
	private JPanel jPanel1 = new JPanel();
	private FlowLayout flowLayout1 = new FlowLayout();
	private JLabel jLabel1 = new JLabel();
	private JPanel jPanel2 = new JPanel();
	private JButton okButton = new JButton();
	private FlowLayout flowLayout2 = new FlowLayout();
	private JButton cancelButton = new JButton();
	private JPanel jPanel3 = new JPanel();
	private JLabel stdLabel[] = new JLabel[5];
	private JTextField dtField[] = new JTextField[5];
	private GridBagLayout gridBagLayout1 = new GridBagLayout();

	private boolean _wasChanged = false;
	private int numstds = 0;
	private DTEquivTableModel myModel = null;
	private String origCodes[];
	private int rowNum = -1;

	/**
	 * Constructor.
	 * @param frame the owner
	 * @param title the dialog title
	 * @param modal true if modal
	 */
	public DTEDialog(Frame frame, String title, boolean modal) 
	{
		super(frame, title, modal);
		stdLabel[0] = new JLabel();
		stdLabel[1] = new JLabel();
		stdLabel[2] = new JLabel();
		stdLabel[3] = new JLabel();
		stdLabel[4] = new JLabel();
		dtField[0] = new JTextField();
		dtField[1] = new JTextField();
		dtField[2] = new JTextField();
		dtField[3] = new JTextField();
		dtField[4] = new JTextField();
		try {
			jbInit();
			pack();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Constructor.
	 * @param parent the owner
	 */
	public DTEDialog(Frame parent) 
	{
		this(parent, "", true);
	}

	/**
	 * No args constructor for JBuilder.
	 */
	public DTEDialog() 
	{
		this(null, "", true);
	}

	/**
	 * Fills the dialog according to the values found in the specified row
	 * of the model.
	 * @param model the model
	 * @param row the selected row
	 */
	public void fillValues(DTEquivTableModel model, int row)
	{
		myModel = model;
		rowNum = row;
		numstds = myModel.getColumnCount();
		if (numstds > stdLabel.length)
			numstds = stdLabel.length;
		int i = 0;
		origCodes = new String[numstds];
		for(; i<numstds; i++)
		{
			stdLabel[i].setText(myModel.getColumnName(i) + ":");
			stdLabel[i].setVisible(true);
			dtField[i].setVisible(true);
			if (row != -1)
				dtField[i].setText(origCodes[i] = (String)myModel.getValueAt(row, i));
			else
				dtField[i].setText(origCodes[i] = "");
		}
		for(; i<stdLabel.length; i++)
		{
			stdLabel[i].setVisible(false);
			dtField[i].setVisible(false);
		}
	}

	private void jbInit() throws Exception 
	{
		panel1.setLayout(borderLayout1);
		this.setModal(true);
		this.setTitle(labels.getString("DTEDialog.dataTypeEquiv"));
		jPanel1.setLayout(flowLayout1);
		flowLayout1.setVgap(10);
		jLabel1.setText(labels.getString("DTEDialog.dataTypesEquiv"));
//		okButton.setMinimumSize(new Dimension(80, 23));
//		okButton.setPreferredSize(new Dimension(80, 23));
		okButton.setText(genericLabels.getString("OK"));
		okButton.addActionListener(new DTEDialog_okButton_actionAdapter(this));
		jPanel2.setLayout(flowLayout2);
		flowLayout2.setHgap(20);
		flowLayout2.setVgap(10);
//		cancelButton.setMinimumSize(new Dimension(80, 23));
//		cancelButton.setPreferredSize(new Dimension(80, 23));
		cancelButton.setText(genericLabels.getString("cancel"));
		cancelButton.addActionListener(new DTEDialog_cancelButton_actionAdapter(this));
		jPanel3.setLayout(gridBagLayout1);
		stdLabel[0].setText(labels.getString("DTEDialog.standardOne"));
		stdLabel[1].setText(labels.getString("DTEDialog.standardTwo"));
		stdLabel[2].setText(labels.getString("DTEDialog.standardThree"));
		stdLabel[3].setText(labels.getString("DTEDialog.standardFour"));
		stdLabel[4].setText(labels.getString("DTEDialog.standardFive"));
		dtField[0].setMinimumSize(new Dimension(100, 20));
		dtField[0].setPreferredSize(new Dimension(100, 20));
		dtField[0].setText("");
		dtField[1].setText("");
		dtField[2].setText("");
		dtField[3].setText("");
		dtField[4].setText("");
		getContentPane().add(panel1);
		panel1.add(jPanel1, BorderLayout.NORTH);
		jPanel1.add(jLabel1, null);
		panel1.add(jPanel2, BorderLayout.SOUTH);
		jPanel2.add(okButton, null);
		jPanel2.add(cancelButton, null);
		panel1.add(jPanel3, BorderLayout.CENTER);
		jPanel3.add(stdLabel[0],		new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
						,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 15, 5, 3), 0, 0));
		jPanel3.add(stdLabel[1],	 new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
						,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 15, 5, 3), 0, 0));
		jPanel3.add(stdLabel[2],	 new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
						,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 15, 5, 3), 0, 0));
		jPanel3.add(stdLabel[3],	 new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
						,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 15, 5, 3), 0, 0));
		jPanel3.add(stdLabel[4],		new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
						,GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, new Insets(5, 15, 5, 3), 0, 0));
		jPanel3.add(dtField[0],	 new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 0, 5, 100), 0, 0));
		jPanel3.add(dtField[1],	 new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 5, 100), 0, 0));
		jPanel3.add(dtField[2],	 new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 5, 100), 0, 0));
		jPanel3.add(dtField[3],	 new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 5, 100), 0, 0));
		jPanel3.add(dtField[4],	 new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0
						,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 10, 100), 0, 0));
	}

	/**
	 * Called when OK button is pressed.
	 * @param e ignored.
	 */
	void okButton_actionPerformed(ActionEvent e) 
	{
		DataType dataTypes[] = new DataType[numstds];
		for(int i=0; i<numstds; i++) dataTypes[i] = null;

		// At least 2 dt's must be defined.
		// ALL DTs in this dialog must be either unchanged or new. 
		// No clashes allowed.
		int numDefined = 0;
		DataTypeSet dts = Database.getDb().dataTypeSet;
		for(int i=0; i<numstds; i++)
		{
			String code = dtField[i].getText().trim();
			if (code.length() > 0)
			{
				numDefined++;
				String std = myModel.getColumnName(i);

				// Make sure this std/code isn't already in another equivalence
				// ring!
				if (myModel.exists(std, code, rowNum))
				{
					showError(LoadResourceBundle.sprintf(
							labels.getString("DTEDialog.dataTypeExistsErr"),
							std,code));
					return;
				}

				// This will create the data type record if necessary.
				dataTypes[i] = DataType.getDataType(std, code);
			}
		}

		/*
		  If user changes a code, might have to de-assert the old equivalence.
		*/
		for(int i=0; i<numstds; i++)
		{
			String std = myModel.getColumnName(i);
			String orig = origCodes[i];
			String now = dtField[i].getText().trim();

			if (orig.length() > 0) // There was a DT in this column before?
			{
				if (!now.equals(orig))
				{
					// It was either modified or blanked.
					// Remove old data type.
					DataType odt = dts.get(std, orig);
					if (odt != null)
					{
						Logger.instance().debug3(
							"De-asserting equivalence of " + odt.toString());
						odt.deAssertEquivalence();
					}
				}
			}
		}

		if (numDefined == 0)
			return; // do nothing.
		else if (numDefined == 1)
		{
			showError(labels.getString("DTEDialog.assertEquvErr"));
			return;
		}

		// Now assert the equivalencies.
		DataType lastDT = null;
		for(DataType dt : dataTypes)
		{
			if (dt != null)
			{
				if (lastDT != null)
				{
					Logger.instance().debug3(
						"Asserting equivalence of " + dt.toString() + " and " 
						+ lastDT.toString());
					lastDT.assertEquivalence(dt);
				}
				lastDT = dt;
			}
		}

		_wasChanged = true;
		closeDlg();
	}

	/**
	 * Called when Cancel button is pressed.
	 * @param e ignored.
	 */
	void cancelButton_actionPerformed(ActionEvent e) 
	{
		_wasChanged = false;
		closeDlg();
	}

	/**
	 * @return true if data in the dialog was changed.
	 */
	public boolean wasChanged() { return _wasChanged; }

	/** Closes the dialog. */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/**
	  Convenience method to display an error in a JOptionPane.
	  @param msg the error message.
	*/
	private void showError(String msg)
	{
		System.err.println(msg);
		JOptionPane.showMessageDialog(this,
			AsciiUtil.wrapString(msg, 60), "Error!", JOptionPane.ERROR_MESSAGE);
	}
}

class DTEDialog_okButton_actionAdapter implements java.awt.event.ActionListener {
	DTEDialog adaptee;

	DTEDialog_okButton_actionAdapter(DTEDialog adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.okButton_actionPerformed(e);
	}
}

class DTEDialog_cancelButton_actionAdapter implements java.awt.event.ActionListener {
	DTEDialog adaptee;

	DTEDialog_cancelButton_actionAdapter(DTEDialog adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.cancelButton_actionPerformed(e);
	}
}
