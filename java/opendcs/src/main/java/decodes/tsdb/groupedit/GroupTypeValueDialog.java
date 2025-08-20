package decodes.tsdb.groupedit;

import ilex.util.LoadResourceBundle;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import decodes.db.EnumValue;
import decodes.gui.TopFrame;
import decodes.util.DecodesSettings;

/**
This dialog edits Group Type combo box. The user
can add new values from here. It is used by
the TsGroupDefinitionPanel class.
*/
public class GroupTypeValueDialog extends JDialog 
{
	//Dialog
	private String module = "GroupTypeValueDialog";
	//Dialog Owner

	//Dialog Components
	private JPanel dialogPanel;
	private JPanel textFieldPanel;
	private JPanel ctrlPanel;
	private JLabel grpTypeNameLabel;
	private JLabel grpTypeDescLabel;
	private JTextField grpTypeNameField;
	private JTextField grpTypeDescField;

	private JButton okButton;
	private JButton cancelButton;

	//Miscellaneous
	private EnumValue theEV;
	private boolean _wasChanged = false;
	private JTextField execClassField = new JTextField();
	private JTextField enumNameField = new JTextField();
	//Titles, Labels defined here for internationalization
	private String dialogTitle;
	private String mnemonicLabel;
	private String completeDescLabel;
	private String okLabel;
	private String cancelLabel;
	
	/**
	 * Constructor.
	 */
	public GroupTypeValueDialog(JFrame owner)
		throws HeadlessException 
	{
		super(owner);

		ResourceBundle groupResources = LoadResourceBundle.getLabelDescriptions(
			"decodes/resources/groupedit", DecodesSettings.instance().language);
		ResourceBundle genericResources = LoadResourceBundle.getLabelDescriptions(
			"decodes/resources/generic",
			DecodesSettings.instance().language);

		dialogTitle = groupResources.getString(
				"GroupTypeValueDialog.dialogTitle");
		mnemonicLabel = groupResources.getString(
				"GroupTypeValueDialog.mnemonicLabel");
		completeDescLabel = groupResources.getString(
				"GroupTypeValueDialog.completeDescLabel");
		okLabel = genericResources.getString("OK");
		cancelLabel = genericResources.getString("cancel");

		try 
		{
			jbInit();
			
			pack();
		} catch(Exception e) 
		{
			e.printStackTrace();
		}
		theEV = null;

		addWindowListener(
			new WindowAdapter()
			{
				boolean started=false;
				public void windowActivated(WindowEvent e)
				{
					if (!started)
						grpTypeNameField.requestFocus();
					started = true;
				}
			});
	}

	/** Initializes GUI components. */
	private void jbInit() throws Exception 
	{
		this.setModal(true);
		this.setTitle(dialogTitle);
		this.setPreferredSize(new Dimension(500, 200));
		this.setResizable(false);
		
		dialogPanel = new JPanel(new BorderLayout());
		textFieldPanel = new JPanel(new GridBagLayout());
		ctrlPanel = new JPanel(new GridBagLayout());

		grpTypeNameLabel = new JLabel(mnemonicLabel);
		grpTypeDescLabel = new JLabel(completeDescLabel);
		grpTypeNameField = new JTextField("");
		grpTypeDescField = new JTextField("");

		cancelButton = new JButton(cancelLabel);
		okButton = new JButton(okLabel);
		okButton.setPreferredSize(cancelButton.getPreferredSize());
		okButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				okButton_actionPerformed(e);
			}
		});
		cancelButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cancelButton_actionPerformed(e);
			}
		});


		this.getContentPane().add(dialogPanel, BorderLayout.CENTER);
		dialogPanel.add(textFieldPanel, BorderLayout.CENTER);
		dialogPanel.add(ctrlPanel, BorderLayout.SOUTH);

		ctrlPanel.add(okButton,
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
				new Insets(12, 10, 12, 10), 0, 0));;
		ctrlPanel.add(cancelButton,
			new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
				new Insets(12, 10, 12, 10), 0, 0));;
		
		textFieldPanel.add(grpTypeNameLabel,
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(10, 20, 5, 0), 0, 0));
		textFieldPanel.add(grpTypeDescLabel,
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(10, 20, 5, 0), 0, 0));
		textFieldPanel.add(grpTypeNameField,
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(10, 2, 5, 20), 0, 0));
		textFieldPanel.add(grpTypeDescField, 
			new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(10, 2, 5, 20), 0, 0));

	}

	/**
	 * Fills the dialog with the specified EnumValue attributes.
	 * @param ev the EnumValue
	 */
	public void fillValues(EnumValue ev)
	{
		theEV = ev;
		enumNameField.setText(ev.getDbenum().enumName);
		grpTypeNameField.setText(ev.getValue() != null ? ev.getValue() : "");
		grpTypeDescField.setText(ev.getDescription() != null ? ev.getDescription() : "");
		execClassField.setText(ev.getExecClassName() != null ? ev.getExecClassName(): "");
		_wasChanged = false;
	}

	
	
	/**
	 * Called when OK button is pressed.
	 * @param e ignored.
	 */
	void okButton_actionPerformed(ActionEvent e) 
	{
		String v = grpTypeNameField.getText();
		if (!v.equals(theEV.getValue()))
		{
			_wasChanged = true;
			theEV.setValue(v);
		}	
		v = grpTypeDescField.getText();
		if (!v.equals(theEV.getDescription()))
		{
			_wasChanged = true;
			theEV.setDescription(v);
		}	
		v = execClassField.getText();
		if (!v.equals(theEV.getExecClassName()))
		{
			_wasChanged = true;
			theEV.setExecClassName(v);
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

	/** @return true if values in the dialog were changed. */
	public boolean wasChanged() { return _wasChanged; }

	/** Closes the dialog. */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}
}
