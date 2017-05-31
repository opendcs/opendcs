package decodes.tsdb.compedit;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.border.*;
import java.util.Properties;
import java.util.Iterator;
import java.util.ResourceBundle;

import ilex.util.TextUtil;
import ilex.util.PropertiesUtil;

import decodes.gui.*;
import decodes.tsdb.DbAlgoParm;
import decodes.tsdb.algo.RoleTypes;

public class AlgoParmDialog extends GuiDialog
{
	private String titleLabel;
	private String algoNameLabelText;
	private String roleNameLabel;
	private String parmTypeLabelText;
	private String OK;
	private String Cancel;
	private final String arbitraryDelta = "idMMM: Delta with # of minutes";

    private JPanel outerPanel = new JPanel();
	private BorderLayout outerLayout = new BorderLayout();
    private JPanel northPanel = new JPanel(new FlowLayout());
    private JLabel algoNameLabel;
    private JTextField algoNameField = new JTextField(30);
    private TitledBorder northBorder;

    private JPanel southButtonPanel = new JPanel();
    private JButton okButton = new JButton();
    private JButton cancelButton = new JButton();
    private FlowLayout southLayout = new FlowLayout();

    private JPanel fieldEntryPanel = new JPanel();
    private JLabel parmNameLabel;
    private JTextField parmNameField = new JTextField();
    private JLabel parmTypeLabel;
    private JComboBox parmTypeCombo = 
		new JComboBox(RoleTypes.getExpandedRoleTypes());
    private GridBagLayout fieldEntryLayout = new GridBagLayout();
    private JLabel numMinutesLabel;
    private JTextField numMinutesField = new JTextField();

	/** The object being edited */
	DbAlgoParm theParm;
	String algoName;
	
	private void fillLabels()
	{
		ResourceBundle compEditLabels =
			CAPEdit.instance().compeditDescriptions;
		titleLabel = compEditLabels.getString("AlgoParmDialog.Title");
		algoNameLabelText = compEditLabels.getString("AlgoParmDialog.AlgoName");
		roleNameLabel = compEditLabels.getString("AlgoParmDialog.RoleName");
		parmTypeLabelText = compEditLabels.getString("AlgoParmDialog.ParmType");
		OK = CAPEdit.instance().genericDescriptions.getString("OK");
		Cancel = CAPEdit.instance().genericDescriptions.getString("cancel");
		algoNameLabel = new JLabel(algoNameLabelText);
		parmNameLabel = new JLabel(roleNameLabel);
		parmTypeLabel = new JLabel(parmTypeLabelText);
		numMinutesLabel = new JLabel(compEditLabels.getString(
			"AlgoParmDialog.NumMinutes"));
	}

	boolean okPressed = false;

	public AlgoParmDialog(String algoName, DbAlgoParm dap)
	{
		super(CAPEdit.instance().getFrame(), CAPEdit.instance().compeditDescriptions.getString("AlgoParmDialog.Title"), true);
		
		theParm = dap;
		this.algoName = algoName;

        try
		{
        	fillLabels();
            jbInit();
			pack();
			fillValues();
			parmNameField.requestFocus();
			getRootPane().setDefaultButton(okButton);
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }


	/** Fills the GUI components with values from the object being edited. */
	void fillValues()
	{
		algoNameField.setText(algoName);
    	parmNameField.setText(theParm.getRoleName());
		String pt = theParm.getParmType();
		int idx = RoleTypes.getIndex(pt);
		if (idx != -1)
			parmTypeCombo.setSelectedIndex(idx);
		else if (pt.toLowerCase().startsWith("id"))
		{
			parmTypeCombo.setSelectedItem(arbitraryDelta);
			numMinutesField.setText(pt.substring(2));
		}
	}

	/** Initializes GUI components. */
    void jbInit() throws Exception
	{
        outerPanel.setLayout(outerLayout);
        outerPanel.add(northPanel, BorderLayout.NORTH);
        outerPanel.add(fieldEntryPanel, BorderLayout.CENTER);
        outerPanel.add(southButtonPanel, BorderLayout.SOUTH);

        northBorder = new TitledBorder(
			BorderFactory.createLineBorder(new Color(153, 153, 153),2),"");
        northPanel.setBorder(northBorder);

        algoNameField.setEditable(false);
        northPanel.add(algoNameLabel, null);
        northPanel.add(algoNameField, null);

        okButton.setText("   "+OK+"   ");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                okButtonPressed();
            }
        });
        cancelButton.setText(Cancel);
        cancelButton.addActionListener(
			new java.awt.event.ActionListener() 
			{
            	public void actionPerformed(ActionEvent e) 
				{
                	cancelButtonPressed();
            	}
        	});
        southButtonPanel.setLayout(southLayout);
        southLayout.setHgap(35);
        southLayout.setVgap(10);
        southButtonPanel.add(okButton, null);
        southButtonPanel.add(cancelButton, null);

        fieldEntryPanel.setLayout(fieldEntryLayout);
        parmNameLabel.setPreferredSize(new Dimension(140, 17));
        parmNameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        parmNameField.setToolTipText("Enter sensor name (no spaces)");
        parmTypeLabel.setPreferredSize(new Dimension(140, 17));
        parmTypeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        fieldEntryPanel.add(parmNameLabel, 
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
            	GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(10, 15, 5, 2), 35, 0));
        fieldEntryPanel.add(parmNameField, 
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
           		GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(10, 0, 5, 15), 0, 0));
        fieldEntryPanel.add(parmTypeLabel, 
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
            	GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(5, 15, 5, 2), 0, 0));
        fieldEntryPanel.add(parmTypeCombo,
			new GridBagConstraints(1, 1, 1, 1, 1.0, 1.0,
            	GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
				new Insets(5, 0, 5, 15), 0, 0));
    	parmTypeCombo.addItem(arbitraryDelta);
		parmTypeCombo.addActionListener(
			new java.awt.event.ActionListener() 
			{
            	public void actionPerformed(ActionEvent e) 
				{
                	parmTypeSelected();
            	}
        	});
		fieldEntryPanel.add(numMinutesLabel,
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
            	GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(5, 15, 10, 2), 0, 0));
        fieldEntryPanel.add(numMinutesField, 
			new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,
           		GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(10, 0, 10, 15), 0, 0));
		parmTypeSelected();

		//outerPanel.setPreferredSize(new Dimension(320, 130));
		getContentPane().add(outerPanel);
    }

	/**
	  Called when the OK button is pressed.
	*/
    void okButtonPressed()
	{
		okPressed = true;
		theParm.setRoleName(parmNameField.getText());
		String pt = (String)parmTypeCombo.getSelectedItem();
		
		if (pt == arbitraryDelta)
			pt = "id" + numMinutesField.getText().trim();
		else
			pt = pt.substring(0, pt.indexOf(':'));
		theParm.setParmType(pt);

		closeDlg();
    }

	/** Closes the dialog. */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/**
	  Called when the Cancel button is pressed.
	*/
    void cancelButtonPressed()
	{
		okPressed = false;
		closeDlg();
    }

	private void parmTypeSelected()
	{
		String pt = (String)parmTypeCombo.getSelectedItem();
		numMinutesLabel.setEnabled(pt == arbitraryDelta);
		numMinutesField.setEnabled(pt == arbitraryDelta);
	}
}
