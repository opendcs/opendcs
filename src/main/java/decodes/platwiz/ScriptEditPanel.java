package decodes.platwiz;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.Iterator;


import decodes.db.Constants;
import decodes.db.Platform;
import decodes.db.PlatformConfig;
import decodes.db.PlatformSensor;
import decodes.db.ConfigSensor;
import decodes.db.DecodesScript;
import decodes.db.DecodesScriptException;
import decodes.db.ScriptSensor;
import decodes.db.TransportMedium;
import decodes.dbeditor.DecodesScriptEditPanel;
import decodes.dbeditor.LoadMessageDialog;
import decodes.dbeditor.TraceDialog;
import decodes.gui.TopFrame;

import ilex.util.LoadResourceBundle;

/**
WiizardPanel for editing a script.
This is a thin layer around decodes.dbeditor.DecodingScriptEditPanel.
*/
public class ScriptEditPanel extends JPanel
	implements WizardPanel
{
	private static ResourceBundle genericLabels = 
		PlatformWizard.getGenericLabels();
	private static ResourceBundle platwizLabels = 
		PlatformWizard.getPlatwizLabels();
	BorderLayout borderLayout1 = new BorderLayout();
	DecodesScriptEditPanel decodingScriptEditPanel 
		= new DecodesScriptEditPanel();

	/** The name of this script */
	String name;

	/** A description for the script */
	String type;

	/** Construct new ScriptEditPanel */
	public ScriptEditPanel() 
	{
		name = type = null;
		try {
			jbInit();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		decodingScriptEditPanel.setTraceDialog(
			new TraceDialog(TopFrame.instance(), true));
	}

	/** 
	  Sets name and type 
	  @param name the script name
	  @param type the script type
	*/
	public void setNameType(String name, String type)
	{
		this.name = name;
		this.type = type;
	}

	/** Initializes GUI components */
	void jbInit() throws Exception 
	{
		this.setLayout(borderLayout1);
		this.add(decodingScriptEditPanel, BorderLayout.CENTER);
	}

	// ------- From Wizard Panel Interface -------

	/** Called once at start-up. */
	public void initialize()
		throws PanelException
	{
	}

	/** @return title for this panel. */
	public String getPanelTitle()
	{
		return LoadResourceBundle.sprintf(
		platwizLabels.getString("ScriptEditPanel.decodingScriptMsg"),
		type);
	}

	/** @return description for this panel. */
	public String getDescription()
	{
		return LoadResourceBundle.sprintf(
		platwizLabels.getString("ScriptEditPanel.description"),
		type);
	}

	/** 
	  The script edit panels will only be displayed if this script type
	  has been selected on the start panel.
	  @return true if this script type was selected in the StartPanel.
	*/
	public boolean shouldSkip() 
	{
		if (name.equals("ST"))
			return !PlatformWizard.instance().processGoesST();
		else if (name.equals("RD"))
			return !PlatformWizard.instance().processGoesRD();
		else if (name.equals("EDL"))
			return !PlatformWizard.instance().processEDL();
		return false;
	}

	/** Called when this panel is activated. */
	public void activate()
		throws PanelException
	{
		PlatformWizard platwiz = PlatformWizard.instance();

		String addr = platwiz.getDcpAddress();
		if (addr != null)
		{
			LoadMessageDialog.addDcpAddress(addr);
			if (name.equals("ST"))
				LoadMessageDialog.setGoesChannel(platwiz.getSTChannel());
			else if (name.equals("RD"))
				LoadMessageDialog.setGoesChannel(platwiz.getRDChannel());
		}

		Platform p = platwiz.getPlatform();
		PlatformConfig pc = p.getConfig();
		if (pc == null)
			return;

		DecodesScript ds = pc.getScript(name);
		if (ds == null)
		{
			try
			{
				ds = DecodesScript.empty()
								.platformConfig(pc)
								.scriptName(name)
								.build();
				int numberOfSensors = pc.getNumSensors();
				PlatformSensor ps;
				int i = 0;
				for(Iterator it = pc.getSensors(); it.hasNext(); ) {
					ConfigSensor cs = (ConfigSensor)it.next();
					ds.addScriptSensor(new ScriptSensor(ds, cs.sensorNumber));
				}
				pc.addScript(ds);
			}
			catch (DecodesScriptException | IOException ex)
			{
				throw new PanelException("Unable to create initial empty script.",ex);
			}
		}

		decodingScriptEditPanel.setDecodesScript(ds);
		decodingScriptEditPanel.clearDataBoxes();
	}

	/** Called when this panel is deactivated. */
	public boolean deactivate()
		throws PanelException
	{
		Platform p = PlatformWizard.instance().getPlatform();
		PlatformConfig pc = p.getConfig();
		if (pc == null)
			return true;
		decodingScriptEditPanel.stopEditing();
		DecodesScript ds = decodingScriptEditPanel.getDataFromFields();
		String chanNumFromRawMsg = decodingScriptEditPanel.getChannelNumber();
		PlatformWizard platwizTemp = PlatformWizard.instance();
		//if (platwizTemp.getSTChannel() == -1  )
		if ((platwizTemp.getFrame().startPanel.
										stChanField.getText()).equals(""))
		{//Set the self time channel from the decoded sample message
			if (chanNumFromRawMsg != null && !chanNumFromRawMsg.equals(""))
			{
				platwizTemp.getFrame().startPanel.stChanField.
												setText(chanNumFromRawMsg);
				if (p != null)
				{
					TransportMedium tm = 
						p.getTransportMedium(Constants.medium_GoesST);
					int tmIndex = p.transportMedia.indexOf(tm); 
					if (tm != null)
					{	
						tm.channelNum = 
							new Integer(chanNumFromRawMsg).intValue();
						//Update the transportMedia
						if (tmIndex != -1)
							p.transportMedia.set(tmIndex, tm);						
					}	
				}
			}
		}
		pc.addScript(ds);
		return true;
	}

	/** Called when application is shutdown. */
	public void shutdown()
	{
	}

}
