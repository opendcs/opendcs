/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:09  cvs
*  Added legacy code to repository
*
*  Revision 1.7  2008/01/27 12:01:23  mmaloney
*  modfied files
*
*  Revision 1.6  2007/12/05 22:44:36  mmaloney
*  dev
*
*  Revision 1.5  2005/12/30 19:36:33  mmaloney
*  Added DES Encrypter. Generalized Login Dialog.
*
*  Revision 1.4  2004/08/30 14:50:19  mjmaloney
*  Javadocs
*
*  Revision 1.3  2002/05/18 20:02:31  mjmaloney
*  Set initial focus to username field when dialog is started.
*
*  Revision 1.2  2000/05/23 11:23:25  mike
*  SingleClickButton modification.
*
*  Revision 1.1  2000/03/28 16:43:53  mike
*  Created
*
*/
package ilex.gui;

import ilex.util.LoadResourceBundle;

import java.awt.*;
import java.awt.event.*;
import java.util.ResourceBundle;

import javax.swing.*;


/**
* LoginDialog is a modal dialog that queries a user for a login name
* and password. After displaying the dialog, call isOK() to determine if
* 'OK' was pressed (as opposed to 'Cancel'. If it was, call getName()
* and getPassword() to retrieve the values entered by the user.
* <p>
* You can re-use the same LoginDialog object by calling clear before
* making it visible.
*/
public class LoginDialog extends JDialog
{
	private static ResourceBundle labels = null;
	private JButton okButton, cancelButton;
	private JTextField usernameField;
	private JPasswordField passwordField;
	private boolean ok;
	
	/**
	* Constructs a new LrgsAccess GUI display.
	* @param f the owning frame
	* @param title the title of the dialog
	*/
	public LoginDialog( JFrame f, String title )
	{
		super(f, title, true);
		getLabels();
		ok = false;
//		Point loc = f.getLocation();
//		setBounds(loc.x+40, loc.y+40, 300, 200);
		Container contpane = getContentPane();

		contpane.setLayout(new BorderLayout());

		// South will contain 'OK' and 'Cancel' buttons.
		JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 7, 7));

		okButton = new JButton(labels.getString("EditPropsAction.ok"));
		okButton.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent av)
				{
					doOK();
				}
			});
		south.add(okButton);
		
		cancelButton = new JButton(labels.getString("EditPropsAction.cancel"));
		cancelButton.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent av)
				{
					doCancel();
				}
			});
		south.add(cancelButton);
		contpane.add(south, BorderLayout.SOUTH);

		// Center will contain a grid with login and password prompts.
		JPanel center = new JPanel(new GridBagLayout());
		JLabel userNameLabel = new JLabel(
				labels.getString("LoginDialog.username"));
		center.add(userNameLabel,
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 10, 3, 2), 0, 0));

		usernameField = new JTextField(10);
		center.add(usernameField,
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(5, 0, 3, 10), 0, 0));

		JLabel passwordLabel = new JLabel(
				labels.getString("LoginDialog.Password"));
		center.add(passwordLabel,
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 10, 5, 2), 0, 0));

		passwordField = new JPasswordField(10);
		center.add(passwordField,
			new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 5, 10), 0, 0));

		contpane.add(center, BorderLayout.CENTER);

		addWindowListener(
			new WindowAdapter()
			{
				boolean started=false;
				public void windowActivated(WindowEvent e)
				{
					if (!started)
					{
						if (usernameField.isEnabled())
							usernameField.requestFocus();
						else
							passwordField.requestFocus();
					}
					started = true;
				}
			});

		getRootPane().setDefaultButton(okButton);
		pack();
	}
	
	public static ResourceBundle getLabels() 
	{
		if (labels == null)
			//Load the labels properties file
			labels = LoadResourceBundle.getLabelDescriptions(
					"ilex/resources/gui", null);
		return labels;
	}
	
	/**
	  Clears username and password fields. 
	*/
	public void clear( )
	{
		usernameField.setText("");
		passwordField.setText("");
		ok = false;
	}

	/** Called when OK pressed. */
	private void doOK( )
	{
		ok = true;
		closeDlg();
	}
	
	/** Called when cancel pressed. */
	private void doCancel( )
	{
		ok = false;
		closeDlg();
	}
	
	/** Closes the dialog */
	private void closeDlg( )
	{
		setVisible(false);
		dispose();
	}
	
	/**
	* @return true if OK was pressed, false if Cancel was pressed.
	*/
	public boolean isOK( )
	{
		return ok;
	}
	
	/**
	 * @return the user name entered in the dialog.
	 */
	public String getName( )
	{
		return usernameField.getText();
	}
	
	/**
	 * For greater security, you should set each character to a blank
	 * after using the password.
	 * @return the password entered in the dialog.
	 */
	public char[] getPassword( )
	{
		return passwordField.getPassword();
	}

	/**
	 * Enables or Disables the username field.
	 * @param tf true to enable username field.
	 * @param user the name to put in the username field.
	 */
	public void setEditableUsername(boolean tf, String user)
	{
		usernameField.setText(user);
		usernameField.setEnabled(tf);
	}
}
