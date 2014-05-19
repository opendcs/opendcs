/*
*  $Id$
*
*  $Log$
*  Revision 1.2  2013/01/23 22:05:19  gchen
*  Comment out the line public char getType() because it cannot be compiled within the 64-bit java 1.7.
*
*  Revision 1.1  2008/04/04 18:21:14  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/31 21:08:41  mjmaloney
*  javadoc
*
*/
package lrgs.gui;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public class NetlistDialog extends JDialog
{
    BorderLayout borderLayout1 = new BorderLayout();


	private boolean ok;
    JPanel jPanel4 = new JPanel();
    JPanel jPanel1 = new JPanel();
    JLabel jLabel1 = new JLabel();
    BorderLayout borderLayout2 = new BorderLayout();
    FlowLayout flowLayout2 = new FlowLayout();
    JTextField fileNameField = new JTextField();
    JPanel jPanel3 = new JPanel();
    JComboBox fileTypeCombo = new JComboBox(
		new String[] { "Local", "Shared", "Remote" } );
    JCheckBox useNameCheckbox = new JCheckBox();
    JLabel jLabel3 = new JLabel();
    JLabel jLabel2 = new JLabel();
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JPanel jPanel2 = new JPanel();
    FlowLayout flowLayout1 = new FlowLayout();
    JButton cancelButton = new JButton();
    JButton okButton = new JButton();

    public NetlistDialog()
	{
        try {
            jbInit();
			ok = false;
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    private void jbInit() throws Exception {
        this.getContentPane().setLayout(borderLayout1);
        jPanel1.setBorder(BorderFactory.createLineBorder(Color.black));
        jPanel1.setLayout(flowLayout2);
        jLabel1.setText("LRGS Network List");
        jPanel4.setLayout(borderLayout2);
        jPanel3.setLayout(gridBagLayout1);
        useNameCheckbox.setText("Use DCP Names in this List");
        jLabel3.setText("File Type:");
        jLabel2.setText("File Name:");
        jPanel2.setLayout(flowLayout1);
        flowLayout1.setHgap(25);
        cancelButton.setPreferredSize(new Dimension(80, 27));
        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancelButton_actionPerformed(e);
            }
        });
        okButton.setPreferredSize(new Dimension(80, 27));
        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                okButton_actionPerformed(e);
            }
        });
        this.setModal(true);
        this.getContentPane().add(jPanel4, BorderLayout.CENTER);
        jPanel4.add(jPanel1, BorderLayout.NORTH);
        jPanel1.add(jLabel1, null);
        jPanel4.add(jPanel3, BorderLayout.CENTER);
        jPanel3.add(jLabel2, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(20, 42, 0, 0), 0, 0));
        jPanel3.add(fileNameField, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(20, 6, 0, 20), 150, 0));
        jPanel3.add(jLabel3, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(16, 50, 0, 0), 0, 0));
        jPanel3.add(fileTypeCombo, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(14, 6, 0, 118), 0, 0));
        jPanel3.add(useNameCheckbox, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(13, 6, 17, 63), 0, 0));
        jPanel4.add(jPanel2, BorderLayout.SOUTH);
        jPanel2.add(okButton, null);
        jPanel2.add(cancelButton, null);
    }

	public void set(String filename, char type, boolean useNames)
	{
    	fileNameField.setText(filename);
		if (type == 'L')
			fileTypeCombo.setSelectedIndex(0);
		else if (type == 'S')
			fileTypeCombo.setSelectedIndex(1);
		if (type == 'R')
			fileTypeCombo.setSelectedIndex(2);
    	useNameCheckbox.setSelected(useNames);
	}

	public String getFileName() { return fileNameField.getText(); }

/*	
	public char getType()
	{
		return ((String)fileTypeCombo.getSelectedItem()).charAt(0);
	}
*/
	
	public boolean getUseNames() { return useNameCheckbox.isSelected(); }

	public boolean okPressed()
	{
		return ok;
	}

    void okButton_actionPerformed(ActionEvent e)
	{
		ok = true;
		setVisible(false);
		dispose();
    }

    void cancelButton_actionPerformed(ActionEvent e)
	{
		ok = false;
		setVisible(false);
		dispose();
    }

}
