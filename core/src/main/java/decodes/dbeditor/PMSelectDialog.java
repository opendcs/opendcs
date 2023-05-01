package decodes.dbeditor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;

import decodes.datasource.GoesPMParser;
import decodes.datasource.IridiumPMParser;
import decodes.gui.GuiDialog;

@SuppressWarnings("serial")
public class PMSelectDialog
	extends GuiDialog
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	private JButton okButton = new JButton();
	private JButton cancelButton = new JButton();

	private boolean cancelled = false;
	private ArrayList<String> results = new ArrayList<String>();
	
	private String[] pmNames =
	{
		GoesPMParser.DCP_ADDRESS,
		GoesPMParser.MESSAGE_LENGTH,
		GoesPMParser.FAILURE_CODE,
		GoesPMParser.SIGNAL_STRENGTH,
		GoesPMParser.FREQ_OFFSET,
		GoesPMParser.MOD_INDEX,
		GoesPMParser.CHANNEL,
		GoesPMParser.SPACECRAFT,
		GoesPMParser.BAUD,
		GoesPMParser.DCP_MSG_FLAGS,
		GoesPMParser.GPS_SYNC,
		IridiumPMParser.LATITUDE,
		IridiumPMParser.LONGITUDE,
		IridiumPMParser.CEP_RADIUS
	};
	private Vector<JCheckBox> pmChecks = new Vector<JCheckBox>();

	public PMSelectDialog(JFrame owner)
	{
		super(owner, "", true);
		init();
	}

	private void init()
	{
		try
		{
			jbInit();
			getRootPane().setDefaultButton(okButton);
			pack();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	/** Initialize GUI components. */
	void jbInit() throws Exception
	{
		this.setModal(true);
		this.setTitle(dbeditLabels.getString("PMSelectDialog.title"));
		JPanel mainPanel = new JPanel(new BorderLayout());
		
		// Center panel is a scrollable list of checkboxes for each PM name.
		pmChecks.clear();
		for(String pmName : pmNames)
			pmChecks.add(new JCheckBox(pmName));
//		JList pmList = new JList(pmChecks);
		JScrollPane scrollPane = new JScrollPane();
		JPanel checkPanel = new JPanel(new GridLayout(pmNames.length, 1));
		for(JCheckBox pmCheck : pmChecks)
			checkPanel.add(pmCheck);
		
		scrollPane.getViewport().add(checkPanel, null);
		mainPanel.add(scrollPane, BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
		okButton.setText(genericLabels.getString("OK"));
		okButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				okPressed();
			}
		});
		cancelButton.setText(genericLabels.getString("cancel"));
		cancelButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				cancelPressed();
			}
		});
		buttonPanel.add(okButton, null);
		buttonPanel.add(cancelButton, null);
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);

		getContentPane().add(mainPanel);
	}

	/**
	 * Called when a double click on the selection
	 */
	void okPressed()
	{
		cancelled = false;
		for(JCheckBox pmCheck : pmChecks)
			if (pmCheck.isSelected())
				results.add(pmCheck.getText());
		closeDlg();
	}

	/** Closes dialog. */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/**
	 * Called when Cancel button is pressed.
	 */
	void cancelPressed()
	{
		cancelled = true;
		results.clear();
		closeDlg();
	}

	/** @return selected (single) site, or null if Cancel was pressed. */
	public ArrayList<String> getResults()
	{
		return results;
	}

	public boolean isCancelled()
	{
		return cancelled;
	}
}
