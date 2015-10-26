/*
 * $Id$
 * 
 * $Log$
 *
 * Open Source Software written by Cove Software, LLC under contract to the
 * U.S. Government.
 * 
 * Copyright 2015 U.S. Army Corps of Engineers Hydrologic Engineering Center
 */
package decodes.tsdb.compedit;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.*;
import java.util.Calendar;
import java.util.Properties;
import java.util.TimeZone;

import javax.swing.table.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import ilex.gui.DateTimeCalendar;
import ilex.util.Logger;
import ilex.util.TeeLogger;
import ilex.var.TimedVariable;
import decodes.gui.PropertiesEditDialog;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.algo.AWAlgoType;


/**
 * Panel for editing a decoding script.
 */
@SuppressWarnings("serial")
public class PythonAlgoEditPanel 
	extends JPanel
	implements CaretListener
{
	private JTextPane beforeScriptPane = null, timeSliceScriptPane = null,
		afterScriptPane = null;
	private JTextField algoNameField = new JTextField(20);
	private JComboBox algoTypeCombo = new JComboBox(AWAlgoType.values());
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
	private DateTimeCalendar runDateTimeCal = null;
	private PropertiesEditDialog propertiesEditDialog = null;

	/** Noargs constructor */
	public PythonAlgoEditPanel(PythonAlgoEditDialog parent)
	{
		guiInit();
		this.parent = parent;
		
//		if (traceLogger == null)
//		{
//			Logger lg = Logger.instance();
//			traceLogger = new TraceLogger(lg.getProcName());
//			TeeLogger teeLogger = new TeeLogger(lg.getProcName(), lg, traceLogger);
//			// teeLogger.setMinLogPriority(Logger.E_DEBUG3);
//			traceLogger.setMinLogPriority(Logger.E_DEBUG3);
//			Logger.setLogger(teeLogger);
//		}
	}

//	public void setTraceDialog(TraceDialog dlg)
//	{
//		traceDialog = dlg;
//	}

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
		beforeScriptPane.setText("");
		timeSliceScriptPane.setText("");
		afterScriptPane.setText("");
	}

	/** Fills GUI components from the object */
	void fillValues()
	{
		clearAll();
		
		algoNameField.setText(pythonAlgo.getName());

		//TODO where do I store the algo type? Property? Get it and set control here.
		algoTypeCombo.setSelectedIndex(0);
		
		//TODO where are the scripts in the Java model?
		// They will be stored somewhere in the DbCompAlgorithm object.
		//TODO go through the text and set proper colors for each type of text.
	}
	
	/**
	 * Gets the data from the fields & puts it back into the object.
	 * 
	 * @return the internal copy of the object being edited.
	 */
	public DbCompAlgorithm getDataFromFields()
	{
		//TODO store the algo type from the combo
		//TODO store the 3 scripts
		return pythonAlgo;
	}

	/** GUI component initialization */
	private void guiInit()
	{
		// North holds algo name and type pulldown
		this.setLayout(new BorderLayout());
		JPanel northHeaderPanel = new JPanel(new GridBagLayout());
		this.add(northHeaderPanel, BorderLayout.NORTH);
		
		// Center holds split pane for scripts and test area
		JSplitPane centerSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		this.add(centerSplitPane, BorderLayout.CENTER);

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
		beforeScriptPane = new JTextPane(new PythonStyledDocument());
		timeSliceScriptPane = new JTextPane();
		afterScriptPane = new JTextPane();
		beforeScriptPane.addCaretListener(this);
		timeSliceScriptPane.addCaretListener(this);
		afterScriptPane.addCaretListener(this);
		
		// Add styles for each of the python text types to each of the panes.
		for(PythonTextType ptt : PythonTextType.values())
		{
			Style style = beforeScriptPane.addStyle(ptt.name(), null);
			StyleConstants.setFontFamily(style, "Monospaced");
			StyleConstants.setFontSize(style, 
				StyleConstants.getFontSize(style) + 1);
			StyleConstants.setForeground(style, ptt.getDisplayColor());

			style = timeSliceScriptPane.addStyle(ptt.name(), null);
			StyleConstants.setFontFamily(style, "Monospaced");
			StyleConstants.setFontSize(style, 
				StyleConstants.getFontSize(style) + 1);
			StyleConstants.setForeground(style, ptt.getDisplayColor());

			style = afterScriptPane.addStyle(ptt.name(), null);
			StyleConstants.setFontFamily(style, "Monospaced");
			StyleConstants.setFontSize(style, 
				StyleConstants.getFontSize(style) + 1);
			StyleConstants.setForeground(style, ptt.getDisplayColor());
		}
		
		JPanel tabbedPaneParent = new JPanel(new BorderLayout());
		tabbedPaneParent.setBorder(new TitledBorder("Python Scripts"));
		centerSplitPane.add(tabbedPaneParent, JSplitPane.TOP);
	
		tabbedPaneParent.add(scriptTabbedPane, BorderLayout.CENTER);
		
		JPanel beforeTab = new JPanel(new BorderLayout());
		scriptTabbedPane.add(beforeTab, "Before Time Slices");
		JPanel timeSliceTab = new JPanel(new BorderLayout());
		scriptTabbedPane.add(timeSliceTab, "Time Slice");
		JPanel afterTab = new JPanel(new BorderLayout());
		scriptTabbedPane.add(afterTab, "After Time Slices");

		// before
beforeScriptPane.setText("beforeScriptPane");
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
		
		//TODO figure out how to do tooltip on the tab label
		
		// ts
timeSliceScriptPane.setText("timeSliceScriptPane");
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

		//=================== Test Area ==================
		testPane = new JTextPane();
		JPanel testAreaPanel = new JPanel(new GridBagLayout());
		centerSplitPane.add(testAreaPanel, JSplitPane.BOTTOM);
		testAreaPanel.setBorder(new TitledBorder("Test Run"));
		
		JButton setInputsButton = new JButton("Set Inputs");
		setInputsButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					setParamsPressed();
				}
			});
		testAreaPanel.add(setInputsButton,
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(3, 4, 2, 2), 0, 0));
		
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("UTC"));
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		runDateTimeCal = new DateTimeCalendar(
			"Run for Time: ", cal.getTime(), "dd/MMM/yyyy", "UTC");
		testAreaPanel.add(runDateTimeCal,
			new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 2, 2, 4), 0, 0));

		JButton runTestButton = new JButton("Run Script");
		runTestButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					runScriptPressed();
				}
			});
		testAreaPanel.add(runTestButton,
			new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 4, 2, 4), 0, 0));
	
		noWrapPanel = new JPanel(new BorderLayout());
		noWrapPanel.add(testPane, BorderLayout.CENTER);
		JScrollPane testScrollPane = new JScrollPane(noWrapPanel,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		testAreaPanel.add(testScrollPane, 
			new GridBagConstraints(0, 1, 3, 1, 1.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, 
				new Insets(4, 8, 4, 8), 0, 0));
	}
	
			




	protected void runScriptPressed()
	{
		// TODO Fill in the following code sections
		testPane.setText("");
		appendToTestPane("Run time will be " + runDateTimeCal.getDate());
		
		appendToTestPane("\nList of Properties with Values Here...");
		appendToTestPane("\n\nInitial Property Values:");
		
		for(Object key : pythonAlgo.getProperties().keySet())
			appendToTestPane("\n\t" + (String)key + " = " + pythonAlgo.getProperty((String)key));
		
		appendToTestPane("\n\nList of Parameters with initial values here...");
		
		appendToTestPane("\n\nScript trace messages here...");
		
		appendToTestPane("\n\nParameter values and flags after script run here...");
	}
	
	public void appendToTestPane(String str)
	{
		StyledDocument doc = testPane.getStyledDocument();
		int pos = testPane.getCaret().getDot();
		try
		{
			doc.insertString(pos, str,
				doc.getStyle(PythonTextType.NormalText.name()));
		}
		catch (BadLocationException ex)
		{
			Logger.instance().warning("Can't insert text '" + str
				+ "' into test pane at position " + pos + ": " + ex);
		}
	}

	protected void setParamsPressed()
	{
		if (pyParamSetDlg == null)
			pyParamSetDlg = new PyParamSetDialog(parent);
		pyParamSetDlg.fillControls(pythonAlgo);
		parent.launchDialog(pyParamSetDlg);
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
				Logger.instance().warning("Can't insert text '" + func.getSignature()
					+ "' into JTextPane at position " + pos + ": " + ex);
			}
		}
	}

	/**
	 * OK was pressed. Stop editing and copy back controls to the object.
	 * @return true if it's ok to exit. Otherwise, show error and return false.
	 */
	public boolean okPressed()
	{
		// TODO Auto-generated method stub
		System.out.println("PythonAlgoEditPanel.okPressed");
		return false;
	}

	public void cancelPressed()
	{
		System.out.println("PythonAlgoEditPanel.cancelPressed");

		// TODO Auto-generated method stub
		
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
System.out.println("caretUpdate pos=" + pos + ", row=" + row + ", col=" + col);
		if (jtp == beforeScriptPane)
			beforeScriptPos.setText("" + row + "/" + col);
		else if (jtp == timeSliceScriptPane)
			tsScriptPos.setText("" + row + "/" + col);
		else
			afterScriptPos.setText("" + row + "/" + col);
	}
}

//@SuppressWarnings("serial")
//class FmtStatementRenderer
//	extends JLabel
//	implements TableCellRenderer
//{
//	public FmtStatementRenderer()
//	{
//		super();
//		setFont(new Font("Monospaced", Font.PLAIN, getFont().getSize()+1));
//	}
//
//	@Override
//	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
//		boolean hasFocus, int row, int column)
//	{
//		//TODO -- decide if I need to do any html insertion/extaction here.
//		setText((String)value);
//		return this;
//	}
//}
//
//@SuppressWarnings("serial")
//class FmtStatementEditor
//	extends DefaultCellEditor
//{
//	private String origText = null;
//	
//	public FmtStatementEditor()
//	{
//		super(new JTextField());
//		editorComponent.setFont(new Font("Monospaced", Font.PLAIN, 
//			editorComponent.getFont().getSize()+1));
//	}
//	
//	public Component getTableCellEditorComponent(JTable table, 
//		Object value, boolean isSelected, int row, int column)
//	{
//		JTextField ec = (JTextField)super.getTableCellEditorComponent(table,
//			value, isSelected, row, column);
//		origText = ec.getText();
////System.out.println("Starting edit of text '" + origText + "'");
//		return ec;
//	}
//	
//	public boolean stopCellEditing()
//	{
////System.out.println("in stopCellEditing, text is now '" + ((JTextField)editorComponent).getText() + "'");
//		return super.stopCellEditing();
//	}
//}


