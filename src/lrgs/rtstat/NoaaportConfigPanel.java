/**
 * $Id$
 * 
 * $Log$
 * Revision 1.7  2013/01/30 21:25:17  mmaloney
 * PDI Initialization
 *
 * Revision 1.6  2013/01/30 20:39:35  mmaloney
 * Added new PDI Noaaport Stuff.
 *
 * Revision 1.5  2011/05/11 14:03:20  mmaloney
 * NoaaportConfigPanel added.
 *
 */
package lrgs.rtstat;
import ilex.gui.JobDialog;

import java.awt.GridBagLayout;
import javax.swing.JPanel;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.border.TitledBorder;
import java.awt.Font;
import java.awt.Color;
import java.awt.Point;

import javax.swing.JCheckBox;
import java.awt.GridBagConstraints;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;

import java.awt.Insets;
import java.io.IOException;
import java.util.ResourceBundle;

import javax.swing.JButton;

import decodes.gui.GuiDialog;
import decodes.gui.TopFrame;

import lrgs.lrgsmain.LrgsConfig;
import lrgs.noaaportrecv.NoaaportTestSvr;

public class NoaaportConfigPanel 
	extends JPanel
	implements LrgsConfigPanel
{
	private static final long serialVersionUID = 1L;
	private JCheckBox enableCheckBox = new JCheckBox();
	private JLabel portLabel = new JLabel();
	private JTextField portText = new JTextField();
	private JButton testButton = new JButton();
	private static final String recvTypes[] = { "Marta", "Unisys", "PDI" };
	private JComboBox recvTypeCombo = new JComboBox(recvTypes);
	private JTextField hostText = new JTextField();
	private JTextField captureFileField = new JTextField();
	
	private NoaaportTestSvr myServer=null;
	private TopFrame myParent = null;
	private GuiDialog myDialogParent = null;
	private JobDialog myDialog = null;
	
	public LrgsConfig myConfig = null;
	private static ResourceBundle genericLabels = 
		RtStat.getGenericLabels();
	private static ResourceBundle rtstatLabels =
		RtStat.getLabels();
	private int origRecvTypeIdx = 0;
	private String origHostText = null;
	String origCaptureFile = null;

	/**
	 * This is the default constructor
	 */
	public NoaaportConfigPanel(TopFrame parent) {
		super();
		myParent = parent;
		initialize();
	}
	
	public String getLabel()
	{
		return rtstatLabels.getString("LrgsConfigDialog.noaaportTab");
	}
	
	public NoaaportConfigPanel(GuiDialog parent) {
		super();
		myDialogParent=parent;
		initialize();
	}

	public void fillFields(LrgsConfig tmp)
	{
		enableCheckBox.setSelected(tmp.noaaportEnabled);
		portText.setText(String.valueOf(tmp.noaaportPort));
		if (tmp.noaaportReceiverType != null)
		{
			recvTypeCombo.setSelectedIndex(0); // default to Marta
			for (int i = 0; i<recvTypes.length; i++)
				if (tmp.noaaportReceiverType.toLowerCase().contains(
					recvTypes[i].toLowerCase()))
				{
					recvTypeCombo.setSelectedIndex(i);
					break;
				}
		}
		origRecvTypeIdx = recvTypeCombo.getSelectedIndex();
		origHostText = tmp.noaaportHostname;
		if (origHostText == null)
			origHostText = "";
		hostText.setText(origHostText);
		
		origCaptureFile = tmp.noaaportCaptureFile;
		if (origCaptureFile == null)
			origCaptureFile = "";
		captureFileField.setText(origCaptureFile);

		recvTypeSelected();
		myConfig = tmp;
	}
	
	public boolean hasChanged()
	{
		if (myConfig == null)
			return false;
		if (enableCheckBox.isSelected() != myConfig.noaaportEnabled
		 || getPort() != myConfig.noaaportPort
		 || recvTypeCombo.getSelectedIndex() != origRecvTypeIdx
		 || !hostText.getText().equals(origHostText)
		 || !captureFileField.getText().equals(origCaptureFile))
			return true;
		return false;
	}
	
	public void saveChanges()
	{
		if(myConfig == null)
			return;

		int p = getPort();
		if (p < 0)
		{
			showError("Invalid Port Format, must be an Integer!");
			return;
		}
		myConfig.noaaportPort = p;
		myConfig.noaaportEnabled = enableCheckBox.isSelected();
		myConfig.noaaportReceiverType = 
			(String)recvTypeCombo.getSelectedItem();
		myConfig.noaaportHostname = hostText.getText().trim();
		myConfig.noaaportCaptureFile = captureFileField.getText().trim();
	}

	/**
	 * 
	 * @return int returns -1 if invalid port
	 */
	public int getPort()
	{
		try { return Integer.parseInt(portText.getText().trim()); }
		catch(Exception ex)
		{
			return -1;
		}
	}
	
	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize()
	{
		enableCheckBox.setText(genericLabels.getString("enable"));
		testButton.setText(
			genericLabels.getString("test"));
		testButton.addActionListener(
			new java.awt.event.ActionListener() 
			{
				public void actionPerformed(java.awt.event.ActionEvent e) {
					testButtonPressed();
			}});
		recvTypeCombo.addActionListener(
			new java.awt.event.ActionListener() 
			{
				public void actionPerformed(java.awt.event.ActionEvent e) {
					recvTypeSelected();
			}});
			
		
		portLabel.setText(
			genericLabels.getString("port") + ":");

		
		this.setLayout(new GridBagLayout());
		
		this.setBorder(BorderFactory.createTitledBorder(null,
			"NOAAPort " + genericLabels.getString("parameters"),
			TitledBorder.CENTER, TitledBorder.BELOW_TOP, 
			new Font("Dialog", Font.BOLD, 14), new Color(51, 51, 51)));
		
		
		add(enableCheckBox, 
			new GridBagConstraints(1, 0, 1, 1, 0.0, 0.5,
				GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE,
				new Insets(5, 0, 5, 5), 0, 0));
		add(new JLabel("Receiver Type:"),
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 30, 5, 3), 0, 0));
		add(recvTypeCombo,
			new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 0, 5, 15), 10, 0));
		add(portLabel, 
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 20, 5, 3), 0, 0));
		add(portText,
			new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 0, 5, 15), 60, 0));
		add(new JLabel("Receiver Host or IP Addr:"),
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 30, 5, 3), 0, 0));
		add(hostText,
			new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 0, 5, 15), 150, 0));
		add(new JLabel("Capture File:"),
			new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 30, 5, 3), 0, 0));
		add(captureFileField,
			new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 0, 5, 15), 300, 0));
		
		add(testButton,
			new GridBagConstraints(1, 5, 1, 1, 1.0, 0.5,
				GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
				new Insets(5, 0, 5, 5), 0, 0));
	}

	private void testButtonPressed()
	{
		if(myServer!=null)
		{
			myServer.shutdown();
		}
		Integer myPortNum=null;
			
		//Get port number from text field
		try{myPortNum=Integer.parseInt(portText.getText().trim());}
		catch(Exception e)
		{
			showError("Bad Port Number");
			return;
		}
		if(myPortNum < 1)
		{
			showError("Bad Port Number");
			return;
		}
			
		//Create new dialog to launch
		if(myParent==null)
		{
			myDialog = new JobDialog(myDialogParent,"Testing Server on Port "+myPortNum,true);
		}
		else
		{
			myDialog = new JobDialog(myParent,"Testing Server on Port "+myPortNum,true);
		}
			
		//Start the server and send it the JobDialog to launch
		try {
			myServer = new NoaaportTestSvr(myPortNum.intValue(),myDialog);
		} catch (IllegalArgumentException e) {
			showError("Illegal Argument "+ e.getMessage());
			return;
		} catch (IOException e) {
			showError("IO Error "+e.getMessage());
			return;
		}
		
		Thread backgroundThread = new Thread(){
			public void run(){
				
				try{myServer.listen();}
				catch(IOException e)
				{
					myDialog.addToProgress("Error Opening Port " + e.getMessage());
					return;
				}
				myDialog.addToProgress("Finished Listening");
				myDialog.finishedJob();
			}
		};
		
		myDialog.addToProgress("Listening to port "+myPortNum);
		backgroundThread.start();
		launch(myDialog);
			
	}
	
	private void showError(String str)
	{
		if(myParent==null)
		{
			myDialogParent.showError(str);
		}
		else
		{
			myParent.showError(str);
		}
	}
	
	private void launch(JDialog dlg)
	{
		Dimension frameSize;
		Point frameLoc;
		if(myParent!=null)
		{
			frameSize = myParent.getSize();
			frameLoc = myParent.getLocation();
		}
		else
		{
			frameSize = myDialogParent.getSize();
			frameLoc = myDialogParent.getLocation();
		}
		Dimension dlgSize = dlg.getPreferredSize();
		int xo = (frameSize.width - dlgSize.width) / 2;
		if (xo < 0) xo = 0;
		int yo = (frameSize.height - dlgSize.height) / 2;
		if (yo < 0) yo = 0;
		
		dlg.setLocation(frameLoc.x + xo, frameLoc.y + yo);
		dlg.setVisible(true);
		
		myServer.shutdown();
	}
	
	private void recvTypeSelected()
	{
		String t = (String)recvTypeCombo.getSelectedItem();
		hostText.setEnabled(t.toLowerCase().contains("unisys"));
	}
}