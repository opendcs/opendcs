/*
*	$Id$
*/
package decodes.dbeditor;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.Date;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.*;
import javax.swing.table.*;
import java.util.ResourceBundle;

import ilex.util.LoadResourceBundle;
import ilex.util.Logger;
import ilex.util.TextUtil;

import decodes.util.DecodesSettings;
import decodes.util.DecodesException;

import decodes.gui.TopFrame;
import decodes.gui.GuiDialog;
import decodes.gui.TableColumnAdjuster;
import decodes.gui.EnumCellEditor;
import decodes.db.Database;
import decodes.db.DecodesScript;
import decodes.db.FormatStatement;
import decodes.db.ScriptSensor;
import decodes.db.UnitConverterDb;
import decodes.db.Constants;
import decodes.db.PlatformConfig;
import decodes.db.PresentationGroup;
import decodes.db.Platform;
import decodes.db.TransportMedium;

import decodes.datasource.RawMessage;
import decodes.datasource.GoesPMParser;
import decodes.decoder.DecodedMessage;
import decodes.consumer.StringBufferConsumer;
import decodes.consumer.OutputFormatter;

/**
Dialog for editing a decoding script within the database editor.
*/
public class DecodingScriptEditDialog 
	extends GuiDialog
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	JPanel panel1 = new JPanel();
	BorderLayout borderLayout1 = new BorderLayout();
	JPanel jPanel4 = new JPanel();
	JButton okButton = new JButton();
	JButton cancelButton = new JButton();
	FlowLayout flowLayout3 = new FlowLayout();
	static DecodesScriptEditPanel decodingScriptEditPanel = null;
	private ConfigEditPanel parentCfgEditPanel = null;

	/**
	 * This is the constructor called by the GUI. Passed the DecodesScript
	 * object to be edited.
	 * @param ds the DecodesScript to edit.
	 */
	public DecodingScriptEditDialog(DecodesScript ds, ConfigEditPanel parentCfgEditPanel)
	{
		super(getDbEditFrame(), 
			dbeditLabels.getString("DecodingScriptEditDialog.title")
			+ ": " + ds.platformConfig.configName, true);
		this.parentCfgEditPanel = parentCfgEditPanel;
		if (decodingScriptEditPanel == null)
		{
			decodingScriptEditPanel = new DecodesScriptEditPanel();
		}
		decodingScriptEditPanel.setParentDialog(this);

		setDecodesScript(ds);
		try 
		{
			jbInit();
			pack();
		}
		catch(Exception ex) 
		{
			ex.printStackTrace();
		}
		trackChanges("DecodingScriptDialog");
	}

	public void setDecodesScript(DecodesScript ds)
	{
		decodingScriptEditPanel.setDecodesScript(ds);
		addWindowListener(
			new WindowAdapter()
			{
				boolean started=false;
				public void windowActivated(WindowEvent e)
				{
//					if (!started)
//						scriptNameField.requestFocus();
					started = true;
				}
			});
		decodingScriptEditPanel.setTraceDialog(new TraceDialog(this, true));
	}

	/** Fills in values from the object */
	void fillValues()
	{
		decodingScriptEditPanel.fillValues();
	}

	/** Move values from GUI components back to object */
	void getDataFromFields()
	{
		decodingScriptEditPanel.getDataFromFields();
	}


	/** JBuilder-generated method to initialize the GUI components */
	private void jbInit()
	{
		panel1.setLayout(borderLayout1);
		okButton.setText(genericLabels.getString("OK"));
		okButton.addActionListener(e -> okButton_actionPerformed(e));

		cancelButton.setText(genericLabels.getString("cancel"));
		cancelButton.addActionListener(e -> cancelButton_actionPerformed(e));
		jPanel4.setLayout(flowLayout3);
		flowLayout3.setHgap(35);
		flowLayout3.setVgap(10);

		panel1.setPreferredSize(new Dimension(800, 600));
		this.setModal(true);
		this.setTitle(
			dbeditLabels.getString("DecodingScriptEditDialog.title"));

		getContentPane().add(panel1);
		panel1.add(jPanel4, BorderLayout.SOUTH);
		panel1.add(decodingScriptEditPanel, BorderLayout.CENTER);
		jPanel4.add(okButton, null);
		jPanel4.add(cancelButton, null);
	}

	/** 
	  Called when OK button is pressed. 
	  @param e ignored
	*/
	void okButton_actionPerformed(ActionEvent e)
	{
		decodingScriptEditPanel.stopEditing();

		if (decodingScriptEditPanel.theScript.scriptName == null
		 || TextUtil.isAllWhitespace(decodingScriptEditPanel.theScript.scriptName))
		{
			TopFrame.instance().showError(
				dbeditLabels.getString("DecodingScriptEditDialog.scriptBlank"));
			return;
		}

		try
		{
			final DecodesScript theScript = decodingScriptEditPanel.getScript();
			theScript.prepareForExec();

			PlatformConfig pc = decodingScriptEditPanel.theScript.platformConfig;
			pc.rmScript(decodingScriptEditPanel.origScript);
			pc.addScript(decodingScriptEditPanel.theScript);
			closeDlg();
		}
		catch (DecodesException ex)
		{
			TopFrame.instance().showError(
				dbeditLabels.getString("DecodingScriptEditPanel.errorDecoding") + ex.getLocalizedMessage());
		}
	}

	/** Closes the dialog */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/** 
	  Called when Cancel button is pressed. 
	  @param e ignored
	*/
	void cancelButton_actionPerformed(ActionEvent e)
	{
		closeDlg();
	}

	public ConfigEditPanel getParentCfgEditPanel()
	{
		return parentCfgEditPanel;
	}

	public void setParentCfgEditPanel(ConfigEditPanel parentCfgEditPanel)
	{
		this.parentCfgEditPanel = parentCfgEditPanel;
	}
}



