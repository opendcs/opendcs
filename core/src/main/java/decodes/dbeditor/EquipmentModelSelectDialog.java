/*
 *  $Id$
 */
package decodes.dbeditor;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.border.*;
import java.util.ResourceBundle;

import decodes.db.EquipmentModel;

/**
 * Dialog to select an equipment model by name.
 */
@SuppressWarnings("serial")
public class EquipmentModelSelectDialog extends JDialog
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	JPanel panel1 = new JPanel();
	JPanel jPanel1 = new JPanel();
	FlowLayout flowLayout1 = new FlowLayout();
	JButton selectButton = new JButton();
	JButton cancelButton = new JButton();

	BorderLayout borderLayout1 = new BorderLayout();
	JPanel jPanel2 = new JPanel();
	BorderLayout borderLayout2 = new BorderLayout();
	TitledBorder titledBorder1;
	Border border1;
	EquipmentModelSelectPanel eqModSelectPanel = new EquipmentModelSelectPanel();
	EquipmentModel eqMod;
	JButton clearButton = new JButton();
	boolean _cancelled;

	/** No args constructor for JBuilder */
	public EquipmentModelSelectDialog()
	{
		super(DecodesDbEditor.getTheFrame(), "", true);
		_cancelled = false;
		eqMod = null;
		try
		{
			jbInit();
			pack();
			getRootPane().setDefaultButton(selectButton);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	/** JBuilder-generated method to initialize the GUI components */
	void jbInit() throws Exception
	{
		titledBorder1 = new TitledBorder(
			BorderFactory.createLineBorder(new Color(153, 153, 153), 2),
			dbeditLabels.getString("EquipmentModelSelectDialog.title"));
		border1 = BorderFactory.createCompoundBorder(titledBorder1,
			BorderFactory.createEmptyBorder(5, 5, 5, 5));
		panel1.setLayout(borderLayout1);
		jPanel1.setLayout(flowLayout1);
		selectButton.setText(genericLabels.getString("OK"));
		selectButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				selectButton_actionPerformed(e);
			}
		});
		cancelButton.setText(genericLabels.getString("cancel"));
		cancelButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				cancelButton_actionPerformed(e);
			}
		});
		flowLayout1.setHgap(35);
		flowLayout1.setVgap(10);
		this.setTitle(dbeditLabels.getString("EquipmentModelSelectDialog.title"));
		jPanel2.setLayout(borderLayout2);
		jPanel2.setBorder(border1);
		clearButton.setText(genericLabels.getString("clear"));
		clearButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				clearButton_actionPerformed(e);
			}
		});
		getContentPane().add(panel1);
		panel1.add(jPanel1, BorderLayout.SOUTH);
		jPanel1.add(selectButton, null);
		jPanel1.add(clearButton, null);
		jPanel1.add(cancelButton, null);
		panel1.add(jPanel2, BorderLayout.CENTER);
		jPanel2.add(eqModSelectPanel, BorderLayout.NORTH);
	}

	/**
	 * Sets the current selection.
	 * 
	 * @param mod
	 *            the current selection.
	 */
	public void setSelection(EquipmentModel mod)
	{
		eqMod = null;
		if (mod == null)
			eqModSelectPanel.clearSelection();
		else
			eqModSelectPanel.setSelection(mod);
	}

	/**
	 * Called when Select button is pressed.
	 * 
	 * @param e
	 *            ignored
	 */
	void selectButton_actionPerformed(ActionEvent e)
	{
		eqMod = eqModSelectPanel.getSelectedEquipmentModel();
		_cancelled = false;
		closeDlg();
	}

	/** Closes the dialog. */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/**
	 * Called when Cancel button is pressed.
	 * 
	 * @param e
	 *            ignored
	 */
	void cancelButton_actionPerformed(ActionEvent e)
	{
		eqMod = null;
		_cancelled = true;
		closeDlg();
	}

	/** @return the selected object. */
	public EquipmentModel getSelectedEquipmentModel()
	{
		// Will return null if none selected
		return eqMod;
	}

	/**
	 * Clears the current selection, if one has been made.
	 * 
	 * @param e
	 *            ignored.
	 */
	void clearButton_actionPerformed(ActionEvent e)
	{
		eqMod = null;
		eqModSelectPanel.clearSelection();
	}

	/** @return true if cancel was pressed. */
	public boolean cancelled()
	{
		return _cancelled;
	}
}
