/*
 * $Id$
 * 
 * $Log$
 * Revision 1.5  2013/03/28 19:19:32  mmaloney
 * User temp files are now placed under DCSTOOL_USERDIR which may be different
 * from DCSTOOL_HOME on linux/unix multi-user installations.
 *
 * Revision 1.4  2012/10/30 17:55:06  mmaloney
 * dev
 *
 * Revision 1.3  2012/10/30 16:09:14  mmaloney
 * dev
 *
 * Revision 1.2  2012/10/30 15:50:13  mmaloney
 * dev
 *
 * Revision 1.1  2012/10/30 15:46:37  mmaloney
 * dev
 *
 * 
 * Open source software written for U.S. Army Corps of Engineers.
 * Author: Mike Maloney, Cove Software, LLC
 */
package decodes.cwms.rating;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import java.util.ResourceBundle;

import ilex.util.EnvExpander;
import ilex.util.LoadResourceBundle;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.gui.TopFrame;
import decodes.gui.GuiDialog;
import decodes.tsdb.TimeSeriesDb;
import decodes.util.DecodesSettings;

/**
This class implements the dialog displayed for exporting ratings.
*/
@SuppressWarnings("serial")
public class ExportDialog extends GuiDialog
{
	private JButton exportButton = new JButton();
	private JRadioButton onlySelectedRadio = new JRadioButton("Only Selected Rating");
	private JRadioButton allRelatedRadio = new JRadioButton("All Ratings with same Spec");
	private JTextField outputFileField = new JTextField();
	private DecodesSettings settings = DecodesSettings.instance();
	private ResourceBundle genericLabels = 
		LoadResourceBundle.getLabelDescriptions(
			"decodes/resources/generic", settings.language);

	static private JFileChooser fileChooser = new JFileChooser(
		EnvExpander.expand("$DCSTOOL_USERDIR"));

	private TimeSeriesDb tsdb = null;
	private CwmsRatingRef cwmsRatingRef = null;
	
	/**
	 * Constructor.
	 * @param owner the db editor top-frame.
	 * @param title
	 */
	public ExportDialog(TopFrame parent, TimeSeriesDb tsdb, CwmsRatingRef cwmsRatingRef)
	{
		super(parent, "Export Ratings to XML", true);
		this.tsdb = tsdb;
		this.cwmsRatingRef = cwmsRatingRef;
		try
		{
			setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			jbInit();
			pack();
		}
		catch (Exception exception)
		{
			exception.printStackTrace();
		}
	}

	private void jbInit() throws Exception
	{
		JPanel mainPanel = new JPanel(new BorderLayout());
		
		JButton doneButton = new JButton(genericLabels.getString("done"));
		doneButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				doneButtonPressed();
			}
		});
		
		exportButton.setText(genericLabels.getString("export"));
		exportButton.setEnabled(false);
		exportButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				exportButtonPressed();
			}
		});

		JPanel southButtonPanel = new JPanel(new FlowLayout());
		southButtonPanel.add(doneButton);
		southButtonPanel.add(exportButton);
		mainPanel.add(southButtonPanel, java.awt.BorderLayout.SOUTH);

		JPanel whatPanel = new JPanel(new GridLayout(2, 1));

		ButtonGroup radioButtonGroup = new ButtonGroup();
		radioButtonGroup.add(onlySelectedRadio);
		radioButtonGroup.add(allRelatedRadio);
		whatPanel.add(onlySelectedRadio);
		whatPanel.add(allRelatedRadio);
		onlySelectedRadio.setSelected(true);
		allRelatedRadio.setSelected(false);

		Border whatEtchedBorder = BorderFactory.createEtchedBorder(Color.white,
			new Color(165, 163, 151));
		Border whatTitledBorder = new TitledBorder(whatEtchedBorder, "What to Export?");
		whatPanel.setBorder(whatTitledBorder);
		mainPanel.add(whatPanel, java.awt.BorderLayout.NORTH);
		
		JPanel fileSelectPanel = new JPanel(new GridBagLayout());
		fileSelectPanel.add(new JLabel(genericLabels.getString("filename")),
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(10, 10, 10, 2), 0, 0));
		
		outputFileField.setText("");
		outputFileField.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				outputFileEntered();
			}
		});
		fileSelectPanel.add(outputFileField,
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(10, 0, 10, 10), 300, 0));
		
		JButton chooseButton = new JButton(genericLabels.getString("choose"));
		chooseButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				chooseButtonPressed();
			}
		});
		fileSelectPanel.add(chooseButton,
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(10, 5, 10, 10), 0, 0));
	
		mainPanel.add(fileSelectPanel, BorderLayout.CENTER);
		
		getContentPane().add(mainPanel);
	}

	public void doneButtonPressed()
	{
		closeDlg();
	}

	public void exportButtonPressed()
	{
		String fn = outputFileField.getText().trim();
		File f = new File(fn);
		if (f.exists())
		{
			int r =  JOptionPane.showConfirmDialog(this,
				LoadResourceBundle.sprintf(
					genericLabels.getString("overwriteConfirm"), fn));
			if (r != JOptionPane.YES_OPTION)
				return;
		}
		FileWriter fw = null;
		try
		{
			fw = new FileWriter(f);
			CwmsRatingDao crd = new CwmsRatingDao((CwmsTimeSeriesDb)tsdb);
			String s = crd.toXmlString(cwmsRatingRef, allRelatedRadio.isSelected());
			fw.write(s);
		}
		catch(Exception ex)
		{
			showError("Cannot save to '" + f.getPath() + "': " + ex);
		}
		finally
		{
			if (fw != null)
				try { fw.close(); }
				catch(Exception ex) {}
		}
	}

	public void chooseButtonPressed()
	{
		if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
		{
			File f = fileChooser.getSelectedFile();
			outputFileField.setText(f.getPath());
			outputFileEntered();
		}
	}

	public void outputFileEntered()
	{
		if (outputFileField.getText().trim().length() > 0)
		{
			exportButton.setEnabled(true);
		}
	}

//	public static void main(String args[])
//	{
//		ExportDialog dlg = new ExportDialog(null, theDb);
//		dlg.setVisible(true);
//	}


	private void closeDlg()
	{
		setVisible(false);
		dispose();
	}

}
