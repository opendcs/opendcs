/*
*  $Id$
*/
package decodes.dbeditor;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.ResourceBundle;
import javax.swing.border.*;

import decodes.db.PlatformConfig;

/** Dialog for selecting a configuration from a list. */
public class ConfigSelectDialog extends JDialog
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

    JPanel panel1 = new JPanel();
    JPanel jPanel1 = new JPanel();
    FlowLayout flowLayout1 = new FlowLayout();
    JButton selectButton = new JButton();
    JButton cancelButton = new JButton();

	ConfigSelectController myController;
    BorderLayout borderLayout1 = new BorderLayout();
    JPanel jPanel2 = new JPanel();
    BorderLayout borderLayout2 = new BorderLayout();
    TitledBorder titledBorder1;
    Border border1;
    ConfigSelectPanel configSelectPanel = new ConfigSelectPanel();
	PlatformConfig config;

	/** 
	  Construct new dialog.
	  @param ctl the owner of this dialog to receive a call-back when a 
	  selection has been made.
	*/
    public ConfigSelectDialog(JFrame parent, ConfigSelectController ctl)
	{
        super(parent, dbeditLabels.getString("ConfigSelectDialog.title"), true);
		myController = ctl;
		allInit();
		configSelectPanel.setParentDialog(this);
	}

	/** 
	  Construct new dialog with dialog parent.
	  @param ctl the owner of this dialog to receive a call-back when a 
	  selection has been made.
	*/
    public ConfigSelectDialog(JDialog parent)
	{
        super(parent, dbeditLabels.getString("ConfigSelectDialog.title"), true);
		myController = null;
		allInit();
	}

	private void allInit()
	{
		config = null;
        try {
            jbInit();
			getRootPane().setDefaultButton(selectButton);
            pack();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

	/** Initializes GUI components */
    void jbInit() throws Exception {
        titledBorder1 = new TitledBorder(BorderFactory.createLineBorder(
			new Color(153, 153, 153),2),
			dbeditLabels.getString("ConfigSelectDialog.title"));
        border1 = BorderFactory.createCompoundBorder(titledBorder1,BorderFactory.createEmptyBorder(5,5,5,5));
        panel1.setLayout(borderLayout1);
        jPanel1.setLayout(flowLayout1);
        selectButton.setText(genericLabels.getString("select"));
        selectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectButtonPressed();
            }
        });
        cancelButton.setText(genericLabels.getString("cancel"));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancelButton_actionPerformed(e);
            }
        });
        flowLayout1.setHgap(35);
        flowLayout1.setVgap(10);
        jPanel2.setLayout(borderLayout2);
        jPanel2.setBorder(border1);
        getContentPane().add(panel1);
        panel1.add(jPanel1, BorderLayout.SOUTH);
        jPanel1.add(selectButton, null);
        jPanel1.add(cancelButton, null);
        panel1.add(jPanel2, BorderLayout.CENTER);
        jPanel2.add(configSelectPanel, BorderLayout.NORTH);
    }

	/**
	  Called when the Select button is pressed.
	*/
    void selectButtonPressed()
	{
		config = configSelectPanel.getSelection();
		if (myController != null)
			myController.selectConfig(config);
		closeDlg();
    }

	/** Closes the dialog */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/**
	  Called when Cancel the button is pressed.
	  @param e ignored.
	*/
    void cancelButton_actionPerformed(ActionEvent e)
	{
		config = null;
		closeDlg();
    }

	/** Returns the selected configuration, or null if none selected. */
	public PlatformConfig getSelectedConfig()
	{
		return config;
	}

	/** Sets current selection. */
	public void setSelection(String name)
	{
		if (name == null)
			configSelectPanel.clearSelection();
		else
			configSelectPanel.setSelection(name);
	}
}
