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
*	Revision 1.5  2004/12/21 14:46:05  mjmaloney
*	Added javadocs
*	
*	Revision 1.4  2004/04/20 20:08:18  mjmaloney
*	Working reference list editor, required several mods to SQL code.
*	
*	Revision 1.3  2004/04/12 17:53:12  mjmaloney
*	Implemented edit functions for Unit Converters.
*	
*	Revision 1.2  2004/04/09 18:59:42  mjmaloney
*	dev.
*	
*	Revision 1.1  2004/02/03 15:19:53  mjmaloney
*	Working GUI prototype complete.
*	
*/
package decodes.rledit;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.*;
import java.util.*;

import decodes.db.*;
import ilex.util.*;

/**
This dialog is used for adding or editing an EU conversion.
*/
public class EUCnvEditDialog extends JDialog
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
	private JLabel jLabel2 = new JLabel();
	private JLabel jLabel3 = new JLabel();
	private JLabel jLabel4 = new JLabel();
	private JTextField equationField = new JTextField();
	private JLabel jLabel5 = new JLabel();
	private JComboBox fromComboBox = new JComboBox();
	private JComboBox toComboBox = new JComboBox();
	private JComboBox algorithmComboBox = new JComboBox(
		new String[] { "none", "linear", "usgs", "poly-5"});
	private JLabel jLabel6 = new JLabel();
	private JLabel jLabel7 = new JLabel();
	private JLabel jLabel8 = new JLabel();
	private JLabel jLabel9 = new JLabel();
	private JLabel jLabel10 = new JLabel();
	private JLabel jLabel11 = new JLabel();
	private JTextField coeffField[];
	private Border border1;
	private JPanel jPanel3 = new JPanel();
	private JButton okButton = new JButton();
	private JButton cancelButton = new JButton();
	private FlowLayout flowLayout2 = new FlowLayout();
	private GridBagLayout gridBagLayout1 = new GridBagLayout();

	private boolean _wasChanged;
	private UnitConverterDb myUC;
	private RefListFrame parent;

	/**
	 * Constructor.
	 * @param frame the owner
	 * @param title the dialog title
	 * @param modal true if this dialog is modal.
	 */
	public EUCnvEditDialog(Frame frame, String title, boolean modal)
	{
		super(frame, title, modal);
		_wasChanged = false;
		coeffField = new JTextField[6];
		coeffField[0] = new JTextField();
		coeffField[1] = new JTextField();
		coeffField[2] = new JTextField();
		coeffField[3] = new JTextField();
		coeffField[4] = new JTextField();
		coeffField[5] = new JTextField();
		try {
			jbInit();
			pack();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}

		myUC = null;
		Database db = Database.getDb();
		Vector eus = new Vector();
		for(Iterator it = db.engineeringUnitList.iterator(); it.hasNext(); )
		{
			EngineeringUnit eu = (EngineeringUnit)it.next();
			eus.add(eu.abbr);
		}
		Collections.sort(eus);
		fromComboBox.addItem("");
		toComboBox.addItem("");
		for(Iterator it = eus.iterator(); it.hasNext(); )
		{
			String s = (String)it.next();
			fromComboBox.addItem(s);
			toComboBox.addItem(s);
		}

		addWindowListener(
			new WindowAdapter()
			{
				boolean started=false;
				public void windowActivated(WindowEvent e)
				{
					if (!started)
						fromComboBox.requestFocus();
					started = true;
				}
			});
	}

	/**
	 * Fill dialog with values from the passed converter.
	 * @param parent owner frame
	 * @param uc the UnitConverterDb object to display
	 */
	public void fillValues(RefListFrame parent, UnitConverterDb uc)
	{
		this.parent = parent;
		myUC = uc;
		fromComboBox.setSelectedItem(uc.fromAbbr);
		toComboBox.setSelectedItem(uc.toAbbr);
		algorithmComboBox.setSelectedItem(uc.algorithm);
		for(int i=0; i<6; i++)
			coeffField[i].setText(uc.getCoeffString(i));
	}

	/**
	 * Constructor.
	 * @param parent owner frame
	 */
	public EUCnvEditDialog(Frame parent)
	{
		this(parent, "", false);
	}

	/**
	 * No args constructor for JBuilder.
	 */
	public EUCnvEditDialog() 
	{
		this(null, "", false);
	}

	/** Initializes GUI components. */
	private void jbInit() throws Exception {
		border1 = BorderFactory.createEtchedBorder(Color.white,new Color(165, 163, 151));
		panel1.setLayout(borderLayout1);
		this.setModal(true);
		this.setTitle(labels.getString("EUCnvEditDialog.title"));
		jPanel1.setLayout(flowLayout1);
		jLabel1.setFont(new java.awt.Font("Dialog", 1, 11));
		jLabel1.setText(labels.getString("EUCnvEditDialog.panelTitle"));
		jPanel2.setLayout(gridBagLayout1);
		jLabel2.setText(labels.getString("EUCnvEditDialog.convertFrom"));
		jLabel3.setText(labels.getString("EUCnvEditDialog.convertTo"));
		jLabel4.setText(labels.getString("EUCnvEditDialog.algorithm"));
		equationField.setEditable(false);
		equationField.setText("y = x");
		jLabel5.setText(labels.getString("EUCnvEditDialog.equation"));
		fromComboBox.setMinimumSize(new Dimension(120, 19));
		fromComboBox.setPreferredSize(new Dimension(120, 19));
		jLabel6.setText("A:");
		jLabel7.setText("B:");
		jLabel8.setText("C:");
		jLabel9.setText("D:");
		jLabel10.setText("E:");
		jLabel11.setText("F:");
		coeffField[0].setText("1.0");
		coeffField[1].setText("0.0");
		coeffField[2].setText("0.0");
		coeffField[3].setText("0.0");
		coeffField[4].setText("0.0");
		coeffField[5].setText("0.0");
		jPanel1.setBorder(border1);
		okButton.setText(genericLabels.getString("OK"));
    okButton.addActionListener(new EUCnvEditDialog_okButton_actionAdapter(this));
		cancelButton.setText(genericLabels.getString("cancel"));
    cancelButton.addActionListener(new EUCnvEditDialog_cancelButton_actionAdapter(this));
		jPanel3.setLayout(flowLayout2);
		flowLayout2.setHgap(25);
		jPanel3.setPreferredSize(new Dimension(187, 45));
		algorithmComboBox.addActionListener(new EUCnvEditDialog_algorithmComboBox_actionAdapter(this));
    getContentPane().add(panel1);
		panel1.add(jPanel1, BorderLayout.NORTH);
		jPanel1.add(jLabel1, null);
		panel1.add(jPanel2, BorderLayout.CENTER);
		jPanel2.add(jLabel3,	new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(13, 50, 0, 0), 0, 0));
		jPanel2.add(jLabel2,	new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(21, 38, 0, 0), 0, 0));
		jPanel2.add(jLabel4,	new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(13, 58, 0, 0), 0, 0));
		jPanel2.add(jLabel5,	new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(14, 62, 0, 0), 0, 0));
		jPanel2.add(jLabel6,	new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(13, 95, 0, 0), 0, 0));
		jPanel2.add(jLabel7,	new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(13, 96, 0, 0), 0, 0));
		jPanel2.add(jLabel8,	new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(13, 96, 0, 0), 0, 0));
		jPanel2.add(jLabel9,	new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(14, 96, 0, 0), 0, 0));
		jPanel2.add(jLabel10,	new GridBagConstraints(0, 8, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(13, 97, 0, 0), 0, 0));
		jPanel2.add(jLabel11,	new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(13, 97, 39, 0), 0, 0));
		jPanel2.add(fromComboBox,	new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(21, 6, 0, 158), 10, 0));
		jPanel2.add(toComboBox,	new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(11, 6, 0, 158), 106, 0));
		jPanel2.add(algorithmComboBox,	new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(11, 6, 0, 158), 106, 0));
		jPanel2.add(equationField,	new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 6, 0, 43), 183, 0));
		jPanel2.add(coeffField[0],	new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 6, 0, 186), 76, 0));
		jPanel2.add(coeffField[1],	new GridBagConstraints(1, 5, 1, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 6, 0, 186), 76, 0));
		jPanel2.add(coeffField[2],	new GridBagConstraints(1, 6, 1, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 6, 0, 186), 76, 0));
		jPanel2.add(coeffField[3],	new GridBagConstraints(1, 7, 1, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 6, 0, 186), 76, 0));
		jPanel2.add(coeffField[4],	new GridBagConstraints(1, 8, 1, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 6, 0, 186), 76, 0));
		jPanel2.add(coeffField[5],	new GridBagConstraints(1, 9, 1, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 6, 39, 186), 76, 0));
		this.getContentPane().add(jPanel3, BorderLayout.SOUTH);
		jPanel3.add(okButton, null);
		jPanel3.add(cancelButton, null);
	}

	/**
	 * Called when algorithm combo box selection is made. This will control
	 * how many coefficients are allowed.
	 * @param e ignored
	 */
	void algorithmComboBox_actionPerformed(ActionEvent e) 
	{
		String s = (String)algorithmComboBox.getSelectedItem();
		if (s.equals("none"))
		{
			coeffField[0].setEnabled(false);
			coeffField[1].setEnabled(false);
			coeffField[2].setEnabled(false);
			coeffField[3].setEnabled(false);
			coeffField[4].setEnabled(false);
			coeffField[5].setEnabled(false);
			equationField.setText("y = x");
		}
		else if (s.equals("linear"))
		{
			coeffField[0].setEnabled(true);
			coeffField[1].setEnabled(true);
			coeffField[2].setEnabled(false);
			coeffField[3].setEnabled(false);
			coeffField[4].setEnabled(false);
			coeffField[5].setEnabled(false);
			equationField.setText("y = Ax + B");
		}
		else if (s.equals("usgs"))
		{
			coeffField[0].setEnabled(true);
			coeffField[1].setEnabled(true);
			coeffField[2].setEnabled(true);
			coeffField[3].setEnabled(true);
			coeffField[4].setEnabled(false);
			coeffField[5].setEnabled(false);
			equationField.setText("y = A * (B + x)^C + D");
		}
		else if (s.equals("poly-5"))
		{
			coeffField[0].setEnabled(true);
			coeffField[1].setEnabled(true);
			coeffField[2].setEnabled(true);
			coeffField[3].setEnabled(true);
			coeffField[4].setEnabled(true);
			coeffField[5].setEnabled(true);
			equationField.setText("y = Ax^5 + Bx^4 + Cx^3 + Dx^2 + Ex + F");
		}
	}

	/**
	 * Called when OK button is pressed.
	 * @param e ignored.
	 */
	void okButton_actionPerformed(ActionEvent e) 
	{
		String newFrom = (String)fromComboBox.getSelectedItem();
		if (newFrom.length() == 0)
		{
			showError(labels.getString("EUCnvEditDialog.convertFromReqErr"));
			return;
		}
		String newTo = (String)toComboBox.getSelectedItem();
		if (newTo.length() == 0)
		{
			showError(labels.getString("EUCnvEditDialog.convertToReqErr"));
			return;
		}

		// If user changed from or to, make sure there is no clash!
		if (!myUC.fromAbbr.equals(newFrom) || !myUC.toAbbr.equals(newTo))
		{
			UnitConverterDb uc = Database.getDb().unitConverterSet.getDb(
				newFrom, newTo);
			if (uc != null)
			{
				showError(LoadResourceBundle.sprintf(labels.getString(						
						"EUCnvEditDialog.convertionModErr"),
						newFrom, newTo));
				return;
			}
		}


		if (!myUC.fromAbbr.equals(newFrom))
		{
			_wasChanged = true;
			myUC.fromAbbr = newFrom;
		}
		if (!myUC.toAbbr.equals(newTo))
		{
			_wasChanged = true;
			myUC.toAbbr = newTo;
		}

		String v = (String)algorithmComboBox.getSelectedItem();
		if (!myUC.algorithm.equals(v))
		{
			_wasChanged = true;
			myUC.algorithm = v;
		}

		for(int i=0; i<6; i++)
		{
			v = coeffField[i].getText().trim();
			if (!myUC.getCoeffString(i).equals(v))
			{
				_wasChanged = true;
				if (v.length() == 0)
					myUC.coefficients[i] = Constants.undefinedDouble;
				else
				{
					try { myUC.coefficients[i] = Double.parseDouble(v); }
					catch(NumberFormatException ex)
					{
						myUC.coefficients[i] = Constants.undefinedDouble;
						showError(labels.getString("EUCnvEditDialog.coefficient") + 
							(char)((byte)'A' + i) + labels.getString(
									"EUCnvEditDialog.coeffErr"));
						return;
					}
				}
			}
		}

		closeDlg();
	}

	/**
	 * Called when cancel button is pressed.
	 * @param e ignored.
	 */
	void cancelButton_actionPerformed(ActionEvent e) 
	{
		_wasChanged = false;
		closeDlg();
	}

	/** Closes the dialog. */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/** @return true if values were changed in the dialog. */
	public boolean wasChanged() { return _wasChanged; }

	private void showError(String msg)
	{
		System.err.println(msg);
		JOptionPane.showMessageDialog(this,
			AsciiUtil.wrapString(msg, 60), "Error!", JOptionPane.ERROR_MESSAGE);
	}

}

class EUCnvEditDialog_algorithmComboBox_actionAdapter implements java.awt.event.ActionListener {
  EUCnvEditDialog adaptee;

  EUCnvEditDialog_algorithmComboBox_actionAdapter(EUCnvEditDialog adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.algorithmComboBox_actionPerformed(e);
  }
}

class EUCnvEditDialog_okButton_actionAdapter implements java.awt.event.ActionListener {
  EUCnvEditDialog adaptee;

  EUCnvEditDialog_okButton_actionAdapter(EUCnvEditDialog adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.okButton_actionPerformed(e);
  }
}

class EUCnvEditDialog_cancelButton_actionAdapter implements java.awt.event.ActionListener {
  EUCnvEditDialog adaptee;

  EUCnvEditDialog_cancelButton_actionAdapter(EUCnvEditDialog adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.cancelButton_actionPerformed(e);
  }
}
