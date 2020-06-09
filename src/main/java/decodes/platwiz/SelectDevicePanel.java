package decodes.platwiz;

import ilex.util.LoadResourceBundle;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.*;
import java.util.ResourceBundle;

import decodes.dbeditor.EquipmentEditPanel;
import decodes.dbeditor.EquipmentModelSelectDialog;
import decodes.db.Database;
import decodes.db.Platform;
import decodes.db.PlatformConfig;
import decodes.db.EquipmentModel;
import decodes.gui.TopFrame;

/**
This panel allows the user to select and edit the equipment model.
This is a thin layer around decodes.dbeditor.EquipmentEditPanel.
*/
public class SelectDevicePanel extends JPanel
	implements WizardPanel
{
	private static ResourceBundle genericLabels = 
		PlatformWizard.getGenericLabels();
	private static ResourceBundle platwizLabels = 
		PlatformWizard.getPlatwizLabels();
	JLabel jLabel1 = new JLabel();
	TitledBorder titledBorder1;
	TitledBorder titledBorder2;
	BorderLayout borderLayout1 = new BorderLayout();
	JPanel jPanel2 = new JPanel();
	EquipmentEditPanel equipmentEditPanel = new EquipmentEditPanel();
	FlowLayout flowLayout1 = new FlowLayout();
	JTextField equipmentNameField = new JTextField();
	JButton equipmentSelectButton = new JButton();
	JButton newButton = new JButton();

	/** Constructs new SiteDevicePanel */
	public SelectDevicePanel()
	{
		try {
			jbInit();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	/** Initializes components. */
	void jbInit() throws Exception {
		titledBorder1 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white,new Color(165, 163, 151)),"Select Equipment & Configuration");
		titledBorder2 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white,new Color(165, 163, 151)),"Decoding Scripts for Message Types");
		jLabel1.setText(platwizLabels.getString("SelectDevicePanel.selectEquip")
				+":");
		this.setLayout(borderLayout1);
		jPanel2.setLayout(flowLayout1);
		equipmentNameField.setPreferredSize(new Dimension(150, 20));
		equipmentNameField.setEditable(false);
		equipmentNameField.setText("");
//		equipmentSelectButton.setPreferredSize(new Dimension(85, 23));
		equipmentSelectButton.setText(genericLabels.getString("select"));
		equipmentSelectButton.addActionListener(new SelectDevicePanel_equipmentSelectButton_actionAdapter(this));
		newButton.setPreferredSize(new Dimension(85, 23));
		newButton.setText(genericLabels.getString("new"));
		newButton.addActionListener(new SelectDevicePanel_newButton_actionAdapter(this));
		this.add(jPanel2, BorderLayout.NORTH);
		jPanel2.add(jLabel1, null);
		jPanel2.add(equipmentNameField, null);
		this.add(equipmentEditPanel, BorderLayout.CENTER);
		jPanel2.add(equipmentSelectButton, null);
		jPanel2.add(newButton, null);
	}

	// ------- From Wizard Panel Interface -------

	/** Called once at start-up. */
	public void initialize()
		throws PanelException
	{
	}

	/** @returns "Select Equipment Model" */
	public String getPanelTitle()
	{
		return platwizLabels.getString("SelectDevicePanel.selectEquip");
	}

	/** @returns description to be shown at top of page. */
	public String getDescription()
	{
		return
		platwizLabels.getString("SelectDevicePanel.description");
	}

	/** Always false -- never skip this panel. */
	public boolean shouldSkip() { return false; }

	/**
		Called when panel is activated.
		Populates GUI components with values in the EquipmentModel object.
	*/
	public void activate()
		throws PanelException
	{
		Platform p = PlatformWizard.instance().getPlatform();
		PlatformConfig pc = p.getConfig();
		EquipmentModel em = pc == null ? null : pc.equipmentModel;
		equipmentEditPanel.setEquipmentModel(em);
		equipmentNameField.setText(em == null ? "" : em.name);
	}

	/**
		Called when panel is done.
		Saves info from GUI components back to EquipmentModel object.
	*/
	public boolean deactivate()
		throws PanelException
	{
		Platform p = PlatformWizard.instance().getPlatform();
		PlatformConfig pc = p.getConfig();

		if (pc != null)
		{
			// Get the internal copy inside the edit panel.
			// Set this in the platform config.
			pc.equipmentModel = equipmentEditPanel.getDataFromFields();
		}
		return true;
	}

	/** Called when app is exiting */
	public void shutdown()
	{
	}

	/**
		Called when 'Select' button is pressed.
		Displays dialog of equipment model objects in my database.
	*/
	void equipmentSelectButton_actionPerformed(ActionEvent e)
	{
		EquipmentModelSelectDialog dlg = new EquipmentModelSelectDialog();
		Platform p = PlatformWizard.instance().getPlatform();
		PlatformConfig pc = p.getConfig();
		if (pc == null)
		{
			TopFrame.instance().showError(
				platwizLabels.getString("SelectDevicePanel.createEquipModErr"));
			return;
		}

		EquipmentModel em = pc.equipmentModel;

		if (em != null)
			dlg.setSelection(em);
		TopFrame.instance().launchDialog(dlg);
		em = dlg.getSelectedEquipmentModel();
		if (em != null && pc != null)
		{
			pc.equipmentModel = em;
			try { activate(); }
			catch(PanelException ex)
			{
				TopFrame.instance().showError(ex.toString());
			}
		}
	}

	void newButton_actionPerformed(ActionEvent e)
	{
		Platform p = PlatformWizard.instance().getPlatform();
		PlatformConfig pc = p.getConfig();
		if (pc == null)
		{
			TopFrame.instance().showError(
			platwizLabels.getString("SelectDevicePanel.createEquipModErr"));
			return;
		}
		String nm = JOptionPane.showInputDialog(TopFrame.instance(),
			platwizLabels.getString("SelectDevicePanel.enterUniqueEquipMod"));
		if (nm != null)
		{
			if (Database.getDb().equipmentModelList.get(nm) != null)
			{
				TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
				platwizLabels.getString("SelectDevicePanel.equipModErr"),
				nm));
				return;
			}
			pc.equipmentModel = new EquipmentModel(nm);
			try { activate(); }
			catch(PanelException ex)
			{
				TopFrame.instance().showError(ex.toString());
			}
		}
	}
}

class SelectDevicePanel_equipmentSelectButton_actionAdapter implements java.awt.event.ActionListener {
	SelectDevicePanel adaptee;

	SelectDevicePanel_equipmentSelectButton_actionAdapter(SelectDevicePanel adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.equipmentSelectButton_actionPerformed(e);
	}
}

class SelectDevicePanel_newButton_actionAdapter implements java.awt.event.ActionListener {
	SelectDevicePanel adaptee;

	SelectDevicePanel_newButton_actionAdapter(SelectDevicePanel adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.newButton_actionPerformed(e);
	}
}
