/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:14  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2004/08/31 21:08:41  mjmaloney
*  javadoc
*
*/
package lrgs.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.*;


/**
Simple dialog to enter new DDS user and optionally, password.
*/
public class NewUserDialog extends JDialog 
{
	JLabel jLabel1 = new JLabel();
	JLabel jLabel2 = new JLabel();
	JTextField userNameField = new JTextField();
	JTextField passwdField = new JPasswordField();
	JPanel jPanel1 = new JPanel();
	JTextArea infoArea = new JTextArea()
		{ public boolean isFocusTraversable() { return false; } };

	GridBagLayout gridBagLayout1 = new GridBagLayout();
	Border border1;
	BorderLayout borderLayout1 = new BorderLayout();
	JPanel jPanel2 = new JPanel();
	FlowLayout flowLayout1 = new FlowLayout();
	JButton okButton = new JButton();
	JButton cancelButton = new JButton();

	private boolean ok = false;

	public NewUserDialog() 
	{
		super((Frame)null, true);
		try {
			jbInit();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	public NewUserDialog(Frame owner)
	{
		super(owner, true);
		try {
			jbInit();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		okButton.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent av)
				{
					doOK();
				}
			});
		cancelButton.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent av)
				{
					doCancel();
				}
			});

		addWindowListener(
			new WindowAdapter()
			{
				boolean started=false;
				public void windowActivated(WindowEvent e)
				{
					if (!started)
						userNameField.requestFocus();
					started = true;
				}
			});
	}

	void setInfo(String s)
	{
			infoArea.setText(s);
	}

	private void jbInit() throws Exception 
	{
		border1 = BorderFactory.createBevelBorder(BevelBorder.LOWERED,Color.white,Color.white,new Color(148, 145, 140),new Color(103, 101, 98));
		jLabel1.setHorizontalAlignment(SwingConstants.RIGHT);
		jLabel1.setText("User Name:");
		this.setTitle("New DCP Data Service User");
		this.getContentPane().setLayout(gridBagLayout1);
		//jLabel2.setPreferredSize(new Dimension(67, 17));
		jLabel2.setHorizontalAlignment(SwingConstants.RIGHT);
		jLabel2.setText("Password:");
		userNameField.setPreferredSize(new Dimension(100, 21));
		passwdField.setPreferredSize(new Dimension(100, 21));
		jPanel1.setLayout(borderLayout1);
		infoArea.setWrapStyleWord(true);
		infoArea.setLineWrap(true);
		//infoArea.setText("Leave Password field blank for unauthenticated users.");
		infoArea.setEditable(false);
		infoArea.setBackground(new Color(212, 208, 200));
		jPanel1.setBorder(border1);
		jPanel2.setLayout(flowLayout1);
		flowLayout1.setHgap(20);
		//okButton.setMinimumSize(new Dimension(80, 27));
		//okButton.setPreferredSize(new Dimension(80, 27));
		okButton.setText("  OK  ");
		//cancelButton.setMinimumSize(new Dimension(80, 27));
		//cancelButton.setPreferredSize(new Dimension(80, 27));
		cancelButton.setText("Cancel");
		this.getContentPane().add(jLabel1, new GridBagConstraints(0, 0, 1, 1, 0.3, 0.0
			,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 7, 3, 3), 0, 0));
			this.getContentPane().add(jLabel2, new GridBagConstraints(0, 1, 1, 1, 0.3, 0.0
		,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(3, 7, 3, 3), 0, 0));
		this.getContentPane().add(passwdField, new GridBagConstraints(1, 1, 1, 1, 0.7, 0.0
		,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(3, 1, 3, 12), 0, 0));
		this.getContentPane().add(userNameField, new GridBagConstraints(1, 0, 1, 1, 0.7, 0.0
		,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(7, 1, 3, 12), 0, 0));
		this.getContentPane().add(jPanel1, new GridBagConstraints(0, 2, 2, 1, 1.0, 1.0
		,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10, 10, 10, 10), 0, 0));
		jPanel1.add(infoArea, BorderLayout.CENTER);
		this.getContentPane().add(jPanel2, new GridBagConstraints(0, 3, 2, 1, 1.0, 0.0
		,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		jPanel2.add(okButton, null);
		jPanel2.add(cancelButton, null);
	}

	private void doOK()
	{
		String user = userNameField.getText();
		if (user == null || user.length() == 0)
		{
			JOptionPane.showMessageDialog(null, "User name cannot be blank!", 
				"Error!", JOptionPane.ERROR_MESSAGE);
			return;
		}

		String passwd = passwdField.getText();
		if (passwd != null && passwd.length() > 0)
		{
			if (passwd.length() < 6)
			{
				JOptionPane.showMessageDialog(null, 
					"Password too short. Must be at least 6 characters.",
					"Error!", JOptionPane.ERROR_MESSAGE);
				return;
			}
			else if (passwd.equals(userNameField.getText()))
			{
				JOptionPane.showMessageDialog(null, 
					"Password must not be the same as user name.",
					"Error!", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		ok = true;
		closeDlg();
	}
	
	private void doCancel()
	{
		ok = false;
		closeDlg();
	}

	public boolean isOK() { return ok; }
	
	private void closeDlg()
	{
		setVisible(false);
		dispose();
	}

//	public void setVisible(boolean tf)
//	{
//		super.setVisible(tf);
//	}
		
}
