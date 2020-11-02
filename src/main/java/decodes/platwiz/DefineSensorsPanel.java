/*
*  $Id$
*/
package decodes.platwiz;

import ilex.util.LoadResourceBundle;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.*;
import java.util.ResourceBundle;
import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.DecodesScript;
import decodes.db.Platform;
import decodes.db.PlatformConfig;
import decodes.db.EquipmentModel;
import decodes.db.TransportMedium;
import decodes.db.DatabaseException;
import decodes.dbeditor.ConfigEditPanel;
import decodes.dbeditor.ConfigSelectDialog;
import decodes.dbeditor.ConfigSelectController;
import decodes.dbeditor.EquipmentModelSelectDialog;
import decodes.gui.TopFrame;
import decodes.util.DecodesSettings;

/** Panel for defining the sensors and selecting the config. */
public class DefineSensorsPanel extends JPanel
		implements WizardPanel

{
	private static ResourceBundle genericLabels = 
		PlatformWizard.getGenericLabels();
	private static ResourceBundle platwizLabels = 
		PlatformWizard.getPlatwizLabels();
//	static { ConfigEditPanel.AddDecodingScriptsPanel = false; }
	ConfigEditPanel configEditPanel = new ConfigEditPanel(false);
	JLabel jLabel1 = new JLabel();
	JPanel jPanel4 = new JPanel();
	BorderLayout borderLayout1 = new BorderLayout();
	FlowLayout flowLayout1 = new FlowLayout();
	JButton selectButton = new JButton();
	JButton newButton = new JButton();
	JTextField configNameField = new JTextField();

	/** Constructs new DefineSensorsPanel */
	public DefineSensorsPanel() {
		try {
			jbInit();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	/** Initializes GUI components */
	void jbInit() throws Exception {
		this.setLayout(borderLayout1);
		configEditPanel.setBorder(null);
		configEditPanel.setMinimumSize(new Dimension(138, 260));
		configEditPanel.setPreferredSize(new Dimension(510, 260));
		jLabel1.setText(
		platwizLabels.getString("DefineSensorsPanel.platformConfig"));
		jPanel4.setLayout(flowLayout1);
//		selectButton.setPreferredSize(new Dimension(85, 23));
		selectButton.setText(genericLabels.getString("select"));
		selectButton.addActionListener(new DefineSensorsPanel_selectButton_actionAdapter(this));
		newButton.setPreferredSize(new Dimension(85, 23));
		newButton.setText(genericLabels.getString("new"));
		newButton.addActionListener(new DefineSensorsPanel_newButton_actionAdapter(this));
		configNameField.setPreferredSize(new Dimension(150, 20));
		configNameField.setEditable(false);
		configNameField.setText("");
		this.add(configEditPanel, BorderLayout.CENTER);
		this.add(jPanel4, BorderLayout.NORTH);
		jPanel4.add(jLabel1, null);
		jPanel4.add(configNameField, null);
		jPanel4.add(selectButton, null);
		jPanel4.add(newButton, null);
	}

	// ------- From Wizard Panel Interface -------

	/** Called once at start-up. */
	public void initialize()
		throws PanelException
	{
	}

	/** Returns panel title */
	public String getPanelTitle()
	{
		return platwizLabels.getString("DefineSensorsPanel.definePlatSensors");
	}

	/** Returns description for this panel */
	public String getDescription()
	{
		return platwizLabels.getString("DefineSensorsPanel.description");
	}

	/** Always return false -- never skip this panel. */
	public boolean shouldSkip() { return false; }

	/** Called when panel is activated. */
	public void activate()
		throws PanelException
	{
		Platform p = PlatformWizard.instance().getPlatform();
		PlatformConfig pc = p.getConfig();
		configEditPanel.setConfig(pc);
		configNameField.setText(pc == null ? "" : pc.configName);
	}

	/** Called when panel is deactivated. */
	public boolean deactivate()
		throws PanelException
	{
		if (configEditPanel.getConfig() == null)
		{
			JOptionPane.showMessageDialog(PlatformWizard.instance().getFrame(),
			platwizLabels.getString("DefineSensorsPanel.selectConfigErr"));
			return false;
		}
		configEditPanel.validateDecodingScriptSensors();
		Platform p = PlatformWizard.instance().getPlatform();
		PlatformConfig pc = configEditPanel.getDataFromFields();
		p.setConfig(pc);
		return true;
	}

	/** Called when application exits. */
	public void shutdown()
	{
	}

	/** 
	  Called when 'Select' button pressed. 
	  @param e ignored
	*/
	void selectButton_actionPerformed(ActionEvent e) 
	{
		Platform p = PlatformWizard.instance().getPlatform();
		PlatformConfig pc = p.getConfig();
		
		ConfigSelectDialog dlg = 
			new ConfigSelectDialog(PlatformWizard.instance().getFrame(),
				(ConfigSelectController)null);
		if (pc != null)
			dlg.setSelection(pc.getName());
		PlatformWizard.instance().getFrame().launchDialog(dlg);
		pc = dlg.getSelectedConfig();
		
		if (pc == null)
		{
//System.out.println("test pc is null");
			return;
		}
		try { pc.read(); }
		catch(DatabaseException ex)
		{
			PlatformWizard.instance().getFrame().showError(
				platwizLabels.getString("DefineSensorsPanel.readConfigErr")
				+ ex.toString());
		}
		p.setConfig(pc);
		p.setConfigName(p.getConfigName());
		configNameField.setText(pc.getName());
		if (PlatformWizard.instance().processEDL())
		{
			EquipmentModel em = pc.equipmentModel;
			setTransportMedium(p, pc, em);
		}
		try { activate(); }
		catch(PanelException ex)
		{
			PlatformWizard.instance().getFrame().showError(ex.toString());
		}
	}

	/** Called when 'New' button pressed.
	  @param e ignored
	*/
	void newButton_actionPerformed(ActionEvent e) 
	{
		DecodesSettings settings = DecodesSettings.instance();
		PlatformConfig pc = null;
		EquipmentModel em = null;
		String originator = settings.decodesConfigOwner;
		String nm = JOptionPane.showInputDialog(
			PlatformWizard.instance().getFrame(),
			platwizLabels.getString("DefineSensorsPanel.enterUniqueConfName"));
		if (nm == null)
			return;
		if ( originator == null || originator.trim().length() == 0 )
		{
			if (Database.getDb().platformConfigList.get(nm) != null)
			{
				PlatformWizard.instance().getFrame().showError(
				LoadResourceBundle.sprintf(
				platwizLabels.getString("DefineSensorsPanel.configNameErr"), 
				nm));
				return;
			}
			pc = new PlatformConfig(nm);
		} else {
			originator=originator.trim();
			EquipmentModelSelectDialog dlg = new EquipmentModelSelectDialog();
			TopFrame.instance().launchDialog(dlg);
			em = dlg.getSelectedEquipmentModel();
			if ( em == null )
				return;
			String modelName = em.getName();
			if ( modelName != null ) {
				modelName = modelName.trim();
				try {
					pc = Database.getDb().getDbIo().newPlatformConfig(pc, modelName, originator);
				} catch ( Exception ex) {
					PlatformWizard.instance().getFrame().showError(ex.toString());
				}
			}
			if ( pc == null ) {
				pc = new PlatformConfig("Unknown");
			}
		}
		pc.equipmentModel = em;
		Platform p = PlatformWizard.instance().getPlatform();
		configNameField.setText(pc.getName());
		p.setConfig(pc);
		if (PlatformWizard.instance().processGoesST())
			pc.addScript(new DecodesScript(pc, "ST"));
		if (PlatformWizard.instance().processGoesRD())
			pc.addScript(new DecodesScript(pc, "RD"));
		if (PlatformWizard.instance().processEDL()) 
			setTransportMedium(p, pc, em);
		try { activate(); }
		catch(PanelException ex)
		{
			PlatformWizard.instance().getFrame().showError(ex.toString());
		}
	}

	void setTransportMedium (Platform p, PlatformConfig pc, EquipmentModel em)
	{
//	    pc.addScript(new DecodesScript(pc, "EDL"));
		String siteId = p.getSiteName(false);
		if ( em != null && siteId != null )
		{
			String mediumId = siteId+"-"+em.getName()+"-1";
			TransportMedium tm=p.getTransportMedium(Constants.medium_EDL);
			int tmIndex = p.transportMedia.indexOf(tm); 
			if (tm == null)
			{
				tm = new TransportMedium(p,
				Constants.medium_EDL, mediumId);
				tm.scriptName = "EDL";
				p.transportMedia.add(tm);	
			} else
			{
				tm.setMediumId(mediumId);
				//Update the transportMedia
				if (tmIndex != -1)
					p.transportMedia.set(tmIndex, tm);
			}
		}
	}
}

class DefineSensorsPanel_selectButton_actionAdapter implements java.awt.event.ActionListener {
	DefineSensorsPanel adaptee;

	DefineSensorsPanel_selectButton_actionAdapter(DefineSensorsPanel adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.selectButton_actionPerformed(e);
	}
}

class DefineSensorsPanel_newButton_actionAdapter implements java.awt.event.ActionListener {
	DefineSensorsPanel adaptee;

	DefineSensorsPanel_newButton_actionAdapter(DefineSensorsPanel adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.newButton_actionPerformed(e);
	}
}
