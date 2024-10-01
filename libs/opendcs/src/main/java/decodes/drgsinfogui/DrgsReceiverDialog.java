package decodes.drgsinfogui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;

/**
 * This Dialog diplays a DRGS Receiver Identification record.
 *
 */
public class DrgsReceiverDialog extends JDialog
{
	private String module = "DrgsReceiverDialog";
	private DrgsReceiversListFrame parent;
	private DrgsReceiverIdent drgsReceiver;
	private boolean save;
	public static boolean MODIFIED;//flag to indicate to the main frame if
	//user has modified a record or not
	
	private JTextField codeTextField = new JTextField();
	private JTextField descriptionTextField = new JTextField();
	private JTextField locationTextField = new JTextField();
	private JTextField contactTextField = new JTextField();
	private JTextField emailAddrTextField = new JTextField();
	private JTextField phoneNumTextField = new JTextField();
	
	private String dialogTitle;
	private String panelTitle;
	private String codeLabelStr;
	private String descriptionLabelStr;
	private String locationLabelStr;
	private String contactLabelStr;
	private String emailLabelStr;
	private String phoneNumLabelStr;
	private String okButtonLabel;
	private String cancelButtonLabel;
	
	/** Constructor */
	public DrgsReceiverDialog(DrgsReceiversListFrame parent)
	{
		super(parent);
		this.parent = parent;
		save = false;
		setAllLabels();
		jbInit();
		drgsReceiver = new DrgsReceiverIdent("","","","","","");
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(
			new WindowAdapter()
			{
				boolean started=false;
				public void windowActivated(WindowEvent e)
				{
					started = true;
				}
				public void windowClosing(WindowEvent e)
				{
					closeDlg();
				}
			});
	}
	
	/**
	 * Create SWING components
	 */
	private void jbInit()
	{
		JPanel mainPanel = new JPanel();
		JPanel identPanel = new JPanel();
		JPanel identPanelLeft = new JPanel();
		JPanel identPanelRight = new JPanel();
		JPanel buttonsPanel = new JPanel();
		String tip = "For Code Field:" +
				" Use at least one character in the range G ... Z";
		JLabel codeTip = new JLabel(tip);
		JLabel codeLabel = new JLabel(codeLabelStr);
		JLabel descriptionLabel = new JLabel(descriptionLabelStr);
		JLabel locationLabel = new JLabel(locationLabelStr);
		JLabel contactLabel = new JLabel(contactLabelStr);
		JLabel emailAddrLabel = new JLabel(emailLabelStr);
		JLabel phoneNumLabel = new JLabel(phoneNumLabelStr);
		JButton okButton = new JButton(okButtonLabel);
		JButton cancelButton = new JButton(cancelButtonLabel);
		
		this.setModal(true);
		this.setTitle(dialogTitle);
		this.setSize(new Dimension(700, 220));
		this.getContentPane().add(mainPanel);
		mainPanel.setLayout(new BorderLayout());
		TitledBorder mainPanelTitledBorder;
		mainPanelTitledBorder = new TitledBorder(BorderFactory
				.createEtchedBorder(Color.white, new Color(148, 145, 140)),
				panelTitle);
		mainPanel.setBorder(mainPanelTitledBorder);
		mainPanel.add(identPanel, BorderLayout.CENTER);
		mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
		okButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				okButton_actionPerformed(e);
			}
		});
		cancelButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				cancelButton_actionPerformed(e);
			}
		});
		//Create Identification Panel
		identPanel.setLayout(new GridBagLayout());
		identPanel.add(codeTip, 
				new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(0, 10, 0, 10), 2, 5));
		
		identPanel.add(identPanelLeft, 
				new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 10, 0, 10), 2, 5));
		identPanel.add(identPanelRight, 
				new GridBagConstraints(1, 1, 1, 1, 1.0,	0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 20, 0, 10), 2, 5));

		codeTextField.setMinimumSize(new Dimension(2, 20));
		codeTextField.setPreferredSize(new Dimension(2, 20));
		
		identPanelLeft.setLayout(new GridBagLayout());
		identPanelLeft.add(codeLabel, 
				new GridBagConstraints(0, 0, 1, 1, 0.0,	0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 2, 5));
		identPanelLeft.add(codeTextField, 
				new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 1), 62, 5));
		identPanelLeft.add(locationLabel, 
				new GridBagConstraints(0, 1, 1, 1, 0.0,	0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 0, 0, 0), 2, 5));
		identPanelLeft.add(locationTextField, 
				new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(5, 0, 0, 1), 62, 5));
		identPanelLeft.add(emailAddrLabel, 
				new GridBagConstraints(0, 2, 1, 1, 0.0,	0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 0, 0, 0), 2, 5));
		identPanelLeft.add(emailAddrTextField, 
				new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(5, 0, 0, 1), 62, 5));
		
		identPanelRight.setLayout(new GridBagLayout());
		identPanelRight.add(descriptionLabel, 
				new GridBagConstraints(0, 0, 1, 1, 0.0,	0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 2, 5));
		identPanelRight.add(descriptionTextField, 
				new GridBagConstraints(1, 0, 1, 1, 1.0,	0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 0, 0, 1), 62, 5));
		identPanelRight.add(contactLabel, 
				new GridBagConstraints(0, 1, 1, 1, 0.0,	0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 0, 0, 0), 2, 5));
		identPanelRight.add(contactTextField, 
				new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(5, 0, 0, 1), 62, 5));
		identPanelRight.add(phoneNumLabel, 
				new GridBagConstraints(0, 2, 1, 1, 0.0,	0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 0, 0, 0), 2, 5));
		identPanelRight.add(phoneNumTextField, 
				new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(5, 0, 0, 1), 62, 5));
		//Create buttons Panel
		buttonsPanel.setLayout(new GridBagLayout());
		buttonsPanel.add(okButton, 
				new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(10, 10, 10, 10), 2, 5));
		buttonsPanel.add(cancelButton, 
				new GridBagConstraints(1, 0, 1, 1, 0.0,	0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(10, 10, 10, 10), 2, 5));
	}
	
	/**
	 * Fill in all GUI values with the DRGS record selected by the user
	 * @param dr
	 */
	public void fillValues(DrgsReceiverIdent dr)
	{
		this.drgsReceiver = dr;
		//fill all components with dr obj
		codeTextField.setText(dr.getCode() == null ? "" : dr.getCode());
		descriptionTextField.setText(
				dr.getDescription() == null ? "" : dr.getDescription());
		locationTextField.setText(
				dr.getLocation() == null ? "" : dr.getLocation());
		contactTextField.setText(
				dr.getContact() == null ? "" : dr.getContact());
		emailAddrTextField.setText(
				dr.getEmailAddr() == null ? "" : dr.getEmailAddr());
		phoneNumTextField.setText(
				dr.getPhoneNum() == null ? "" : dr.getPhoneNum());
	}
	
	/**
	 * User presses the OK button
	 * @param e
	 */
	private void okButton_actionPerformed(ActionEvent e)
	{
		if (save())
		{
			closeDlg();
		}
	}
	
	/**
	 * Add this entry to the DRGS List - it will be saved when the user
	 * presses the Save All button.
	 * This not a really save method it is a add this record to the 
	 * list of DRGS Receivers that will be saved when the user presses
	 * the Save All button on the main frame.
	 * 
	 * @return
	 */
	private boolean save()
	{
		boolean result = true;
		//Get information from user and validate
		DrgsReceiverIdent dr = getDrgsReceiverInfo();

		//Add or modify a DRGS List record in the table model
		if (dr != null)
		{
			if (parent.getDrgsTableModel() != null)
			{
				//Check in here for duplicate codes???
				if (!drgsReceiver.getCode().equalsIgnoreCase(dr.getCode()) &&
						parent.getDrgsTableModel().drgsCodeExits(dr.getCode()))
				{
					JOptionPane.showMessageDialog(this,
					"This Code exists, Please type a different one.", 
							"Error!", JOptionPane.ERROR_MESSAGE);
					result = false;
				}
				else
				{
					parent.getDrgsTableModel().modifyDRGSList(
														drgsReceiver, dr);
					save = true;	
				}
			}
		}
		else
		{
			result = false;
		}
		return result;
	}

	/**
	 * Get fields from user and make sure that all fields are fill in.
	 * 
	 * @return
	 */
	private DrgsReceiverIdent getDrgsReceiverInfo()
	{
		DrgsReceiverIdent dr = null;
		if (codeTextField.getText() == null || 
				codeTextField.getText().equals("") ||
				descriptionTextField.getText() == null || 
				descriptionTextField.getText().equals("") ||
				locationTextField.getText() == null || 
				locationTextField.getText().equals("") ||
				contactTextField.getText() == null || 
				contactTextField.getText().equals("") ||
				emailAddrTextField.getText() == null || 
				emailAddrTextField.getText().equals("") ||
				phoneNumTextField.getText() == null || 
				phoneNumTextField.getText().equals("")
				)
		{
			//Missing fields - display error to user
			JOptionPane.showMessageDialog(this,
					"All Fields are required, Please fill in all fields", 
					"Error!", JOptionPane.ERROR_MESSAGE);
			dr = null;
		}
		else
		{
			//All fields in place - create a Drgs record
			dr = new DrgsReceiverIdent(codeTextField.getText(),
					descriptionTextField.getText(),
					locationTextField.getText(),
					contactTextField.getText(),
					emailAddrTextField.getText(),
					phoneNumTextField.getText());
		}		
		return dr;
	}
	
	/** 
	 * User presses cancel button
	 * 
	 * @param e
	 */
	private void cancelButton_actionPerformed(ActionEvent e)
	{
		closeDlg();
	}

	/** Closes the dialog. */
	private void closeDlg()
	{
		//Special code used by the main frame to verify if user modify
		//record or not
		if (hasChanged())
		{
			MODIFIED = true;
		}
		
		if (hasChanged() && save == false)
		{
			int r = JOptionPane.showConfirmDialog(this, "Save any changes?");
			if (r == JOptionPane.CANCEL_OPTION)
				return;
			else if (r == JOptionPane.YES_OPTION)
			{ 
				if (!save())
					return;
			} else if (r == JOptionPane.NO_OPTION)
			{
			}
		}
		setVisible(false);
		dispose();
	}
	
	private boolean hasChanged()
	{
		boolean result = false;
		
		if (drgsReceiver != null &&
			!drgsReceiver.getCode().equalsIgnoreCase(codeTextField.getText()))
		{
			result = true;
		}
		if (drgsReceiver != null &&
				!drgsReceiver.getDescription().equalsIgnoreCase(
				descriptionTextField.getText()))
		{
			result = true;
		}
		if (drgsReceiver != null &&
				!drgsReceiver.getLocation().equalsIgnoreCase(
				locationTextField.getText()))
		{
			result = true;
		}
		if (drgsReceiver != null &&
				!drgsReceiver.getContact().equalsIgnoreCase(
				contactTextField.getText()))
		{
			result = true;
		}
		if (drgsReceiver != null &&
				!drgsReceiver.getEmailAddr().equalsIgnoreCase(
				emailAddrTextField.getText()))
		{
			result = true;
		}
		if (drgsReceiver != null &&
				!drgsReceiver.getPhoneNum().equalsIgnoreCase(
				phoneNumTextField.getText()))
		{
			result = true;
		}
		return result;
	}

	/**
	 * Set all Labels of the GUI
	 */
	private void setAllLabels()
	{
		dialogTitle = "DRGS Receiver";
		panelTitle = "Identification";
		codeLabelStr = "Code (2 char):";
		descriptionLabelStr = "Description:";
		locationLabelStr = "Location:";
		contactLabelStr = "Contact:";
		emailLabelStr = "Email Addr:";
		phoneNumLabelStr = "Phone #:";
		okButtonLabel = "OK";
		cancelButtonLabel = "Cancel";
	}
}
