/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:04  cvs
*  Added legacy code to repository
*
*  Revision 1.6  2008/02/10 20:17:33  mmaloney
*  dev
*
*  Revision 1.2  2008/02/01 15:20:40  cvs
*  modified files for internationalization
*
*  Revision 1.5  2007/12/04 18:26:55  mmaloney
*  dev
*
*  Revision 1.4  2005/03/15 16:11:28  mjmaloney
*  Modify 'Enum' for Java 5 compat.
*
*  Revision 1.3  2004/12/21 14:46:06  mjmaloney
*  Added javadocs
*
*  Revision 1.2  2004/04/01 22:37:23  mjmaloney
*  Implemented controls for enumerations.
*
*/
package decodes.rledit;

import hec.util.TextUtil;
import ilex.util.Logger;

import javax.swing.*;
import java.awt.*;
import javax.swing.border.*;
import java.awt.event.*;
import java.util.ResourceBundle;

import decodes.db.*;

/**
This dialog edits a single enum value.
*/
public class EnumValueDialog extends JDialog 
{
	private static ResourceBundle genericLabels = 
		RefListEditor.getGenericLabels();
	private static ResourceBundle labels = RefListEditor.getLabels();
	private Border border1;
	private JPanel jPanel1 = new JPanel();
	private BorderLayout borderLayout1 = new BorderLayout();
	private JPanel jPanel2 = new JPanel();
	private JLabel jLabel1 = new JLabel();
	private JTextField enumNameField = new JTextField();
	private FlowLayout flowLayout1 = new FlowLayout();
	private Border border2;
	private JPanel labelValuePanel = new JPanel();
	private JPanel southButtonPanel = new JPanel();
	private FlowLayout flowLayout2 = new FlowLayout();
	private JButton okButton = new JButton();
	private JButton cancelButton = new JButton();
	private JTextField valueField = new JTextField();
	private JTextField descriptionField = new JTextField();
	private JTextField execClassField = new JTextField();
	private JTextField optionsField = new JTextField();

	//==============================================================
	private boolean _wasChanged = false;
	private EnumValue myEV;

	/**
	 * Constructor.
	 */
	public EnumValueDialog() throws HeadlessException 
	{
		try {
			jbInit();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		myEV = null;

		addWindowListener(
			new WindowAdapter()
			{
				boolean started=false;
				public void windowActivated(WindowEvent e)
				{
					if (!started)
						valueField.requestFocus();
					started = true;
				}
			});
	}

	/**
	 * Fills the dialog with the specified EnumValue attributes.
	 * @param ev the EnumValue
	 */
	public void fillValues(EnumValue ev)
	{
		myEV = ev;
		enumNameField.setText(ev.dbenum.enumName);
		valueField.setText(ev.value != null ? ev.value : "");
		descriptionField.setText(ev.description != null ? ev.description : "");
		execClassField.setText(ev.execClassName != null ? ev.execClassName:"");
		optionsField.setText(ev.editClassName != null ? ev.editClassName : "");
		_wasChanged = false;
	}

	/** Initializes GUI components. */
	private void jbInit() throws Exception 
	{
		border1 = BorderFactory.createBevelBorder(BevelBorder.LOWERED,Color.white,Color.white,new Color(115, 114, 105),new Color(165, 163, 151));
		border2 = BorderFactory.createLineBorder(SystemColor.controlText,1);
		this.setModal(true);
		this.setTitle(labels.getString("EnumValueDialog.title"));
		jPanel1.setLayout(borderLayout1);
		this.setSize(new Dimension(500, 220));
		//jPanel1.setPreferredSize(new Dimension(500, 150));
		jLabel1.setText(labels.getString("EnumValueDialog.enumerationType"));
		enumNameField.setMinimumSize(new Dimension(130, 20));
		enumNameField.setPreferredSize(new Dimension(130, 20));
		enumNameField.setEditable(false);
		enumNameField.setText("enumNameHere");
		jPanel2.setLayout(flowLayout1);
		flowLayout1.setVgap(10);
		labelValuePanel.setLayout(new GridBagLayout());
		southButtonPanel.setLayout(flowLayout2);
		okButton.setPreferredSize(new Dimension(100, 23));
		okButton.setText(genericLabels.getString("OK"));
		okButton.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e) 
				{
					okButtonPressed();
				}
			});

		cancelButton.setPreferredSize(new Dimension(100, 23));
		cancelButton.setText(genericLabels.getString("cancel"));
		cancelButton.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e) 
				{
					cancelButtonPressed();
				}
			});

		flowLayout2.setHgap(15);
		flowLayout2.setVgap(10);
		valueField.setToolTipText(labels.getString(
				"EnumValueDialog.enumerationTT"));
		
		valueField.setMinimumSize(new Dimension(120, 20));
		valueField.setPreferredSize(new Dimension(120, 20));
		valueField.setText("");
		descriptionField.setMinimumSize(new Dimension(200, 20));
		descriptionField.setPreferredSize(new Dimension(240, 20));
		descriptionField.setText("");
		execClassField.setPreferredSize(new Dimension(240, 20));
		execClassField.setText("");
		southButtonPanel.add(okButton, null);
		southButtonPanel.add(cancelButton, null);
		this.getContentPane().add(jPanel1, BorderLayout.CENTER);
		jPanel1.add(jPanel2, BorderLayout.NORTH);
		jPanel2.add(jLabel1, null);
		jPanel2.add(enumNameField, null);
		jPanel1.add(labelValuePanel, BorderLayout.CENTER);
		jPanel1.add(southButtonPanel, BorderLayout.SOUTH);
		labelValuePanel.add(new JLabel(labels.getString("EnumValueDialog.mnemonicValue")),
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(10, 20, 5, 0), 0, 0));
		labelValuePanel.add(valueField,
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(10, 2, 5, 100), 0, 0));
		labelValuePanel.add(new JLabel(labels.getString("EnumValueDialog.completeDescription")),
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(5, 20, 5, 0), 0, 0));
		labelValuePanel.add(descriptionField,
			new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(5, 2, 5, 20), 0, 0));
		labelValuePanel.add(new JLabel(labels.getString("EnumValueDialog.executableJavaClass")),
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 20, 5, 0), 0, 0));
		labelValuePanel.add(execClassField,
			new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(5, 2, 5, 20), 0, 0));
		labelValuePanel.add(new JLabel("Options:"),
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 20, 10, 0), 0, 0));
		labelValuePanel.add(optionsField,
			new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(5, 2, 10, 20), 0, 0));
	}

	/**
	 * Called when OK button is pressed.
	 * @param e ignored.
	 */
	void okButtonPressed() 
	{
		String v = valueField.getText();
		if (!v.equals(myEV.value))
		{
			_wasChanged = true;
			myEV.value = v;
		}	
		v = descriptionField.getText();
		if (!v.equals(myEV.description))
		{
			_wasChanged = true;
			myEV.description = v;
		}	
		v = execClassField.getText();
		if (!v.equals(myEV.execClassName))
		{
			_wasChanged = true;
			myEV.execClassName = v;
		}
		v = optionsField.getText();
		if (!TextUtil.equals(v, myEV.editClassName))
		{
			_wasChanged = true;
			myEV.editClassName = v;
		}
		
		closeDlg();
	}

	/** @return true if values in the dialog were changed. */
	public boolean wasChanged() { return _wasChanged; }

	/**
	 * Called when cancel button is pressed.
	 * @param e ignored.
	 */
	void cancelButtonPressed()
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

}
