/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.tsdb.compedit;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.TitledBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.*;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbCompAlgorithmScript;
import decodes.tsdb.ScriptType;
import decodes.tsdb.algo.AWAlgoType;
import decodes.util.DecodesSettings;


/**
 * Panel for editing a decoding script.
 */
@SuppressWarnings("serial")
public class PythonAlgoEditPanel extends JPanel implements CaretListener, PythonAlgoTracer
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private JTextPane beforeScriptPane = null, timeSliceScriptPane = null,
		afterScriptPane = null;
	private JTextField algoNameField = new JTextField(20);
	private JComboBox<AWAlgoType> algoTypeCombo = new JComboBox<>(AWAlgoType.values());
	private JTextPane testPane = null;
	private JTextField beforeScriptPos = new JTextField(6);
	private JTextField tsScriptPos = new JTextField(6);
	private JTextField afterScriptPos = new JTextField(6);
	private JTabbedPane scriptTabbedPane = new JTabbedPane();

	private DbCompAlgorithm pythonAlgo = null;
	private PythonAlgoEditDialog parent = null;
	private PyFuncSelectDialog pyFuncSelectDlg = null;
	private PyFuncSelectDialog cpFuncSelectDlg = null;
	private PyParamSetDialog pyParamSetDlg = null;
	private Properties initProps = new Properties();


	/** Noargs constructor */
	public PythonAlgoEditPanel(PythonAlgoEditDialog parent)
	{
		guiInit();
		this.parent = parent;

	}

	public void setPythonAlgo(DbCompAlgorithm pythonAlgo)
	{
		this.pythonAlgo = pythonAlgo;
		fillValues();
	}

	/**
	 * This method clears the raw and decoded message text boxes. PLATWIZ uses
	 * this method to clear.
	 */
	public void clearAll()
	{
		algoNameField.setText("");
		algoTypeCombo.setSelectedIndex(0);
		initProps.clear();
		try
		{
			if (beforeScriptPane.getDocument().getLength() > 0)
				beforeScriptPane.getDocument().remove(0, beforeScriptPane.getDocument().getLength());
			if (timeSliceScriptPane.getDocument().getLength() > 0)
				timeSliceScriptPane.getDocument().remove(0, timeSliceScriptPane.getDocument().getLength());
			if (afterScriptPane.getDocument().getLength() > 0)
				afterScriptPane.getDocument().remove(0, afterScriptPane.getDocument().getLength());
		}
		catch (BadLocationException ex)
		{
			log.atError().setCause(ex).log("Invalid location in document.");
		}
	}

	/** Fills GUI components from the object */
	void fillValues()
	{
		clearAll();

		algoNameField.setText(pythonAlgo.getName());

		algoTypeCombo.setSelectedIndex(0);
		log.debug("PythonAlgoEditPanel.fillValues, algo has {} scripts.", pythonAlgo.getScripts().size());
		for(DbCompAlgorithmScript script : pythonAlgo.getScripts())
		{
			log.debug("PythonAlgoEditPanel.fillValues script {} text='\n{}'",
					  script.getScriptType(), script.getText());
			try
			{
				switch(script.getScriptType())
				{
					case PY_BeforeTimeSlices:
						beforeScriptPane.getDocument().insertString(0, script.getText(), null);
						break;
					case PY_TimeSlice:
						timeSliceScriptPane.getDocument().insertString(0, script.getText(), null);
						break;
					case PY_AfterTimeSlices:
						afterScriptPane.getDocument().insertString(0, script.getText(), null);
						break;
					case PY_Init:
						try
						{
							initProps.load(new StringReader(script.getText()));
							String s = PropertiesUtil.getIgnoreCase(initProps, "AlgorithmType");
							if (s != null)
							{
								AWAlgoType aat = AWAlgoType.fromString(s);
								if (aat != null)
									algoTypeCombo.setSelectedItem(aat);
							}
						}
						catch (IOException ex)
						{
							log.atWarn()
							.setCause(ex)
							.log("PythonAlgoEditPanel.fillValues cannot parse init properties from '{}'",
									script.getText());
						}
						break;
					default:
					{
						log.warn("Algorithm '{}' has invalid script type '{}' -- ignored.",
								 pythonAlgo.getName(), script.getScriptType());
					}
				}
			}
			catch (BadLocationException ex)
			{
				log.atWarn()
				   .setCause(ex)
				   .log("PythonAlgoEditPanel.fillValues script {} text='{}'",
				   		script.getScriptType(), script.getText());
			}
		}
	}

	/** GUI component initialization */
	private void guiInit()
	{
		// North holds algo name and type pulldown
		this.setLayout(new BorderLayout());
		JPanel northHeaderPanel = new JPanel(new GridBagLayout());
		this.add(northHeaderPanel, BorderLayout.NORTH);

		// ================= North Header Pane ===================
		JLabel algoNameLabel = new JLabel("Algorithm Name:");
		northHeaderPanel.add(algoNameLabel,
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(8, 8, 8, 2), 0, 0));
		algoNameField.setEditable(false);
		northHeaderPanel.add(algoNameField,
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(8, 0, 8, 20), 0, 0));

		JLabel algoTypeLabel = new JLabel("Algorithm Type:");
		northHeaderPanel.add(algoTypeLabel,
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(8, 5, 8, 2), 0, 0));
		northHeaderPanel.add(algoTypeCombo,
			new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(8, 0, 8, 8), 0, 0));

		// =============== Center Script Editing Pane ====================

		// Construct the JTextPanes for the scripts
		PythonStyledDocument psd = new PythonStyledDocument();
		beforeScriptPane = new JTextPane(psd);
		psd.setPane(beforeScriptPane);
		beforeScriptPane.addCaretListener(this);

		psd = new PythonStyledDocument();
		timeSliceScriptPane = new JTextPane(psd);
		psd.setPane(timeSliceScriptPane);
		timeSliceScriptPane.addCaretListener(this);

		psd = new PythonStyledDocument();
		afterScriptPane = new JTextPane(psd);
		psd.setPane(afterScriptPane);
		afterScriptPane.addCaretListener(this);

		// Add styles for each of the python text types to each of the panes.
		int fsize = 0;
		for(PythonTextType ptt : PythonTextType.values())
		{
			Style style = beforeScriptPane.addStyle(ptt.name(), null);
			StyleConstants.setFontFamily(style, "Monospaced");
			StyleConstants.setFontSize(style,
				(fsize = StyleConstants.getFontSize(style) + 1 + DecodesSettings.instance().fontAdjust));
			StyleConstants.setForeground(style, ptt.getDisplayColor());

			style = timeSliceScriptPane.addStyle(ptt.name(), null);
			StyleConstants.setFontFamily(style, "Monospaced");
			StyleConstants.setFontSize(style,
				StyleConstants.getFontSize(style) + 1 + DecodesSettings.instance().fontAdjust);
			StyleConstants.setForeground(style, ptt.getDisplayColor());

			style = afterScriptPane.addStyle(ptt.name(), null);
			StyleConstants.setFontFamily(style, "Monospaced");
			StyleConstants.setFontSize(style,
				StyleConstants.getFontSize(style) + 1 + DecodesSettings.instance().fontAdjust);
			StyleConstants.setForeground(style, ptt.getDisplayColor());
		}



		// Put the code panes in a tabbed pane.
		JPanel tabbedPaneParent = new JPanel(new BorderLayout());
		tabbedPaneParent.setBorder(new TitledBorder("Python Scripts"));

		this.add(tabbedPaneParent, BorderLayout.CENTER);

		tabbedPaneParent.add(scriptTabbedPane, BorderLayout.CENTER);

		JPanel beforeTab = new JPanel(new BorderLayout());
		scriptTabbedPane.add(beforeTab, "Before Time Slices");
		JPanel timeSliceTab = new JPanel(new BorderLayout());
		scriptTabbedPane.add(timeSliceTab, "Time Slice");
		JPanel afterTab = new JPanel(new BorderLayout());
		scriptTabbedPane.add(afterTab, "After Time Slices");

		// before
		JPanel noWrapPanel = new JPanel(new BorderLayout());
		noWrapPanel.add(beforeScriptPane, BorderLayout.CENTER);
		JScrollPane beforeScrollPane = new JScrollPane(noWrapPanel,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		beforeTab.add(beforeScrollPane, BorderLayout.CENTER);
		JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 1));
		p.add(new JLabel("Position: "));
		beforeScriptPos.setEditable(false);
		p.add(beforeScriptPos);
		beforeTab.add(p, BorderLayout.SOUTH);

		// ts
		noWrapPanel = new JPanel(new BorderLayout());
		noWrapPanel.add(timeSliceScriptPane, BorderLayout.CENTER);
		JScrollPane tsScrollPane = new JScrollPane(noWrapPanel,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		timeSliceTab.add(tsScrollPane, BorderLayout.CENTER);
		p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2,1));
		p.add(new JLabel("Position: "));
		tsScriptPos.setEditable(false);
		p.add(tsScriptPos);
		timeSliceTab.add(p, BorderLayout.SOUTH);

		// after
		afterScriptPane.setText("afterScriptPane");
		noWrapPanel = new JPanel(new BorderLayout());
		noWrapPanel.add(afterScriptPane, BorderLayout.CENTER);
		JScrollPane afterScrollPane = new JScrollPane(noWrapPanel,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		afterTab.add(afterScrollPane, BorderLayout.CENTER);
		p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2,1));
		p.add(new JLabel("Position: "));
		afterScriptPos.setEditable(false);
		p.add(afterScriptPos);
		afterTab.add(p, BorderLayout.SOUTH);

		// button panel to right of tabbed pane with editing functions
		JPanel editButtonsPanel = new JPanel(new GridBagLayout());
		JButton pythonBuiltInsButton = new JButton("Python Built-In");
		pythonBuiltInsButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					pythonBuiltInsPressed();
				}
			});
		editButtonsPanel.add(pythonBuiltInsButton,
			new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(20, 4, 2, 4), 0, 0));
		JButton ccpBuiltInsButton = new JButton("CCP Built-In");
		ccpBuiltInsButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					ccpBuiltInsPressed();
				}
			});
		editButtonsPanel.add(ccpBuiltInsButton,
			new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
				GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
				new Insets(2, 4, 2, 4), 0, 0));
		tabbedPaneParent.add(editButtonsPanel, BorderLayout.EAST);

		setTabStops();

	}

	private void setTabStops()
	{
		// Set tabstop to be 4 chars
		TabStop tabStops[] = new TabStop[10];

		int charWidth = 8;

		for(int ti = 0; ti<tabStops.length; ti++)
		{
			int pos = (ti+1) * 4 * charWidth;
			tabStops[ti] = new TabStop(pos, TabStop.ALIGN_LEFT, TabStop.LEAD_NONE);

		}
		TabSet tabset = new TabSet(tabStops);
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.TabSet, tabset);
        beforeScriptPane.setParagraphAttributes(aset, false);
        timeSliceScriptPane.setParagraphAttributes(aset, false);
        afterScriptPane.setParagraphAttributes(aset, false);
	}

	protected void ccpBuiltInsPressed()
	{
		if (cpFuncSelectDlg == null)
			cpFuncSelectDlg = new PyFuncSelectDialog(null,
				PythonStyledDocument.getCpFunctions());
		launchFuncDialog(cpFuncSelectDlg);
	}

	protected void pythonBuiltInsPressed()
	{
		if (pyFuncSelectDlg == null)
			pyFuncSelectDlg = new PyFuncSelectDialog(null,
			PythonStyledDocument.getBuiltinFunctions());
		launchFuncDialog(pyFuncSelectDlg);
	}

	private void launchFuncDialog(PyFuncSelectDialog dlg)
	{
		parent.launchDialog(dlg);
		if (!dlg.wasCancelled())
		{
			PyFunction func = dlg.getSelection();
			int idx = scriptTabbedPane.getSelectedIndex();
			JTextPane activeScriptPane =
				idx == 0 ? beforeScriptPane :
				idx == 1 ? timeSliceScriptPane : afterScriptPane;
			StyledDocument activeDoc = activeScriptPane.getStyledDocument();
			int pos = activeScriptPane.getCaret().getDot();
			try
			{
				activeDoc.insertString(pos, func.getSignature(),
					activeDoc.getStyle(PythonTextType.NormalText.name()));
			}
			catch (BadLocationException ex)
			{
				log.atWarn()
				   .setCause(ex)
				   .log("Can't insert text '{}' into JTextPane at position {}", func.getSignature(), pos);
			}
		}
	}

	/** Called when the parent dialog hits the commit button. */
	public void saveToObject(DbCompAlgorithm ob)
	{
		ob.clearScripts();
		String text = beforeScriptPane.getText();
		if (text.length() > 0 && !TextUtil.isAllWhitespace(text))
		{
			DbCompAlgorithmScript script = new DbCompAlgorithmScript(ob,
				ScriptType.PY_BeforeTimeSlices);
			script.addToText(text);
			ob.putScript(script);
		}

		text = timeSliceScriptPane.getText();
		if (text.length() > 0 && !TextUtil.isAllWhitespace(text))
		{
			DbCompAlgorithmScript script = new DbCompAlgorithmScript(ob,
				ScriptType.PY_TimeSlice);
			script.addToText(text);
			ob.putScript(script);
		}

		text = afterScriptPane.getText();
		if (text.length() > 0 && !TextUtil.isAllWhitespace(text))
		{
			DbCompAlgorithmScript script = new DbCompAlgorithmScript(ob,
				ScriptType.PY_AfterTimeSlices);
			script.addToText(text);
			ob.putScript(script);
		}

		initProps.setProperty("AlgorithmType", algoTypeCombo.getSelectedItem().toString());
		if (pyParamSetDlg != null)
		{
			ArrayList<PyParamSpec> pspecs = pyParamSetDlg.getParamSpecs();
			if (pspecs != null)
				for (PyParamSpec pps : pspecs)
				{
					if (pps.tsid != null)
						initProps.setProperty(pps.role + ".tsid", pps.tsid.getUniqueString());
					if (pps.value != null)
						initProps.setProperty(pps.role + ".value", pps.value.toString());
				}
		}
		StringWriter sw = new StringWriter();
		try
		{
			initProps.store(sw, null);
			DbCompAlgorithmScript script = new DbCompAlgorithmScript(ob,
				ScriptType.PY_Init);
			script.addToText(sw.toString());
			ob.putScript(script);
		}
		catch (IOException ex)
		{
			log.atWarn().setCause(ex).log("Error building init script from properties.");
		}
	}

	@Override
	public void caretUpdate(CaretEvent e)
	{
		JTextPane jtp = (JTextPane)e.getSource();
		int pos = e.getDot();
		Element map = jtp.getDocument().getDefaultRootElement();
		int row = map.getElementIndex(pos);
		Element lineElem = map.getElement(row);
		int col = pos - lineElem.getStartOffset();

		if (jtp == beforeScriptPane)
			beforeScriptPos.setText("" + row + "/" + col);
		else if (jtp == timeSliceScriptPane)
			tsScriptPos.setText("" + row + "/" + col);
		else
			afterScriptPos.setText("" + row + "/" + col);
	}

	@Override
	public void traceMsg(String msg)
	{
		/* do notthing? */
	}

}
