/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:03  cvs
*  Added legacy code to repository
*
*  Revision 1.6  2004/09/08 12:24:21  mjmaloney
*  javadoc
*
*  Revision 1.5  2004/07/28 20:54:39  mjmaloney
*  dev
*
*  Revision 1.4  2004/07/28 15:32:43  mjmaloney
*  GUI Mods.
*
*  Revision 1.3  2004/07/28 15:06:15  mjmaloney
*  dev
*
*/
package decodes.platwiz;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.*;

import decodes.dbeditor.LoadMessageDialog;

/**
This panel allows the user to retrieve sample raw data for test decoding
within the wizard.

MJM - Withdrawn -- DO NOT USE THIS PANEL.

*/
public class SampleDataPanel extends JPanel
	implements WizardPanel
{
	GridLayout gridLayout1 = new GridLayout();
	JPanel jPanel1 = new JPanel();
	TitledBorder titledBorder1;
	JPanel jPanel2 = new JPanel();
	TitledBorder titledBorder2;
	JPanel jPanel3 = new JPanel();
	TitledBorder titledBorder3;
	JTextArea sampleSTArea = new JTextArea();
	JButton findSTButton = new JButton();
	JButton clearSTButton = new JButton();
	JScrollPane jScrollPane1 = new JScrollPane();
	JCheckBox wrapSTCheck = new JCheckBox();
	GridBagLayout gridBagLayout1 = new GridBagLayout();
	JScrollPane jScrollPane2 = new JScrollPane();
	JTextArea sampleRDArea = new JTextArea();
	JButton findRDButton = new JButton();
	JButton clearRDButton = new JButton();
	JCheckBox wrapRDCheck = new JCheckBox();
	GridBagLayout gridBagLayout2 = new GridBagLayout();
	JScrollPane jScrollPane3 = new JScrollPane();
	JTextArea sampleEDLArea = new JTextArea();
	JButton loadEDLButton = new JButton();
	JButton clearEDLButton = new JButton();
	JCheckBox wrapEDLCheck = new JCheckBox();
	GridBagLayout gridBagLayout3 = new GridBagLayout();

	/** Constructs the SampleDataPanel */
	public SampleDataPanel()
	{
		try {
			jbInit();
			int fsz = sampleSTArea.getFont().getSize();
			Font newfont = new Font("Monospaced", Font.PLAIN, fsz);
			sampleSTArea.setFont(newfont);
			sampleRDArea.setFont(newfont);
			sampleEDLArea.setFont(newfont);
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	/** Initializes the GUI components. */
	void jbInit() throws Exception
	{
		titledBorder1 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white,new Color(165, 163, 151)),"Sample Self Timed GOES Message");
		titledBorder2 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white,new Color(165, 163, 151)),"Sample Random GOES Message");
		titledBorder3 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white,new Color(165, 163, 151)),"Sample EDL File");
		gridLayout1.setColumns(1);
		gridLayout1.setRows(3);
		this.setLayout(gridLayout1);
		jPanel1.setBorder(titledBorder1);
		jPanel1.setLayout(gridBagLayout1);
		jPanel2.setBorder(titledBorder2);
		jPanel2.setLayout(gridBagLayout2);
		jPanel3.setBorder(titledBorder3);
		jPanel3.setLayout(gridBagLayout3);
		sampleSTArea.setBorder(null);
		findSTButton.setToolTipText("Finds a recent ST message from your LRGS & Loads into window.");
		findSTButton.setText("Find");
    findSTButton.addActionListener(new SampleDataPanel_findSTButton_actionAdapter(this));
		clearSTButton.setText("Clear");
    clearSTButton.addActionListener(new SampleDataPanel_clearSTButton_actionAdapter(this));
		wrapSTCheck.setToolTipText("Selects whether to wrap long lines in the sample window.");
		wrapSTCheck.setText("Wrap");
    wrapSTCheck.addActionListener(new SampleDataPanel_wrapSTCheck_actionAdapter(this));
		sampleRDArea.setBorder(null);
		findRDButton.setToolTipText("Finds a recent RD message from your LRGS.");
		findRDButton.setText("Find");
    findRDButton.addActionListener(new SampleDataPanel_findRDButton_actionAdapter(this));
		clearRDButton.setText("Clear");
    clearRDButton.addActionListener(new SampleDataPanel_clearRDButton_actionAdapter(this));
		wrapRDCheck.setText("Wrap");
    wrapRDCheck.addActionListener(new SampleDataPanel_wrapRDCheck_actionAdapter(this));
		loadEDLButton.setToolTipText("Loads an EDL message from a file.");
		loadEDLButton.setText("Load");
    loadEDLButton.addActionListener(new SampleDataPanel_loadEDLButton_actionAdapter(this));
		clearEDLButton.setText("Clear");
    clearEDLButton.addActionListener(new SampleDataPanel_clearEDLButton_actionAdapter(this));
		wrapEDLCheck.setText("Wrap");
    wrapEDLCheck.addActionListener(new SampleDataPanel_wrapEDLCheck_actionAdapter(this));
		this.add(jPanel1, null);
		this.add(jPanel2, null);
		jPanel2.add(jScrollPane2,	new GridBagConstraints(0, 0, 1, 3, 1.0, 1.0
						,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 13, 4, 0), 179, 102));
		jScrollPane2.getViewport().add(sampleRDArea, null);
		this.add(jPanel3, null);
		jPanel1.add(jScrollPane1,	new GridBagConstraints(0, 0, 1, 3, 1.0, 1.0
						,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 13, 8, 0), 180, 96));
		jPanel1.add(wrapSTCheck,	 new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
						,GridBagConstraints.SOUTH, GridBagConstraints.NONE, new Insets(4, 9, 4, 14), 0, 0));
		jPanel1.add(findSTButton,	new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(3, 9, 0, 14), 4, 0));
		jPanel1.add(clearSTButton,	 new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 9, 0, 14), 0, 0));
		jScrollPane1.getViewport().add(sampleSTArea, null);
		jPanel2.add(findRDButton,	new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(3, 14, 0, 12), 4, 0));
		jPanel2.add(clearRDButton,	new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(6, 14, 0, 12), 0, 0));
		jPanel2.add(wrapRDCheck,	 new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
						,GridBagConstraints.SOUTH, GridBagConstraints.NONE, new Insets(4, 14, 4, 12), 0, 0));
		jPanel3.add(jScrollPane3,	new GridBagConstraints(0, 0, 1, 3, 1.0, 1.0
						,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 12, 3, 0), 195, 104));
		jPanel3.add(clearEDLButton,	new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(7, 17, 0, 8), 0, 0));
		jPanel3.add(loadEDLButton,	new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 17, 0, 8), 0, 0));
		jPanel3.add(wrapEDLCheck,	 new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
						,GridBagConstraints.SOUTH, GridBagConstraints.NONE, new Insets(4, 16, 4, 8), 0, 0));
		jScrollPane3.getViewport().add(sampleEDLArea, null);
	}

	// ------- From Wizard Panel Interface -------

	/** Called once at start-up. */
	public void initialize()
		throws PanelException
	{
	}

	/** @returns panel title */
	public String getPanelTitle()
	{
		return "Sample Platform Data";
	}

	/** @returns description to display in frame */
	public String getDescription()
	{
		return "In this panel you can put sample messages from the platform."
			+ " Subsequent panels will attempt to analyze the messages to "
			+ "suggest format statements for decoding.";
	}

	/** @returns true if this panel should be skipped */
	public boolean shouldSkip() { return false; }

	/** Called when panel is being activated */
	public void activate()
		throws PanelException
	{
		PlatformWizard pw = PlatformWizard.instance();
		boolean enabled = pw.processGoesST();
		sampleSTArea.setEnabled(enabled);
		findSTButton.setEnabled(enabled);
		clearSTButton.setEnabled(enabled);
		wrapSTCheck.setEnabled(enabled);

		enabled = pw.processGoesRD();
		sampleRDArea.setEnabled(enabled);
		findRDButton.setEnabled(enabled);
		clearRDButton.setEnabled(enabled);
		wrapRDCheck.setEnabled(enabled);

		enabled = pw.processEDL();
		sampleEDLArea.setEnabled(enabled);
		loadEDLButton.setEnabled(enabled);
		clearEDLButton.setEnabled(enabled);
		wrapEDLCheck.setEnabled(enabled);
	}

	/** Called when panel is being deactivated */
	public boolean deactivate()
		throws PanelException
	{
		return true;
	}

	/** Called when app is shutting down */
	public void shutdown()
	{
	}

	/**
	  Called when Self-Timed Find button is pressed. Search for message
	  from specified DCP & populate the window.
	*/
	void findSTButton_actionPerformed(ActionEvent e) 
	{
		PlatformWizard pw = PlatformWizard.instance();
		LoadMessageDialog dlg = new LoadMessageDialog();
		dlg.setDcpAddress(pw.getDcpAddress());
		dlg.setGoesChannel(pw.getSTChannel());
		dlg.enableGoes(true);
		dlg.setTargetArea(sampleSTArea);

		PlatformWizard.instance().launchDialog(dlg);
	}

	/**
	  Called when Self-Timed Clear button is pressed.
	  Clears the text area.
	*/
	void clearSTButton_actionPerformed(ActionEvent e) 
	{
		sampleSTArea.setText("");
	}

	/**
	  Called when self-timed wrap check is clicked.
	  Set wrap attribute on text area.
	*/
	void wrapSTCheck_actionPerformed(ActionEvent e) 
	{
		sampleSTArea.setLineWrap(wrapSTCheck.isSelected());
	}

	/**
	  Called when Random Find button is pressed. Search for message
	  from specified DCP & populate the window.
	*/
	void findRDButton_actionPerformed(ActionEvent e) 
	{
		PlatformWizard pw = PlatformWizard.instance();
		LoadMessageDialog dlg = new LoadMessageDialog();
		dlg.setDcpAddress(pw.getDcpAddress());
		dlg.setGoesChannel(pw.getRDChannel());
		dlg.enableGoes(true);
		dlg.setTargetArea(sampleRDArea);

		PlatformWizard.instance().launchDialog(dlg);
	}

	/**
	  Called when Random Clear button is pressed.
	  Clears the text area.
	*/
	void clearRDButton_actionPerformed(ActionEvent e) 
	{
		sampleRDArea.setText("");
	}

	/**
	  Called when random wrap check is clicked.
	  Set wrap attribute on text area.
	*/
	void wrapRDCheck_actionPerformed(ActionEvent e) 
	{
		sampleRDArea.setLineWrap(wrapRDCheck.isSelected());
	}

	/**
	  Called when EDL Load button is pressed. 
	  Display file open dialog for user to select file.
	*/
	void loadEDLButton_actionPerformed(ActionEvent e) 
	{
		PlatformWizard pw = PlatformWizard.instance();
		LoadMessageDialog dlg = new LoadMessageDialog();
		dlg.enableGoes(false);
		dlg.setTargetArea(sampleEDLArea);
		PlatformWizard.instance().launchDialog(dlg);
	}

	/**
	  Called when EDL Clear button is pressed.
	  Clears the text area.
	*/
	void clearEDLButton_actionPerformed(ActionEvent e) 
	{
		sampleEDLArea.setText("");
	}

	/**
	  Called when EDL wrap check is clicked.
	  Set wrap attribute on text area.
	*/
	void wrapEDLCheck_actionPerformed(ActionEvent e) 
	{
		sampleEDLArea.setLineWrap(wrapEDLCheck.isSelected());
	}

}

class SampleDataPanel_findSTButton_actionAdapter implements java.awt.event.ActionListener {
  SampleDataPanel adaptee;

  SampleDataPanel_findSTButton_actionAdapter(SampleDataPanel adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.findSTButton_actionPerformed(e);
  }
}

class SampleDataPanel_clearSTButton_actionAdapter implements java.awt.event.ActionListener {
  SampleDataPanel adaptee;

  SampleDataPanel_clearSTButton_actionAdapter(SampleDataPanel adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.clearSTButton_actionPerformed(e);
  }
}

class SampleDataPanel_wrapSTCheck_actionAdapter implements java.awt.event.ActionListener {
  SampleDataPanel adaptee;

  SampleDataPanel_wrapSTCheck_actionAdapter(SampleDataPanel adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.wrapSTCheck_actionPerformed(e);
  }
}

class SampleDataPanel_findRDButton_actionAdapter implements java.awt.event.ActionListener {
  SampleDataPanel adaptee;

  SampleDataPanel_findRDButton_actionAdapter(SampleDataPanel adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.findRDButton_actionPerformed(e);
  }
}

class SampleDataPanel_clearRDButton_actionAdapter implements java.awt.event.ActionListener {
  SampleDataPanel adaptee;

  SampleDataPanel_clearRDButton_actionAdapter(SampleDataPanel adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.clearRDButton_actionPerformed(e);
  }
}

class SampleDataPanel_wrapRDCheck_actionAdapter implements java.awt.event.ActionListener {
  SampleDataPanel adaptee;

  SampleDataPanel_wrapRDCheck_actionAdapter(SampleDataPanel adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.wrapRDCheck_actionPerformed(e);
  }
}

class SampleDataPanel_loadEDLButton_actionAdapter implements java.awt.event.ActionListener {
  SampleDataPanel adaptee;

  SampleDataPanel_loadEDLButton_actionAdapter(SampleDataPanel adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.loadEDLButton_actionPerformed(e);
  }
}

class SampleDataPanel_clearEDLButton_actionAdapter implements java.awt.event.ActionListener {
  SampleDataPanel adaptee;

  SampleDataPanel_clearEDLButton_actionAdapter(SampleDataPanel adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
System.out.println("Clearing EDL area");
    adaptee.clearEDLButton_actionPerformed(e);
  }
}

class SampleDataPanel_wrapEDLCheck_actionAdapter implements java.awt.event.ActionListener {
  SampleDataPanel adaptee;

  SampleDataPanel_wrapEDLCheck_actionAdapter(SampleDataPanel adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.wrapEDLCheck_actionPerformed(e);
  }
}
