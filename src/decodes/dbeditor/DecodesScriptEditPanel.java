/*
 * $Id$
 * 
 * $Log$
 * Revision 1.5  2015/04/15 19:59:46  mmaloney
 * Fixed synchronization bugs when the same data sets are being processed by multiple
 * routing specs at the same time. Example is multiple real-time routing specs with same
 * network lists. They will all receive and decode the same data together.
 *
 * Revision 1.4  2015/02/06 19:01:11  mmaloney
 * Bugfix: reset dbedit Load Message Dialog after successful retrieval.
 *
 * Revision 1.3  2015/01/31 15:44:59  mmaloney
 * Configurable Decoded Value Colors
 *
 * Revision 1.2  2014/10/07 12:55:39  mmaloney
 * dev
 *
 * Revision 1.1  2014/10/02 14:25:09  mmaloney
 * Added platformListDesignatorCol
 *
 * 
 * Reworked from open-source code in USGS repository and USACE HEC repository.
 * 
 * Open Source Software written by Cove Software, LLC under contract to the
 * U.S. Government.
 * 
 * Copyright 2014 U.S. Government
 */
package decodes.dbeditor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Properties;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.TitledBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.*;

import javax.swing.table.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import java.util.ResourceBundle;

import ilex.util.Logger;
import ilex.util.TeeLogger;
import ilex.var.TimedVariable;
import decodes.util.DecodesSettings;
import decodes.util.DecodesException;
import decodes.gui.EnumComboBox;
import decodes.gui.TopFrame;
import decodes.gui.TableColumnAdjuster;
import decodes.gui.EnumCellEditor;
import decodes.db.DecodesScript;
import decodes.db.FormatStatement;
import decodes.db.ScriptSensor;
import decodes.db.UnitConverterDb;
import decodes.db.Constants;
import decodes.db.Platform;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.db.TransportMedium;
import decodes.dbeditor.DbEditorFrame;
import decodes.datasource.RawMessage;
import decodes.datasource.GoesPMParser;
import decodes.datasource.EdlPMParser;
import decodes.datasource.IridiumPMParser;
import decodes.datasource.PMParser;
import decodes.datasource.HeaderParseException;
import decodes.decoder.DecodedMessage;
import decodes.decoder.DecodedSample;
import decodes.decoder.TimeSeries;
import decodes.decoder.TokenPosition;


/**
 * Panel for editing a decoding script.
 */
@SuppressWarnings("serial")
public class DecodesScriptEditPanel 
	extends JPanel
	implements SampleMessageOwner
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	private String channelNumber;
	private JTextField scriptNameField = new JTextField(10);
	private EnumComboBox headerTypeCombo = new EnumComboBox(Constants.enum_TMType, "");

	private FormatStatementTableModel formatStatementTableModel = new FormatStatementTableModel();
	private JTable formatStatementTable = new JTable(formatStatementTableModel);

	private UnitConversionTableModel unitConversionTableModel;
	private static GoesPMParser goesPMParser = new GoesPMParser();
	private static EdlPMParser edlPMParser = new EdlPMParser();
	private static IridiumPMParser iridiumPMParser = new IridiumPMParser();

	private TimeZone editTZ;
	private JTable unitConversionTable = new JTable();
	private JTextPane rawMessagePane = new JTextPane();
	private JTable decodedDataTable = null;
	DecodedDataTableModel decodedDataTableModel = new DecodedDataTableModel();
	private DataOrderCombo dataOrderCombo = new DataOrderCombo();

	/** local copy of script being edited. */
	DecodesScript theScript;

	/** Original script */
	DecodesScript origScript;

	public static TraceLogger traceLogger = null;
	private TraceDialog traceDialog = null;
	
	private int selectedDecodedDataRow = -1;
	private int selectedDecodedDataCol = -1;
	private LoadMessageDialog loadMessageDialog = null;
	
	boolean decodingDone = false;
	
	/** Color values used for sensors. Make mods here if necessary. */
	static int colorValues[] =
	{
		0x0000FF,	// blue
		0x00FFFF,	// cyan
		0xD2691E,	// orangish brown
		0x00D000,	// dark green
		0x8B0000,	// dark red
		0x4B0082,	// dark purple
		0x808000,	// olive
		0x8B4513	// dark brown
	};
	/** Strings used in the html JLabels for sensor colors */
	static String sensorColorHtml[] = new String[colorValues.length];
	static
	{
		for(int i=0; i<colorValues.length; i++)
		{
			StringBuilder sb = new StringBuilder("#");
			sb.append(Integer.toHexString(colorValues[i]));
			while(sb.length() < 7)
				sb.insert(1, '0');
			sensorColorHtml[i] = sb.toString();
		}
	}

	/** Style used for normal raw data text */
	Style normalRawDataStyle = null;
	/** Used for the message header (gray) */
	Style headerDataStyle = null;
	/** Styles used for painting raw data in different colors */
	Style sensorColorStyle[] = new Style[sensorColorHtml.length];
	/** Style used to highlight with bright yellow background */
	Style highlightStyle = null;
	/** Style used to unhighlight to plain white background */
	Style unhighlightStyle = null;
	
	/** After decoding, this is set to the header length. */
	int headerLength = 0;
	
	/** Noargs constructor */
	public DecodesScriptEditPanel()
	{
		origScript = theScript = null;
		
		unitConversionTable = new JTable(new UnitConversionTableModel(null, this));
		
		unitConversionTableModel = new UnitConversionTableModel(null, this);
		unitConversionTable = new JTable(unitConversionTableModel);
		TableColumnAdjuster.adjustColumnWidths(unitConversionTable, new int[]
		{ 6, 11, 11, 15, 10, 10, 10, 10, 10 });
		unitConversionTable.getTableHeader().setReorderingAllowed(false);
		TableColumn tc = unitConversionTable.getColumnModel().getColumn(3);
		tc.setCellEditor(new EnumCellEditor(Constants.eucvt_enumName));

		editTZ = java.util.TimeZone.getTimeZone(DecodesSettings.instance().editTimeZone);
		if (editTZ == null)
			editTZ = java.util.TimeZone.getTimeZone("UTC");
		decodedDataTableModel.setTZ(editTZ);

		Properties props = new Properties();
		props.setProperty("includeSensorNum", "true");
		guiInit();
		
		if (traceLogger == null)
		{
			Logger lg = Logger.instance();
			traceLogger = new TraceLogger(lg.getProcName());
			TeeLogger teeLogger = new TeeLogger(lg.getProcName(), lg, traceLogger);
			// teeLogger.setMinLogPriority(Logger.E_DEBUG3);
			traceLogger.setMinLogPriority(Logger.E_DEBUG3);
			Logger.setLogger(teeLogger);
		}
	}

	public void setTraceDialog(TraceDialog dlg)
	{
		traceDialog = dlg;
	}

	/**
	 * This is the constructor called by the GUI. Passed the DecodesScript
	 * object to be edited.
	 * 
	 * @param ds
	 *            the object to edit in this panel.
	 */
	public void setDecodesScript(DecodesScript ds)
	{
		origScript = ds;
		theScript = ds.copy(ds.platformConfig);
		formatStatementTableModel.setScript(theScript);
		unitConversionTableModel.setScript(theScript);
		fillValues();
	}

	/**
	 * This method clears the raw and decoded message text boxes. PLATWIZ uses
	 * this method to clear.
	 */
	public void clearDataBoxes()
	{
		rawMessagePane.setText("");
		
		decodedDataTableModel.clear();
	}

	/** Fills GUI components from the object */
	void fillValues()
	{
		formatStatementTable.getColumnModel().getColumn(0).setPreferredWidth(100);
		formatStatementTable.getColumnModel().getColumn(1).setPreferredWidth(700);

		scriptNameField.setText(theScript.scriptName);
		dataOrderCombo.setSelection(theScript.getDataOrder());
		String headerType = theScript.getHeaderType();
		if (headerType != null)
			headerTypeCombo.setSelection(headerType);
		else
			headerTypeCombo.setSelectedIndex(0);
		initColors();
	}
	
	void initColors()
	{
		DecodesSettings settings = DecodesSettings.instance();
		if (settings.decodeScriptColor1 != null && settings.decodeScriptColor1.trim().length() > 0)
		{
			try { colorValues[0] = Integer.parseInt(settings.decodeScriptColor1.trim(), 16); }
			catch(NumberFormatException ex)
			{
				Logger.instance().warning("Invalid setting 'decodesScriptColor1: "
					+ settings.decodeScriptColor1 + " ignored.");
			}
		}
		if (settings.decodeScriptColor2 != null && settings.decodeScriptColor2.trim().length() > 0)
		{
			try { colorValues[1] = Integer.parseInt(settings.decodeScriptColor2.trim(), 16); }
			catch(NumberFormatException ex)
			{
				Logger.instance().warning("Invalid setting 'decodesScriptColor2: "
					+ settings.decodeScriptColor2 + " ignored.");
			}
		}
		if (settings.decodeScriptColor3 != null && settings.decodeScriptColor3.trim().length() > 0)
		{
			try { colorValues[2] = Integer.parseInt(settings.decodeScriptColor3.trim(), 16); }
			catch(NumberFormatException ex)
			{
				Logger.instance().warning("Invalid setting 'decodesScriptColor3: "
					+ settings.decodeScriptColor3 + " ignored.");
			}
		}
		if (settings.decodeScriptColor4 != null && settings.decodeScriptColor4.trim().length() > 0)
		{
			try { colorValues[3] = Integer.parseInt(settings.decodeScriptColor4.trim(), 16); }
			catch(NumberFormatException ex)
			{
				Logger.instance().warning("Invalid setting 'decodesScriptColor4: "
					+ settings.decodeScriptColor4 + " ignored.");
			}
		}
		if (settings.decodeScriptColor5 != null && settings.decodeScriptColor5.trim().length() > 0)
		{
			try { colorValues[4] = Integer.parseInt(settings.decodeScriptColor5.trim(), 16); }
			catch(NumberFormatException ex)
			{
				Logger.instance().warning("Invalid setting 'decodesScriptColor5: "
					+ settings.decodeScriptColor5 + " ignored.");
			}
		}
		if (settings.decodeScriptColor6 != null && settings.decodeScriptColor6.trim().length() > 0)
		{
			try { colorValues[5] = Integer.parseInt(settings.decodeScriptColor6.trim(), 16); }
			catch(NumberFormatException ex)
			{
				Logger.instance().warning("Invalid setting 'decodesScriptColor6: "
					+ settings.decodeScriptColor6 + " ignored.");
			}
		}
		if (settings.decodeScriptColor7 != null && settings.decodeScriptColor7.trim().length() > 0)
		{
			try { colorValues[6] = Integer.parseInt(settings.decodeScriptColor7.trim(), 16); }
			catch(NumberFormatException ex)
			{
				Logger.instance().warning("Invalid setting 'decodesScriptColor7: "
					+ settings.decodeScriptColor7 + " ignored.");
			}
		}
		if (settings.decodeScriptColor8 != null && settings.decodeScriptColor8.trim().length() > 0)
		{
			try { colorValues[7] = Integer.parseInt(settings.decodeScriptColor8.trim(), 16); }
			catch(NumberFormatException ex)
			{
				Logger.instance().warning("Invalid setting 'decodesScriptColor8: "
					+ settings.decodeScriptColor8 + " ignored.");
			}
		}
		
		for(int i=0; i<colorValues.length; i++)
		{
			StringBuilder sb = new StringBuilder("#");
			sb.append(Integer.toHexString(colorValues[i]));
			while(sb.length() < 7)
				sb.insert(1, '0');
			sensorColorHtml[i] = sb.toString();
		}

		for(int i = 0; i<colorValues.length; i++)
		{
			sensorColorStyle[i] = rawMessagePane.addStyle("sensor"+i, null);
			StyleConstants.setForeground(sensorColorStyle[i], new Color(colorValues[i]));
		}
	}

	/**
	 * Gets the data from the fields & puts it back into the object.
	 * 
	 * @return the internal copy of the object being edited.
	 */
	public DecodesScript getDataFromFields()
	{
		theScript.scriptName = scriptNameField.getText();
		theScript.setDataOrder(dataOrderCombo.getSelection());
		theScript.scriptType = Constants.scriptTypeDecodes;
		String ht = headerTypeCombo.getSelection();
		if (ht != null && ht.length() > 0)
			theScript.scriptType = theScript.scriptType + ":" + ht;
		return theScript;
	}

	/** GUI component initialization */
	private void guiInit()
	{
		this.setLayout(new BorderLayout());
		JPanel northHeaderPanel = new JPanel(new GridBagLayout());
		this.add(northHeaderPanel, BorderLayout.NORTH);
		
		JLabel scriptNameLabel = new JLabel(
			dbeditLabels.getString("DecodingScriptEditDialog.scriptName"));
		northHeaderPanel.add(scriptNameLabel,
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(10, 5, 10, 2), 0, 0));
		northHeaderPanel.add(scriptNameField,
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(10, 0, 10, 5), 20, 0));
	
		JLabel dataOrderLabel = new JLabel(
			dbeditLabels.getString("DecodingScriptEditPanel.dataOrder"));
		northHeaderPanel.add(dataOrderLabel, 
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(10, 5, 10, 2), 0, 0));
		northHeaderPanel.add(dataOrderCombo, 
			new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(10, 0, 10, 5), 0, 0));
		
		JLabel headerTypeLabel = new JLabel(
			dbeditLabels.getString("DecodingScriptEditDialog.headerType"));
		northHeaderPanel.add(headerTypeLabel, 
			new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(10, 5, 10, 2), 0, 0));
		northHeaderPanel.add(headerTypeCombo,
			new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(10, 0, 10, 5), 0, 0));

		// Center main panel holds all the info
		JPanel mainPanel = new JPanel(new BorderLayout());
		this.add(mainPanel, BorderLayout.CENTER);

		// Upper splitpane is between format statements and the sample message.
		JSplitPane upperSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		mainPanel.add(upperSplitPane, BorderLayout.CENTER);
		JPanel fmtSensorGridPanel = new JPanel(new GridLayout(2, 1));
		upperSplitPane.add(fmtSensorGridPanel, JSplitPane.TOP);

		// Format Statement Panel with buttons on right
		JPanel fmtStatementPanel = new JPanel(new BorderLayout(4,4));
		fmtSensorGridPanel.add(fmtStatementPanel);
		fmtStatementPanel.setBorder(new TitledBorder(
			dbeditLabels.getString("DecodingScriptEditDialog.fmtStmts")));
		JScrollPane fmtStatementScrollPane = new JScrollPane();
		fmtStatementPanel.add(fmtStatementScrollPane, BorderLayout.CENTER);
		formatStatementTable = new JTable(formatStatementTableModel);

		TableColumn fmtStatementColumn = formatStatementTable.getColumnModel().getColumn(1);
		
		fmtStatementColumn.setCellRenderer(new FmtStatementRenderer());
		fmtStatementColumn.setCellEditor(new FmtStatementEditor());
		formatStatementTable.getTableHeader().setReorderingAllowed(false);
		fmtStatementScrollPane.getViewport().add(formatStatementTable);
		JPanel fmtStatementButtonPanel = new JPanel(new GridBagLayout());
		fmtStatementPanel.add(fmtStatementButtonPanel, BorderLayout.EAST);

		JButton moveFormatUpButton = new JButton(genericLabels.getString("upAbbr"));
		moveFormatUpButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				moveFormatUpButtonPressed();
			}
		});
		fmtStatementButtonPanel.add(moveFormatUpButton, 
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 4, 2, 4), 0, 0));

		JButton moveFormatDownButton = new JButton(genericLabels.getString("downAbbr"));
		moveFormatDownButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				moveFormatDownButtonPressed();
			}
		});
		fmtStatementButtonPanel.add(moveFormatDownButton, 
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 4, 2, 4), 0, 0));
		
		JButton addFormatStatementButton = new JButton(genericLabels.getString("add"));
		addFormatStatementButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				addFormatStatementButtonPressed();
			}
		});
		fmtStatementButtonPanel.add(addFormatStatementButton, 
			new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 4, 2, 4), 0, 0));

		JButton deleteFormatStatementButton = new JButton(genericLabels.getString("delete"));
		deleteFormatStatementButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				deleteFormatStatementButtonPressed();
			}
		});
		fmtStatementButtonPanel.add(deleteFormatStatementButton, 
			new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 4, 2, 4), 0, 0));

		// Script Sensor Panel for Units and Conversions
		JPanel scriptSensorPanel = new JPanel(new BorderLayout());
		scriptSensorPanel.setBorder(new TitledBorder(
			dbeditLabels.getString("DecodingScriptEditDialog.unitCnvt")));
		fmtSensorGridPanel.add(scriptSensorPanel);
		JScrollPane scriptSensorScrollPane = new JScrollPane();
		scriptSensorPanel.add(scriptSensorScrollPane, BorderLayout.CENTER);
		scriptSensorScrollPane.getViewport().add(unitConversionTable, null);

		// Sample Message area with buttons on the right
		JPanel rawMessagePanel = new JPanel(new BorderLayout(10,5));
		JPanel rawMsgButtonPanel = new JPanel(new GridBagLayout());
		rawMessagePanel.setBorder(new TitledBorder(
			dbeditLabels.getString("DecodingScriptEditDialog.sampMsg")));
		JScrollPane rawMsgScrollPane = new JScrollPane();
		rawMessagePanel.add(rawMsgScrollPane, BorderLayout.CENTER);
		rawMsgScrollPane.getViewport().add(rawMessagePane, null);
		rawMessagePanel.add(rawMsgButtonPanel, BorderLayout.EAST);
		
		// LOAD
		JButton loadSampleButton = new JButton(genericLabels.getString("load"));
		loadSampleButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				loadSampleButtonPressed();
			}
		});
		rawMsgButtonPanel.add(loadSampleButton, 
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 4, 2, 4), 0, 0));
		
		// CLEAR
		JButton clearSampleButton = new JButton(genericLabels.getString("clear"));
		clearSampleButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				clearSampleButtonPressed();
			}
		});
		rawMsgButtonPanel.add(clearSampleButton, 
			new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 4, 2, 4), 0, 0));

		// DECODES
		JButton decodeSampleButton = new JButton(genericLabels.getString("decode"));
		decodeSampleButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				decodeSampleButtonPressed();
			}
		});
		rawMsgButtonPanel.add(decodeSampleButton,
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 4, 2, 4), 0, 0));
		
		// TRACE
		JButton traceButton = new JButton(dbeditLabels.getString("DecodingScriptEditPanel.trace"));
		traceButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				traceButton_actionPerformed(e);
			}
		});
		rawMsgButtonPanel.add(traceButton,
			new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE, 
				new Insets(2, 4, 2, 4), 0, 0));

		decodedDataTable = new JTable(decodedDataTableModel);
		decodedDataTable.getTableHeader().setReorderingAllowed(false);
		DefaultTableCellRenderer headerRenderer = 
			(DefaultTableCellRenderer)decodedDataTable.getTableHeader().getDefaultRenderer();
		headerRenderer.setHorizontalAlignment(JLabel.CENTER);
		decodedDataTable.setCellSelectionEnabled(true);
		decodedDataTable.getColumnModel().getSelectionModel().addListSelectionListener(
			new ListSelectionListener()
			{
				@Override
				public void valueChanged(ListSelectionEvent e)
				{
					selectDecodedDataCell(decodedDataTable.getSelectedRow(),
						decodedDataTable.getSelectedColumn());
				}
			});

		decodedDataTable.getSelectionModel().addListSelectionListener(
			new ListSelectionListener()
			{
				@Override
				public void valueChanged(ListSelectionEvent e)
				{
					selectDecodedDataCell(decodedDataTable.getSelectedRow(),
						decodedDataTable.getSelectedColumn());
				}
			});

		JScrollPane decodedDataScrollPane = new JScrollPane(
			decodedDataTable, 
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		decodedDataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		decodedDataTable.getColumnModel().getColumn(0).setPreferredWidth(160);
//		decodedDataScrollPane.getViewport().add(decodedDataTable);

		JPanel decodedDataPanel = new JPanel(new BorderLayout());
		decodedDataPanel.setBorder(new TitledBorder(
			dbeditLabels.getString("DecodingScriptEditDialog.decData")));

		JSplitPane rawDecMsgSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		rawDecMsgSplitPane.setBottomComponent(decodedDataPanel);
		rawDecMsgSplitPane.setTopComponent(rawMessagePanel);

		
		
		upperSplitPane.add(rawDecMsgSplitPane, JSplitPane.BOTTOM);
		
		rawDecMsgSplitPane.add(rawMessagePanel, JSplitPane.TOP);
		rawDecMsgSplitPane.add(decodedDataPanel, JSplitPane.BOTTOM);
		decodedDataPanel.add(decodedDataScrollPane, BorderLayout.CENTER);
		
		normalRawDataStyle = rawMessagePane.addStyle("normal", null);
		StyleConstants.setFontFamily(normalRawDataStyle, "Monospaced");
		StyleConstants.setFontSize(normalRawDataStyle, 
			StyleConstants.getFontSize(normalRawDataStyle) + 2);
		headerDataStyle = rawMessagePane.addStyle("header", null);
		StyleConstants.setForeground(headerDataStyle, Color.gray);
		
		initColors();

		highlightStyle = rawMessagePane.addStyle("highlight", null);
		StyleConstants.setBackground(highlightStyle, Color.yellow);
		unhighlightStyle = rawMessagePane.addStyle("unhighlight", null);
		StyleConstants.setBackground(unhighlightStyle, Color.white);
		
		rawMessagePane.addCaretListener(
			new CaretListener()
			{
				@Override
				public void caretUpdate(CaretEvent e)
				{
//System.out.println("rawMessagePane CaretEvent: " + e);
					MutableAttributeSet inputAttr = rawMessagePane.getInputAttributes();
					inputAttr.removeAttribute(StyleConstants.Foreground);
				}
			});
		
		FontMetrics metrics = formatStatementTable.getFontMetrics(
			formatStatementTable.getFont());
		int fontHeight = metrics.getHeight();
//System.out.println("format statement table font height=" + fontHeight);
		formatStatementTable.setRowHeight((int)(fontHeight*1.4));
	}

	private void selectDecodedDataCell(int row, int col)
	{
		if (row < 0 || col < 0)
		{
//System.out.println("selectDecodedDataCell called with negative! " + row + "," + col);
			return;
		}

		if (row != selectedDecodedDataRow
		 || col != selectedDecodedDataCol)
		{
//System.out.println("selectDecodedDataCell(" + row + ", " + col + ")");
//System.out.println("Unhighlighting all");
			rawMessagePane.getStyledDocument().setCharacterAttributes(
				0, rawMessagePane.getStyledDocument().getLength(),
				unhighlightStyle, false);
			selectedDecodedDataRow = row;
			selectedDecodedDataCol = col;
			if (col == 0)
			{
				if (row > 0)
				{
					
				}
//System.out.println("column 0 (time) selected");
				//TODO Highlight all cells in this row.
				//TODO Highlight corresponding raw data
			}
			else
			{
				DecodedSample decSamp = findDecodedSampleContaining(
					decodedDataTableModel.getTimedVariableAt(row, col));
				if (decSamp == null)
				{
//System.out.println("No decoded sample at row " + row + ", col " + col);
				}
				else
				{
					TokenPosition rawpos = decSamp.getRawDataPosition();
//					TimeSeries ts = decSamp.getTimeSeries();
					highlightRawData(rawpos.getStart(), 
						rawpos.getEnd()-rawpos.getStart(), (col-1) % sensorColorStyle.length,
						true);
				}
			}
		}
	}
	
	/**
	 * @param tv the timed variable
	 * @return the DecodedSample bean containing the passed timed variable, or null
	 *    if none found.
	 */
	private DecodedSample findDecodedSampleContaining(TimedVariable tv)
	{
		if (tv == null)
			return null;
		for(DecodedSample samp : theScript.getDecodedSamples())
			if (samp.getSample() == tv)
				return samp;
		return null;
	}
	
	private void colorRawData()
	{
		// TODO Clear all the styles from the JTextPane
		// StyledDocument.setCharacterAttributes(0, lenOfTextPane,
		//   attrForPlainBlack, true);
		
		// TODO
		for(DecodedSample decSamp : theScript.getDecodedSamples())
		{
			int sensorNum = decSamp.getTimeSeries().getSensorNumber();
			// TODO determine the color from the sensor.
			// This is in the decoded data table model.
			TokenPosition rawDataPos = decSamp.getRawDataPosition();
			// StyledDocument.setCharacterAttributes(
			// rawDataPos.getStart(), rawDataPos.getEnd() - rawDataPos.getStart(),
			// sampleColorAttrs, false);
			// Note use false for replace: leave font style & size unchanged.
		}
	}
	
	
	/** Called when 'Add' button is pressed to add a format statement. */
	void addFormatStatementButtonPressed()
	{
		formatStatementTableModel.add(new FormatStatement(theScript, formatStatementTableModel
			.getRowCount()));
	}

	/**
	 * Called when 'Delete' button is pressed to delete a format statement.
	 */
	void deleteFormatStatementButtonPressed()
	{ // Allow multiple deletes
		int nrows = formatStatementTable.getSelectedRowCount();
		if (nrows == 0)
		{
			TopFrame.instance().showError(
				dbeditLabels.getString("DecodingScriptEditPanel.selectDelete"));
			return;
		}
		int[] rows = formatStatementTable.getSelectedRows();
		FormatStatement fs[] = new FormatStatement[nrows];
		for (int x = 0; x < nrows; x++)
			fs[x] = formatStatementTableModel.getObjectAt(rows[x]);


		// New code added to skip this confirm delete check
		// If you start up dbedit with the turnOnOffPopUps command set to Off
		// do not display this check
		if (DecodesDbEditor.turnOffPopUps) // if true just remove do not ask
		{
			for (int i = 0; i < nrows; i++)
			{
				if (fs[i] != null)
					formatStatementTableModel.remove(fs[i]);
			}
		}
		else
		{
			int rsp = JOptionPane.showConfirmDialog(this,
				dbeditLabels.getString("DecodingScriptEditPanel.confirmDeleteMsg"),
				dbeditLabels.getString("DecodingScriptEditPanel.confirmDeleteTitle"),
				JOptionPane.YES_NO_OPTION);
			if (rsp == JOptionPane.YES_OPTION)
			{
				for (int i = 0; i < nrows; i++)
				{
					if (fs[i] != null)
						formatStatementTableModel.remove(fs[i]);
				}
			}
		}
	}

	/**
	 * Called when the 'Move Up' button is pressed. Moves selected format up one
	 * line.
	 */
	void moveFormatUpButtonPressed()
	{
		int r = formatStatementTable.getSelectedRow();
		FormatStatement fs;
		if (r == -1 || (fs = formatStatementTableModel.getObjectAt(r)) == null)
		{
			TopFrame.instance().showError(
				dbeditLabels.getString("DecodingScriptEditPanel.selectUp"));
			return;
		}
		formatStatementTableModel.moveUp(fs, r);
		if (r > 0)
		{
			formatStatementTable.setRowSelectionInterval(r - 1, r - 1);
			formatStatementTable.setEditingRow(r - 1);
		}
	}

	/**
	 * Called when the 'Move Dn' button is pressed. Moves selected format down
	 * one line.
	 */
	void moveFormatDownButtonPressed()
	{
		int r = formatStatementTable.getSelectedRow();
		FormatStatement fs;
		if (r == -1 || (fs = formatStatementTableModel.getObjectAt(r)) == null)
		{
			TopFrame.instance().showError(
				dbeditLabels.getString("DecodingScriptEditPanel.selectDn"));
			return;
		}
		formatStatementTableModel.moveDown(fs, r);
		if (r < formatStatementTableModel.getRowCount() - 1)
		{
			formatStatementTable.setRowSelectionInterval(r + 1, r + 1);
			formatStatementTable.setEditingRow(r + 1);
		}
	}

	/**
	 * Called when the 'Load Sample Message' is pressed. Instantiates & starts
	 * the modal LoadMessageDialog.
	 */
	void loadSampleButtonPressed()
	{
		if (loadMessageDialog == null)
		{
			loadMessageDialog = new LoadMessageDialog();
			loadMessageDialog.setSampleMessageOwner(this);
		}
		else
			loadMessageDialog.reset();
		TopFrame.getDbEditFrame().launchDialog(loadMessageDialog);
		decodingDone = false;
	}

	/**
	 * Called when the 'Clear Sample Message' button is pressed. Clears the
	 * JTextArea containing the sample message.
	 */
	void clearSampleButtonPressed()
	{
		rawMessagePane.setText("");
		decodedDataTableModel.clear();
	}

	/**
	 * Called when the Decode button is pressed. Dummy up the DECODES database
	 * objects (platform, transport medium, etc.) needed to do the decoding for
	 * this script in this config. Do the decoding and display the results in
	 * the decoded-message JTextArea.
	 */
	void decodeSampleButtonPressed()
	{
		rawMessagePane.getStyledDocument().setCharacterAttributes(
			0, rawMessagePane.getStyledDocument().getLength(),
			normalRawDataStyle, true);

		TableCellEditor tce = formatStatementTable.getCellEditor();
		if (tce != null)
		{
			tce.stopCellEditing();
		}

		tce = unitConversionTable.getCellEditor();
		if (tce != null)
			tce.stopCellEditing();

		getDataFromFields();

		String s = rawMessagePane.getText();
		StringBuilder sb = new StringBuilder(s);
		for (int i = 0; i < sb.length(); i++)
		{
			char c = sb.charAt(i);
			if (c == (char) 0x00AE)
				sb.setCharAt(i, '\r');
		}
		s = sb.toString();
		String mediumType = Constants.medium_Goes;
		PMParser pmParser = goesPMParser;
		if (s.startsWith("//"))
		{
			mediumType = Constants.medium_EDL;
			pmParser = edlPMParser;
		}
		else if (s.startsWith("ID="))
		{
			mediumType = Constants.medium_IRIDIUM;
			pmParser = iridiumPMParser;
		}
		else
		{
			String mt = headerTypeCombo.getSelectedItem().toString();
			if (mt != null && mt.length() > 0)
				try
				{
					pmParser = PMParser.getPMParser(mt);
					mediumType = mt;
				}
				catch (HeaderParseException e1)
				{
					e1.printStackTrace();
				}
		}

		int len = s.length();
		if (len < pmParser.getHeaderLength())
		{
			TopFrame.instance().showError(
				dbeditLabels.getString("DecodingScriptEditPanel.incompleteSample"));
			return;
		}
		try
		{
			// Set up dummy platform to do decoding.
			Platform tmpPlatform = new Platform();
			tmpPlatform.setSite(new Site());
			tmpPlatform.getSite().addName(new SiteName(tmpPlatform.getSite(), "USGS", "dummy"));
			tmpPlatform.setConfig(theScript.platformConfig);
			tmpPlatform.setConfigName(theScript.platformConfig.configName);
			TransportMedium tmpMedium = new TransportMedium(tmpPlatform, mediumType, "11111111");
			tmpMedium.scriptName = theScript.scriptName;
			tmpMedium.setDecodesScript(theScript);
			tmpPlatform.transportMedia.add(tmpMedium);

			traceDialog.clear();
			traceLogger.setDialog(traceDialog);

			RawMessage rawMessage = new RawMessage(s.getBytes(), len);
			rawMessage.setPlatform(tmpPlatform);
			rawMessage.setTransportMedium(tmpMedium);
			headerLength = 0;
			try
			{
				pmParser.parsePerformanceMeasurements(rawMessage);
				headerLength = rawMessage.getHeaderLength();
				Logger.instance().info("Header type '" + mediumType 
					+ "' length=" + pmParser.getHeaderLength());
				for(Iterator<String> pmnit = rawMessage.getPMNames(); pmnit.hasNext(); )
				{
					String pmn = pmnit.next();
					Logger.instance().info("  PM:" + pmn + "=" + rawMessage.getPM(pmn));
				}
			}
			catch (HeaderParseException ex)
			{
				tmpMedium.setTimeZone(DecodesSettings.instance().editTimeZone);
				tmpMedium.setMediumType(Constants.medium_EDL);
				// Set dummy medium id -- rawMessage must have a medium id set
				// to avoid
				// an error in the parser. It doesn't actually need one because
				// the platform and
				// script id is known by context. (SED - 06/11/2008)
				rawMessage.setMediumId("11111111");
				edlPMParser.parsePerformanceMeasurements(rawMessage);
				Logger.instance().info("" + ex + " -- will process as EDL file with no header.");
			}
			Date timeStamp;
			try
			{
				timeStamp = rawMessage.getPM(GoesPMParser.MESSAGE_TIME).getDateValue();
			}
			catch (Exception ex)
			{
				timeStamp = new Date();
			}

			rawMessage.setTimeStamp(timeStamp);
			theScript.prepareForExec();
			// MJM 20020920 added:
			tmpMedium.prepareForExec();

			DecodesScript.trackDecoding = true;
			DecodedMessage dm = theScript.decodeMessage(rawMessage);
			Logger.instance().debug1("After decoding there are " 
				+ theScript.getDecodedSamples().size() + " decoded samples.");
			
			decodedDataTableModel.clear();
			decodedDataTableModel.setDecodedData(dm);
			
			// setDecodedData will change the table structure, which resets column widths.
			// Make sure the date/time column is wide enough.
			if (decodedDataTableModel.getRowCount() > 0)
			{
				String d = (String)decodedDataTableModel.getValueAt(0, 0);
				JLabel t = new JLabel(d);
				int w = t.getPreferredSize().width;
//System.out.println("preferred width of '" + d + "' is " + w);
				decodedDataTable.getColumnModel().getColumn(0).setPreferredWidth(w + 20);
			}
			
			// Now make sure that the columns are wide enough to accommodate the sensor
			// names.
			DefaultTableColumnModel colModel = 
				(DefaultTableColumnModel)decodedDataTable.getColumnModel();
			for(int colidx = 1; colidx < decodedDataTableModel.getColumnCount(); colidx++)
			{
			    TableColumn col = colModel.getColumn(colidx);
			    int width = 0;
			    TableCellRenderer renderer = col.getHeaderRenderer();
			    if (renderer == null)
			        renderer = decodedDataTable.getTableHeader().getDefaultRenderer();
			    java.awt.Component comp = renderer.getTableCellRendererComponent(
			    	decodedDataTable, col.getHeaderValue(), false, false, -1, colidx);
			    width = comp.getPreferredSize().width;
			    if (width < 80)
			    	width = 80;
			    col.setPreferredWidth(width);
			}
			
			traceLogger.setDialog(null);
			
			rawMessagePane.getStyledDocument().setCharacterAttributes(0, 
				rawMessage.getHeaderLength(), headerDataStyle, false);
			for(DecodedSample decodedSample : theScript.getDecodedSamples())
			{
				TokenPosition rawpos = decodedSample.getRawDataPosition();
				TimeSeries ts = decodedSample.getTimeSeries();
				int col = decodedDataTableModel.getTimeSeriesColumn(ts);
				if (col != -1)
				{
					Style st = sensorColorStyle[col % sensorColorStyle.length];
					rawMessagePane.getStyledDocument().setCharacterAttributes(
						headerLength+rawpos.getStart(), 
						rawpos.getEnd()-rawpos.getStart(), st, false);
				}
			}
			
			selectedDecodedDataRow = selectedDecodedDataCol = -1;
		
			// Get the Channel number - used by the platform wizard
			try
			{
				channelNumber = rawMessage.getPM(GoesPMParser.CHANNEL).getStringValue();
			}
			catch (Exception ex)
			{
				channelNumber = "";
			}
			decodingDone = true;
			unitConversionTableModel.ftdc();
			unitConversionTable.clearSelection();
		}
		catch (DecodesException ex)
		{
			TopFrame.instance().showError(
				dbeditLabels.getString("DecodingScriptEditPanel.errorDecoding") + ex);
			return;
		}
	}
	
	/**
	 * Stops all the table cell editors so that current changes are stored in
	 * the JTable.
	 */
	public void stopEditing()
	{
		TableCellEditor tce = formatStatementTable.getCellEditor();
		if (tce != null)
			tce.stopCellEditing();
		formatStatementTableModel.adjustSequenceNumbers();

		tce = unitConversionTable.getCellEditor();
		if (tce != null)
			tce.stopCellEditing();

		getDataFromFields();
	}

	void traceButton_actionPerformed(ActionEvent e)
	{
		traceDialog.setVisible(true);
	}

	public String getChannelNumber()
	{
		return channelNumber;
	}

	public void setChannelNumber(String channelNumber)
	{
		this.channelNumber = channelNumber;
	}

	@Override
	public void setRawMessage(String msgData)
	{
//		rawMessagePane.setText(msgData);
		StyledDocument sdoc = rawMessagePane.getStyledDocument();
		try
		{
			sdoc.remove(0, sdoc.getLength());
			sdoc.insertString(0, msgData, normalRawDataStyle);
		}
		catch (BadLocationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//System.out.println("set raw msg '" + msgData + "'");
	}
	
	public void highlightRawData(int start, int length, int sensorColorIdx, boolean highlight)
	{
		StyledDocument sdoc = rawMessagePane.getStyledDocument();
		sdoc.setCharacterAttributes(headerLength+start, length, 
			highlight ? highlightStyle : unhighlightStyle, false);
		
//System.out.println("Highlighted (after headerlen=" + headerLength + ") start=" 
//+ start + ", len=" + length + ", highlight=" + highlight);
	}
}

class FormatStatementTableModel extends AbstractTableModel
{
	static String columnNames[] =
	{ DecodesScriptEditPanel.dbeditLabels.getString("DecodingScriptEditPanel.labelCol"),
		DecodesScriptEditPanel.dbeditLabels.getString("DecodingScriptEditPanel.formatCol") };
	private DecodesScript theScript = null;

	public FormatStatementTableModel()
	{
	}

	void setScript(DecodesScript ds)
	{
		theScript = ds;
		fillValues();
	}

	public void fillValues()
	{
		fireTableDataChanged();
	}

	public int getRowCount()
	{
		return theScript == null ? 0 : theScript.formatStatements.size();
	}

	public int getColumnCount()
	{
		return columnNames.length;
	}

	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	public FormatStatement getObjectAt(int r)
	{
		if (r < 0 || r >= getRowCount())
			return null;
		return theScript.formatStatements.elementAt(r);
	}

	public Object getValueAt(int r, int c)
	{
		FormatStatement fs = getObjectAt(r);
		if (fs == null)
			return null;

		switch (c)
		{
		case 0:
			return fs.label;
		case 1:
			return fs.format;
		default:
			return "";
		}
	}

	void add(FormatStatement ob)
	{
		if (theScript != null)
			theScript.formatStatements.add(ob);
		fireTableDataChanged();
	}

	void remove(FormatStatement ob)
	{
		if (theScript != null)
			theScript.formatStatements.remove(ob);
		fireTableDataChanged();
	}

	public boolean isCellEditable(int r, int c)
	{
		return true; // Both label and format fields are editable
	}

	public void setValueAt(Object aValue, int r, int c)
	{
		FormatStatement fs = getObjectAt(r);
		if (fs == null)
			return;
		switch (c)
		{
		case 0:
			fs.label = (String) aValue;
			break;
		case 1:
			fs.format = (String) aValue;
			if (fs.format == null)
				fs.format = "";
			break;
		}

		fireTableCellUpdated(r, c);
	}

	void moveUp(FormatStatement ob, int r)
	{
		if (r <= 0)
			return;
		FormatStatement tmp = theScript.formatStatements.elementAt(r - 1);
		theScript.formatStatements.setElementAt(ob, r - 1);
		theScript.formatStatements.setElementAt(tmp, r);
		fireTableDataChanged();
	}

	void moveDown(FormatStatement ob, int r)
	{
		int n = theScript.formatStatements.size();
		if (r >= n - 1)
			return;
		FormatStatement tmp = theScript.formatStatements.elementAt(r + 1);
		theScript.formatStatements.setElementAt(ob, r + 1);
		theScript.formatStatements.setElementAt(tmp, r);
		fireTableDataChanged();
	}

	/*
	 * Called when OK button is pressed. Go through table and make the sequence
	 * numbers conform to the current position in the table.
	 */
	void adjustSequenceNumbers()
	{
		int nr = getRowCount();
		for (int i = 0; i < nr; i++)
		{
			FormatStatement fs = getObjectAt(i);
			fs.sequenceNum = i;
		}
	}
}

class UnitConversionTableModel extends AbstractTableModel
{
	static String columnNames[] =
	{ "#", DecodesScriptEditPanel.genericLabels.getString("name"),
		DecodesScriptEditPanel.genericLabels.getString("units"),
		DecodesScriptEditPanel.genericLabels.getString("algorithm"), "A", "B", "C", "D", "E", "F" };
	private DecodesScript theScript;
	private DecodesScriptEditPanel parent = null;

	public UnitConversionTableModel(DecodesScript ds, DecodesScriptEditPanel parent)
	{
		setScript(ds);
		this.parent = parent;
	}

	void setScript(DecodesScript ds)
	{
		theScript = ds;
		fillValues();
	}
	
	void ftdc()
	{
		fireTableDataChanged();
	}

	public void fillValues()
	{
		for (int i = 0; i < getRowCount(); i++)
		{
			ScriptSensor ss = getObjectAt(i);
			if (ss.rawConverter == null)
			{
				ss.rawConverter = new UnitConverterDb("raw", "unknown");
				ss.rawConverter.algorithm = Constants.eucvt_none;
			}
		}
		fireTableDataChanged();
	}

	public int getRowCount()
	{
		return theScript == null ? 0 : theScript.scriptSensors.size();
	}

	public int getColumnCount()
	{
		return columnNames.length;
	}

	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	public ScriptSensor getObjectAt(int r)
	{
		if (r < 0 || r >= getRowCount())
			return null;
		return (ScriptSensor) theScript.scriptSensors.elementAt(r);
	}

	public Object getValueAt(int r, int c)
	{
		ScriptSensor ss = getObjectAt(r);
		if (ss == null)
			return null;
		UnitConverterDb uc = ss.rawConverter;
		int nCoeffs = 0;
		if (uc == null)
			nCoeffs = 0;
		else
			nCoeffs = getNumCoeffs(uc.algorithm);
		
		// If decoding is done, try to get the html color corresponding to the
		// sensor corresponding to the selected row.
		String htmlColor = null;
		if (parent.decodingDone)
		{
			int decodedSensorColumn = parent.decodedDataTableModel.getSensorNumberColumn(
				ss.sensorNumber);
			if (decodedSensorColumn >= 0) // this sensor has decoded data
			{
				int colorIdx = decodedSensorColumn % 
					DecodesScriptEditPanel.sensorColorHtml.length;
				htmlColor = DecodesScriptEditPanel.sensorColorHtml[colorIdx];                        
			}
		}

		switch (c)
		{
		case 0:
			return htmlColor == null ? ("" + ss.sensorNumber)
				: ("<html><font color=\"" + htmlColor + "\">" + ss.sensorNumber 
					+ "</font></html>");
		case 1:
			return htmlColor == null ? ss.getSensorName()
				: ("<html><font color=\"" + htmlColor + "\">" + ss.getSensorName() 
					+ "</font></html>");
		case 2:
			return uc == null ? "" : uc.toAbbr;
		case 3:
			return uc == null ? "" : uc.algorithm;
		case 4:
			return uc == null ? "" : nCoeffs <= 0 ? ""
				: uc.coefficients[0] == Constants.undefinedDouble ? "" : ("" + uc.coefficients[0]);

		case 5:
			return uc == null ? "" : nCoeffs <= 1 ? ""
				: uc.coefficients[0] == Constants.undefinedDouble ? "" : ("" + uc.coefficients[1]);
		case 6:
			return uc == null ? "" : nCoeffs <= 2 ? ""
				: uc.coefficients[0] == Constants.undefinedDouble ? "" : ("" + uc.coefficients[2]);
		case 7:
			return uc == null ? "" : nCoeffs <= 3 ? ""
				: uc.coefficients[0] == Constants.undefinedDouble ? "" : ("" + uc.coefficients[3]);
		case 8:
			return uc == null ? "" : nCoeffs <= 4 ? ""
				: uc.coefficients[0] == Constants.undefinedDouble ? "" : ("" + uc.coefficients[4]);
		case 9:
			return uc == null ? "" : nCoeffs <= 5 ? ""
				: uc.coefficients[0] == Constants.undefinedDouble ? "" : ("" + uc.coefficients[5]);
		default:
			return "";
		}
	}

	private int getNumCoeffs(String algorithm)
	{
		return algorithm.equalsIgnoreCase(Constants.eucvt_none) ? 0 : algorithm
			.equalsIgnoreCase(Constants.eucvt_linear) ? 2 : algorithm
			.equalsIgnoreCase(Constants.eucvt_usgsstd) ? 4 : algorithm
			.equalsIgnoreCase(Constants.eucvt_poly5) ? 6 : 6;
	}

	public boolean isCellEditable(int r, int c)
	{
		ScriptSensor ss = getObjectAt(r);
		if (ss == null)
			return false;
		UnitConverterDb uc = ss.rawConverter;
		if (uc == null)
			return false;
		int nCoeffs = getNumCoeffs(uc.algorithm);
		return c >= 2 && c <= (3 + nCoeffs);
	}

	public void setValueAt(Object aValue, int r, int c)
	{
		String s = (String) aValue;
		ScriptSensor ss = getObjectAt(r);
		if (ss == null)
			return;
		UnitConverterDb uc = ss.rawConverter;
		uc.execConverter = null;
		if (c == 2)
		{
			uc.toAbbr = (String) aValue;
		}
		else if (c == 3)
		{
//			int oldn = this.getNumCoeffs(uc.algorithm);
			uc.algorithm = s;
			int newn = this.getNumCoeffs(uc.algorithm);
			double defaults[] =
			{ 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
			if (uc.algorithm.equalsIgnoreCase(Constants.eucvt_linear))
			{
				defaults[0] = 1.0;
			}
			else if (uc.algorithm.equalsIgnoreCase(Constants.eucvt_usgsstd))
			{
				defaults[0] = 1.0;
				defaults[2] = 1.0;
			}
			else if (uc.algorithm.equalsIgnoreCase(Constants.eucvt_poly5))
			{
				defaults[4] = 1.0;
			}

			// Set default values depending on algorithm used.
			for (int i = 0; i < newn; i++)
				uc.coefficients[i] = defaults[i];

			fireTableDataChanged();
		}
		else
		{
			try
			{
				double d = Double.parseDouble(s);
				uc.coefficients[c - 4] = d;
				fireTableCellUpdated(r, c);
			}
			catch (NumberFormatException e)
			{
				TopFrame.instance().showError(
					DecodesScriptEditPanel.dbeditLabels
						.getString("DecodingScriptEditPanel.coeffNumber"));
			}
		}
	}
}

@SuppressWarnings("serial")
class DecodedDataTableModel extends AbstractTableModel
{
	private TimeSeries colTimeSeries[] = new TimeSeries[0];
	class Row
	{
		// The date in the leftmost column
		Date rowDate = null;
		// One for each column. This is the index into the TimeSeries
		// of the value displayed in this cell. -1 means the cell is blank.
		int tsIndeces[] = null;
		
		Row(Date d, int numColumns)
		{
			rowDate = d;
			tsIndeces = new int[numColumns];
			for(int i=0; i<numColumns; i++)
				tsIndeces[i] = -1;
		}
	};
	private ArrayList<Row> rows = new ArrayList<Row>();
	private SimpleDateFormat decDataDateFmt = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
	private String columnColor[] = new String[0];
	

	
	public void setTZ(TimeZone editTZ)
	{
		decDataDateFmt.setTimeZone(editTZ);
	}

	@Override
	public int getRowCount()
	{
		return rows.size();
	}

	@Override
	public int getColumnCount()
	{
		// A column for date and one for each time series.
		return colTimeSeries.length + 1;
	}
	
	public TimeSeries getColumnTimeSeries(int col)
	{
		if (col < 1 || col > colTimeSeries.length)
			return null;
		return colTimeSeries[col-1];
	}
	
	public int getTimeSeriesColumn(TimeSeries ts)
	{
		for(int i=0; i<colTimeSeries.length; i++)
			if (ts == colTimeSeries[i])
				return i;
		return -1;
	}
	
	public int getSensorNumberColumn(int sensorNum)
	{
		for(int i=0; i<colTimeSeries.length; i++)
			if (colTimeSeries[i] != null
			 && colTimeSeries[i].getSensorNumber() == sensorNum)
				return i;
		return -1;
	}
	
	public TimedVariable getTimedVariableAt(int rowIndex, int columnIndex)
	{
		if (columnIndex == 0)
			return null; // column 0 is date/time
		
		Row row = rows.get(rowIndex);
		int tsIndex = row.tsIndeces[columnIndex-1];
		return tsIndex != -1 ? colTimeSeries[columnIndex-1].sampleAt(tsIndex)
			: null;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		Row row = rows.get(rowIndex);
		if (columnIndex == 0)
			return decDataDateFmt.format(row.rowDate);
			
		int tsIndex = row.tsIndeces[columnIndex-1];
		String color = columnColor[columnIndex-1];
		String v = tsIndex != -1 
			? colTimeSeries[columnIndex-1].formattedSampleAt(tsIndex)
			: "";
		String ret = "<html><font color=\"" + color + "\">" + v + "</font></html>";
		return ret;
	}
	
	@Override
	public String getColumnName(int col)
	{
		if (col == 0)
			return "<html>Date/Time<br>(" + decDataDateFmt.getTimeZone().getID()
				+ ")</html>";
		
		String color = columnColor[col-1];
		
		// <html>sensorNum: sensorName<br>units</html>
		return "<html><font color=\"" + color + "\">" 
			+ colTimeSeries[col-1].getSensorNumber()
			+ ": " + colTimeSeries[col-1].getSensorName()
			+ "<br>" + colTimeSeries[col-1].getUnits()
			+ "</font></html>";
	}
	
	public void clear()
	{
		colTimeSeries = new TimeSeries[0];
		rows.clear();
		fireTableDataChanged();
	}
	
	public void setDecodedData(DecodedMessage decmsg)
	{
		colTimeSeries = new TimeSeries[decmsg.getNumTimeSeries()];
		columnColor = new String[decmsg.getNumTimeSeries()];
		
		// Get the time series into the columns.
		int numTS = 0;
		for(Iterator<TimeSeries> tsit = decmsg.getAllTimeSeries(); tsit.hasNext();
			numTS++)
		{
			colTimeSeries[numTS] = tsit.next();
			columnColor[numTS] = DecodesScriptEditPanel.sensorColorHtml[
			     numTS % DecodesScriptEditPanel.sensorColorHtml.length];
		}
		
		// Build the Row data structures.
		for(int tsIdx = 0; tsIdx < numTS; tsIdx++)
		{
			TimeSeries ts = colTimeSeries[tsIdx];
			
			for(int sampleNum = 0; sampleNum < ts.size(); sampleNum++)
			{
				TimedVariable tv = ts.sampleAt(sampleNum);
				Date rowDate = tv.getTime();
				Row row = findRowFor(rowDate);
				if (row == null)
				{
					row = new Row(rowDate, numTS);
					rows.add(row);
				}
				row.tsIndeces[tsIdx] = sampleNum;
			}
		}
		//Sort the rows by row date.
		Collections.sort(rows,
			new Comparator<Row>()
			{
				@Override
				public int compare(Row o1, Row o2)
				{
					return o1.rowDate.compareTo(o2.rowDate);
				}
			});
//System.out.println("There are now " + getColumnCount() + " columns.");
		fireTableStructureChanged();
	}
	
	private Row findRowFor(Date d)
	{
		for(Row row : rows)
			if (row.rowDate.equals(d))
				return row;
		return null;
	}
}

@SuppressWarnings("serial")
class FmtStatementRenderer
	extends JLabel
	implements TableCellRenderer
{
	public FmtStatementRenderer()
	{
		super();
		setFont(new Font("Monospaced", Font.PLAIN, getFont().getSize()+1));
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
		boolean hasFocus, int row, int column)
	{
		//TODO -- decide if I need to do any html insertion/extaction here.
		setText((String)value);
		return this;
	}
}

@SuppressWarnings("serial")
class FmtStatementEditor
	extends DefaultCellEditor
{
	private String origText = null;
	
	public FmtStatementEditor()
	{
		super(new JTextField());
		editorComponent.setFont(new Font("Monospaced", Font.PLAIN, 
			editorComponent.getFont().getSize()+1));
	}
	
	public Component getTableCellEditorComponent(JTable table, 
		Object value, boolean isSelected, int row, int column)
	{
		JTextField ec = (JTextField)super.getTableCellEditorComponent(table,
			value, isSelected, row, column);
		origText = ec.getText();
//System.out.println("Starting edit of text '" + origText + "'");
		return ec;
	}
	
	public boolean stopCellEditing()
	{
//System.out.println("in stopCellEditing, text is now '" + ((JTextField)editorComponent).getText() + "'");
		return super.stopCellEditing();
	}
}


