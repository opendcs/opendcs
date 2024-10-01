package decodes.platwiz;

import ilex.util.LoadResourceBundle;

import java.io.File;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.*;

import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.DecodesScript;
import decodes.db.EquipmentModel;
import decodes.db.Platform;
import decodes.db.PlatformConfig;
import decodes.db.Site;
import decodes.db.TransportMedium;
import decodes.gui.TopFrame;
import decodes.util.DecodesException;
import decodes.xml.TopLevelParser;

/**
This class implements the final 'save' panel in the platform wizard.
*/
public class SavePanel extends JPanel
	implements WizardPanel
{
	private static ResourceBundle genericLabels = 
		PlatformWizard.getGenericLabels();
	private static ResourceBundle platwizLabels = 
		PlatformWizard.getPlatwizLabels();
	JPanel jPanel1 = new JPanel();
	TitledBorder titledBorder1;
	JPanel jPanel2 = new JPanel();
	TitledBorder titledBorder2;
	JButton writeToDbButton = new JButton();
	JButton writeToXmlFileButton = new JButton();
	GridBagLayout gridBagLayout1 = new GridBagLayout();
	BorderLayout borderLayout1 = new BorderLayout();
	JScrollPane jScrollPane1 = new JScrollPane();
	JTextArea summaryArea = new JTextArea();
	GridBagLayout gridBagLayout2 = new GridBagLayout();
	JButton validateButton = new JButton();
	JFileChooser jFileChooser = new JFileChooser();


	/** Default constructor. */
	public SavePanel() 
	{
		try 
		{
			jbInit();
			jFileChooser.setCurrentDirectory(
				new File(System.getProperty("user.dir")));
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	/** Initialize GUI components. */
	void jbInit() throws Exception 
	{
		titledBorder1 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white,new Color(165, 163, 151)),
				platwizLabels.getString("SavePanel.summaryActions"));
		titledBorder2 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white,new Color(165, 163, 151)),
				platwizLabels.getString("SavePanel.savingWork"));
		this.setLayout(gridBagLayout2);
		jPanel1.setBorder(titledBorder1);
		jPanel1.setLayout(borderLayout1);
		jPanel2.setBorder(titledBorder2);
		jPanel2.setLayout(gridBagLayout1);
//		writeToDbButton.setPreferredSize(new Dimension(250, 23));
		writeToDbButton.setText(platwizLabels.getString("SavePanel.writeToDB"));
		writeToDbButton.addActionListener(new SavePanel_writeToDbButton_actionAdapter(this));
//		writeToXmlFileButton.setPreferredSize(new Dimension(250, 23));
		writeToXmlFileButton.setText(platwizLabels.getString("SavePanel.writeToXML"));
		writeToXmlFileButton.addActionListener(new SavePanel_writeToXmlFileButton_actionAdapter(this));
		summaryArea.setEditable(false);
		summaryArea.setText("");
//		validateButton.setPreferredSize(new Dimension(250, 23));
		validateButton.setText(
				platwizLabels.getString("SavePanel.validatePlatform"));
    validateButton.addActionListener(new SavePanel_validateButton_actionAdapter(this));
    this.add(jPanel1,	new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
						,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(11, 6, 0, 6), 520, 311));
		this.add(jPanel2,	 new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(9, 6, 13, 6), 0, 0));
		jPanel2.add(writeToDbButton,		   new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(4, 0, 4, 0), 0, 0));
		jPanel2.add(writeToXmlFileButton,		   new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(4, 0, 8, 0), 0, 0));
    jPanel2.add(validateButton,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(4, 0, 4, 0), 0, 0));
		jPanel1.add(jScrollPane1, BorderLayout.CENTER);
		jScrollPane1.getViewport().add(summaryArea, null);
	}

	/**
	  Called when write to editable database button is pressed.
	  @param e ignored
	*/
	void writeToDbButton_actionPerformed(ActionEvent e)
	{
		Platform p = PlatformWizard.instance().getPlatform();
		PlatformConfig pc = p.getConfig();
		try
		{
			pc.write();
			EquipmentModel em = pc.equipmentModel;
			if (em != null)
				em.write();
			Site site = p.getSite();
			site.write();
			p.write();
			Database.getDb().platformList.add(p);
			Database.getDb().platformList.write();
			summaryArea.append(
					platwizLabels.getString("SavePanel.infoSaveSucc"));
			PlatformWizard.instance().saved = true;
		}
		catch(DecodesException ex)
		{
			TopFrame.instance().showError(
			platwizLabels.getString("SavePanel.couldNotWriteDB") + ex);
		}
	}

	/**
	  Called when write to xml file button is pressed.
	  @param e ignored
	*/
	void writeToXmlFileButton_actionPerformed(ActionEvent e)
	{
        if (jFileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
			return;
		File output = jFileChooser.getSelectedFile();

		Database theDb = Database.getDb();
		try
		{
			Platform p = PlatformWizard.instance().getPlatform();
			Database newDb = new decodes.db.Database();
			newDb.platformList.add(p);
        	Database.setDb(newDb);
        	TopLevelParser.write(output, newDb);
			summaryArea.append(
					LoadResourceBundle.sprintf(
					platwizLabels.getString("SavePanel.platWrittenTo"),
					output.getPath()));
			PlatformWizard.instance().saved = true;
		}
		catch(Exception ex)
		{
			TopFrame.instance().showError(
					LoadResourceBundle.sprintf(
					platwizLabels.getString
					("SavePanel.cannotWriteErr"),
					output.getPath()) + ex);
		}
		finally
		{
        	Database.setDb(theDb);
		}
	}


	/**
	  Called when validate button is pressed.
	  @param e ignored
	*/
	void validateButton_actionPerformed(ActionEvent e) 
	{
		validatePlatform();
	}

	/** Called once at start-up. */
	public void initialize()
		throws PanelException
	{
	}

	/**
	* A short (couple word) title to be displayed at the top of the panel.
	*/
	public String getPanelTitle()
	{
		return platwizLabels.getString("SavePanel.saveWork");
	}

	/**
	* A 3-line description area is at the top of each panel. This method should
	* return a description of the current panel.
	* @return string
	*/
	public String getDescription()
	{
		return platwizLabels.getString("SavePanel.description");
	}


	/**
	* In some contexts, certain panels should be skipped. By default this
	* method returns false. Implementation should override & check current
	* status, & return a value if appropriate.
	* @return boolean
	*/
	public boolean shouldSkip()
	{
		return false;
	}

	/**
	* Called just prior to making this panel visible.
	* Implementation should read info in the object & populate fields in the
	* panel.
	*/
	public void activate()
		throws PanelException
	{
		writeToDbButton.setEnabled(false);
		writeToXmlFileButton.setEnabled(false);
	}

	/**
	* Called just prior to making this panel invisible.
	* Implementation should get info the controls and populate the db
	* objects.
	* @return true if OK to deactivate & move-on.
	*/
	public boolean deactivate()
		throws PanelException
	{
		PlatformWizard.instance().saved = false;
		return true;
	}

	/** Called just prior to application shutting down. */
	public void shutdown()
	{
	}

	/**
	  Validates platform information prior to saving.
	  Any errors shown in a dialog.
	  @return true if OK to proceed with save.
	*/
	private void validatePlatform()
	{
		PlatformWizard platwiz = PlatformWizard.instance();
		summaryArea.setText("");
		summaryArea.append(
				platwizLabels.getString("SavePanel.validatingPlatInfo"));

		Platform p = platwiz.getPlatform();
		boolean ok = true;
		Site site = p.getSite();
		if (site == null)
		{
			ok = false;
			summaryArea.append(
				platwizLabels.getString("SavePanel.noSiteAssignErr"));
		}
		else if (site.getPreferredName() == null)
		{
			ok = false;
			summaryArea.append(
					platwizLabels.getString("SavePanel.siteAtLeast1NameErr"));
		}
		PlatformConfig pc = p.getConfig();
		if (pc == null)
		{
			ok = false;
			summaryArea.append(
				platwizLabels.getString("SavePanel.noConfigErr"));
		}
		else if (pc.getNumScripts() == 0)
		{
			ok = false;
			summaryArea.append(
					platwizLabels.getString("SavePanel.noPlatTypeErr"));
		}
		else
		{
			EquipmentModel em = pc.equipmentModel;
			if (em == null)
				summaryArea.append(
					platwizLabels.getString("SavePanel.noEquipModelErr"));

			if (platwiz.processGoesST())
			{
				DecodesScript ds = (DecodesScript)pc.getScript("ST");
				if (ds != null
				 && ds.getFormatStatements().size() == 0)
					summaryArea.append(LoadResourceBundle.sprintf(
						platwizLabels.getString("SavePanel.noScriptFSErr"),
						ds.scriptName));
			}
			if (platwiz.processGoesRD())
			{
				DecodesScript ds = (DecodesScript)pc.getScript("RD");
				if (ds != null
				 && ds.getFormatStatements().size() == 0)
					summaryArea.append(LoadResourceBundle.sprintf(
							platwizLabels.getString("SavePanel.noScriptFSErr"),
							ds.scriptName));
			}
			if (platwiz.processEDL())
			{
				DecodesScript ds = (DecodesScript)pc.getScript("EDL");
				if (ds != null
				 && ds.getFormatStatements().size() == 0)
					summaryArea.append(LoadResourceBundle.sprintf(
							platwizLabels.getString("SavePanel.noScriptFSErr"),
							ds.scriptName));
			}
		}

		if (p.transportMedia.size() == 0)
		{
			ok = false;
			summaryArea.append(
					platwizLabels.getString("SavePanel.noTransportMediaErr"));
		}
		for(Iterator it = p.transportMedia.iterator(); it.hasNext(); )
		{
			TransportMedium tm = (TransportMedium)it.next();
			String type = tm.getMediumType();
			if ((type.equalsIgnoreCase(Constants.medium_GoesST)
			 || type.equalsIgnoreCase(Constants.medium_GoesRD))
				&& tm.channelNum <= 0)
			{
				ok = false;
				summaryArea.append(LoadResourceBundle.sprintf(
				platwizLabels.getString("SavePanel.noTransportMediaChanErr"),
				type));
			}
			if (tm.scriptName.trim().length() == 0)
			{
				ok = false;
				summaryArea.append(LoadResourceBundle.sprintf(
				platwizLabels.getString("SavePanel.noTransportMediaScriptErr"),
				type));
			}
		}
		if (ok)
		{
			summaryArea.append(
			platwizLabels.getString("SavePanel.platformOK"));
			writeToDbButton.setEnabled(true);
			writeToXmlFileButton.setEnabled(true);
		}
		else
			summaryArea.append(
				platwizLabels.getString("SavePanel.platformNotOK"));
	}
}

class SavePanel_writeToDbButton_actionAdapter implements java.awt.event.ActionListener {
	SavePanel adaptee;

	SavePanel_writeToDbButton_actionAdapter(SavePanel adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.writeToDbButton_actionPerformed(e);
	}
}

class SavePanel_writeToXmlFileButton_actionAdapter implements java.awt.event.ActionListener {
	SavePanel adaptee;

	SavePanel_writeToXmlFileButton_actionAdapter(SavePanel adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.writeToXmlFileButton_actionPerformed(e);
	}
}

class SavePanel_validateButton_actionAdapter implements java.awt.event.ActionListener {
  SavePanel adaptee;

  SavePanel_validateButton_actionAdapter(SavePanel adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.validateButton_actionPerformed(e);
  }
}
