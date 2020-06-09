/*
 *  $Id$
 *
 *  This is open-source software written by ILEX Engineering, Inc., under
 *  contract to the federal government. You are free to copy and use this
 *  source code for your own purposes, except that no part of the information
 *  contained in this file may be claimed to be proprietary.
 *
 *  Except for specific contractual terms between ILEX and the federal 
 *  government, this source code is provided completely without warranty.
 *  For more information contact: info@ilexeng.com
 *  
 *  $Log$
 *  Revision 1.6  2013/01/27 19:32:31  mmaloney
 *  Fix cosmetic problem in output parm area. Some refactoring.
 *  Got rid of dead code.
 *
 */
package decodes.tsdb.algoedit;

import ilex.util.LoadResourceBundle;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.ArrayList;

import decodes.db.Constants;
import decodes.gui.AddEditDeletePanel;
import decodes.gui.SortingListTable;
import decodes.gui.TopFrame;
import decodes.util.DecodesSettings;
import decodes.tsdb.algo.AWAlgoType;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CompMetaData;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbAlgoParm;
import decodes.tsdb.xml.CompXio;
import decodes.tsdb.TsdbAppTemplate;

public class AlgorithmWizard
	extends TsdbAppTemplate
	implements AlgoData
{
	//Variables for all labels
	private static ResourceBundle labels = null;
	private static ResourceBundle genericLabels = null;
	
	private TopFrame algowizFrame = null;
	private JPanel topPanel = null;
	private JPanel northPanel = null;
	private JLabel algorithmNameLabel;
	private JTextField algorithmField = null;
	private JLabel classNameLabel;
	private JLabel algorithmTypeLabel;
	private JTextField classField = null;
	private JComboBox typeCombo = null;
	private JLabel packageLabel;
	private JTextField packageField = null;
	private JPanel Comments = null;
	private JPanel propPanel = null;
	private JLabel extendsLabel;
	private JTextField extendsField;
	private JLabel implementsLabel;
	private JTextField implementsField;

	private JTextArea commentsArea = null;
	private JTextArea importsArea = null;
	private JScrollPane commentsScrollPane = null;
	private JPanel inputParmPanel = null;
	private JPanel outputParmPanel = null;
	private JComboBox aggregateCombo = null;
	private JTable inputParmTable = null;
	private InputTimeSeriesTableModel inputTableModel = null;
	private JScrollPane inputParmScrollPane = null;
	private JList outputParmList = null;
	private JPanel codePanel = null; // @jve:decl-index=0:visual-constraint="613,308"

	private JTabbedPane codeSectionTabbedPane = null;
	private JScrollPane localVarsPane = null;
	private JTextArea localVarsArea = null;
	private JScrollPane oneTimeInitAreaPane = null;
	private JTextArea oneTimeInitArea = null;
	private JScrollPane beforeTimeSlicePane = null;
	private JTextArea beforeTimeSliceArea = null;
	private JScrollPane timeSlicePanel = null;
	private JTextArea timeSliceArea = null;
	private JScrollPane afterTimeSlicePanel = null;
	private JTextArea afterTimeSliceArea = null;
	private DefaultListModel outputListModel = null;
	private JMenuBar menuBar = null;

	/** Used on file-open and file-saveAs */
	private JFileChooser fileChooser;

	public static final String templateFile = 
		"decodes/tsdb/algo/AW_AlgorithmTemplate.java";

	private AlgoWriter algoWriter = null;
	private AlgoReader algoReader = null;
	File theFile = null;
	private AlgoPropTableModel algoPropModel;
	private SortingListTable algoPropTable;
	CompileDialog compileDialog = null;
	private boolean exitOnClose = true;

	private static AlgorithmWizard _instance = null;

	public AlgorithmWizard()
	{
		super("algoedit.log");
		_instance = this;
	}

	public static AlgorithmWizard instance() { return _instance; }

	public static void getMyLabelDescriptions()
	{
		DecodesSettings settings = DecodesSettings.instance();
		//Load the generic properties file - includes labels that are used
		//in multiple screens
		genericLabels = LoadResourceBundle.getLabelDescriptions(
				"decodes/resources/generic",
				settings.language);
		//Return the main label descriptions for Algorithm Wizard App
		labels = LoadResourceBundle.getLabelDescriptions(
				"decodes/resources/algoedit",
				settings.language);	
	}
	
	public static ResourceBundle getLabels() 
	{
		if (labels == null)
			getMyLabelDescriptions();
		return labels;
	}

	public static ResourceBundle getGenericLabels() 
	{
		if (genericLabels == null)
			getMyLabelDescriptions();
		return genericLabels;
	}
	
	private TopFrame initTopFrame()
	{
		algoWriter = new AlgoWriter(templateFile);
		algoReader = new AlgoReader();
		compileDialog = new CompileDialog(this, algoWriter);
		exitOnClose = true;
		
		algorithmNameLabel = 
			new JLabel(labels.getString("AlgorithmWizard.algorithmName"));
		classNameLabel = 
			new JLabel(labels.getString("AlgorithmWizard.javaClassName"));
		algorithmTypeLabel = 
			new JLabel(labels.getString("AlgorithmWizard.algorithmType"));
		packageLabel = 
			new JLabel(labels.getString("AlgorithmWizard.javaPackage"));
		extendsLabel = 
			new JLabel(labels.getString("AlgorithmWizard.extend"));
		implementsLabel = 
			new JLabel(labels.getString("AlgorithmWizard.implement"));
		
		fileChooser = new JFileChooser();
		extendsField = new JTextField();
		implementsField = new JTextField();
		
		algowizFrame = new TopFrame();
		algowizFrame.setTitle(labels.getString("AlgorithmWizard.frameTitle"));
		makeMenus();
		algowizFrame.setJMenuBar(menuBar);
		algowizFrame.setContentPane(getTopPanel());
		//algowizFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		algowizFrame.setDefaultCloseOperation(
				WindowConstants.DO_NOTHING_ON_CLOSE);
		algowizFrame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				doExit();
			}
		});
		algowizFrame.pack();
//		algowizFrame.setSize(new java.awt.Dimension(880, 780));
		algowizFrame.trackChanges("AlgoEditFrame");
		return algowizFrame;
	}
	
	/**
	 * Call with true if the application is to exit when the frame is closed.
	 * 
	 * @param tf
	 *            true if the application is to exit when the frame is closed.
	 */
	public void setExitOnClose(boolean tf)
	{
		exitOnClose = tf;
	}
	
	/** Get the frame used here */
	public TopFrame getFrame()
	{
		return algowizFrame;
	}
	

	/**
	 * This method initializes topPanel
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getTopPanel()
	{
		if (topPanel == null)
		{
			topPanel = new JPanel();
			topPanel.setLayout(new GridBagLayout());

			topPanel.add(getNorthPanel(),
				new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(5, 5, 5, 5), 0, 0));

			topPanel.add(getCommentsPanel(),
				new GridBagConstraints(0, 1, 2, 1, 1.0, 0.2,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(2, 2, 2, 2), 0, 80));

			topPanel.add(getInputParmPanel(),
				new GridBagConstraints(0, 2, 2, 1, 1.0, 0.05,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(2, 2, 2, 2), 0, 40));

			topPanel.add(getOutputParmPanel(),
				new GridBagConstraints(0, 3, 1, 1, 0.3, 0.05,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(2, 2, 2, 2), 0, 40));

			topPanel.add(getPropPanel(),
				new GridBagConstraints(1, 3, 1, 1, 0.7, 0.05,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(2, 2, 2, 2), 100, 40));

			topPanel.add(getCodePanel(),
				new GridBagConstraints(0, 4, 2, 1, 1.0, 0.7,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(2, 2, 2, 2), 0, 80));
		}
		return topPanel;
	}

	/**
	 * This method initializes northPanel with the algorithm name, algorithm type,
	 * java class name, and java package. system.getproperty
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getNorthPanel()
	{
		if (northPanel == null)
		{
			GridBagConstraints algoNameConstraints = 
				new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(5, 15, 5, 2), 0, 0);

			GridBagConstraints algoFieldConstraints = 
				new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
					GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
					new Insets(5, 0, 5, 0), 0, 0);

			GridBagConstraints algoTypeLabelConstraints = 
				new GridBagConstraints(2, 0, 1, 1, 0, 0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 15, 5, 2), 0, 0);

			GridBagConstraints typeComboConstraints = 
				new GridBagConstraints(3, 0, 1, 1, 1.0, 0,
					GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
					new Insets(5, 0, 5, 15), 0, 0);

			GridBagConstraints classNameLabelConstraints = 
				new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 15, 5, 2), 0, 0);

			GridBagConstraints classFieldConstraints = 
				new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
				new Insets(5, 0, 5, 0), 0, 0);

			GridBagConstraints packageLabelConstraints = 
				new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 15, 5, 2), 0, 0);

			GridBagConstraints packageFieldConstraints = 
				new GridBagConstraints(3, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
				new Insets(5, 0, 5, 15), 0, 0);

			northPanel = new JPanel();
			northPanel.setLayout(new GridBagLayout());
			northPanel.add(algorithmNameLabel, algoNameConstraints);
			algorithmField = new JTextField();
			algorithmField.setToolTipText(
				labels.getString("AlgorithmWizard.algorithmNameTT"));
			northPanel.add(algorithmField, algoFieldConstraints);
			northPanel.add(classNameLabel, classNameLabelConstraints);
			northPanel.add(algorithmTypeLabel, algoTypeLabelConstraints);
			classField = new JTextField();
			classField.setToolTipText(
				labels.getString("AlgorithmWizard.javaClassNameTT"));
			northPanel.add(classField, classFieldConstraints);

			String algoTypes[] = new String[AWAlgoType.values().length];
			for(int i=0; i<AWAlgoType.values().length; i++)
				algoTypes[i] = AWAlgoType.values()[i].getDisplayName();
			typeCombo = new JComboBox(algoTypes);
			typeCombo.addActionListener(
				new ActionListener() 
				{
					public void actionPerformed(ActionEvent e) 
					{
						doAlgoTypeSelected(); 
					}
				});

			northPanel.add(typeCombo, typeComboConstraints);
			northPanel.add(packageLabel, packageLabelConstraints);

			northPanel.add(extendsLabel,
				new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(5, 15, 5, 2), 0, 0));
			packageField = new JTextField();
			northPanel.add(extendsField,
				new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0,
					GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
					new Insets(5, 0, 5, 0), 0, 0));

			northPanel.add(implementsLabel,
				new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(5, 15, 5, 2), 0, 0));
			packageField = new JTextField();
			northPanel.add(implementsField,
				new GridBagConstraints(3, 3, 1, 1, 1.0, 0.0,
					GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
					new Insets(5, 0, 5, 15), 0, 0));

			packageField.setToolTipText(
					labels.getString("AlgorithmWizard.javaPackageTT"));
			northPanel.add(packageField, packageFieldConstraints);
		}
		return northPanel;
	}

	private JPanel getCommentsPanel()
	{
		if (Comments == null)
		{
			Comments = new JPanel();
			Comments.setLayout(new BorderLayout());

			Comments.setBorder(javax.swing.BorderFactory.createTitledBorder(
					javax.swing.BorderFactory.createLineBorder(
							java.awt.Color.gray, 2), 
							labels.getString("AlgorithmWizard.commentsLabel"),
					javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
					javax.swing.border.TitledBorder.DEFAULT_POSITION,
					new java.awt.Font("Dialog", java.awt.Font.BOLD, 12),
					new java.awt.Color(51, 51, 51)));

			commentsArea = new JTextArea();
			commentsArea.setWrapStyleWord(true);
			commentsArea.setToolTipText(
					labels.getString("AlgorithmWizard.commentsTT"));
			commentsArea.setLineWrap(true);
			commentsArea.setTabSize(4);
			Font oldfont = commentsArea.getFont();
			Font newfont = new Font("Monospaced",Font.PLAIN,oldfont.getSize());
			commentsArea.setFont(newfont);

			commentsScrollPane = new JScrollPane();
			commentsScrollPane.setHorizontalScrollBarPolicy(
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			commentsScrollPane.setViewportView(getCommentsArea());
			Comments.add(commentsScrollPane, java.awt.BorderLayout.CENTER);
		}
		return Comments;
	}

	/**
	 * This method initializes jTextArea
	 * 
	 * @return javax.swing.JTextArea
	 */
	private JTextArea getCommentsArea()
	{
		if (commentsArea == null)
		{
			commentsArea = new JTextArea();
			commentsArea.setWrapStyleWord(true);
			commentsArea.setToolTipText(
					labels.getString("AlgorithmWizard.commentsTT"));
			commentsArea.setLineWrap(true);
			commentsArea.setTabSize(4);
			Font oldfont = commentsArea.getFont();
			Font newfont = new Font("Monospaced",Font.PLAIN,oldfont.getSize());
			commentsArea.setFont(newfont);
			// Min size is 6 text lines:
			commentsArea.setMinimumSize(
				new Dimension(200, 6 * newfont.getSize()));
		}
		return commentsArea;
	}

	protected JPanel getInputParmPanel()
	{
		if (inputParmPanel == null)
		{
			inputParmPanel = new JPanel();
			inputParmPanel.setLayout(new BorderLayout());
			inputParmPanel.setSize(new java.awt.Dimension(60, 87));
			inputParmPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(
					javax.swing.BorderFactory.createLineBorder(
						java.awt.Color.gray, 2), 
						labels.getString("AlgorithmWizard.inputTimeSeries"),
					javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
					javax.swing.border.TitledBorder.DEFAULT_POSITION,
					new java.awt.Font("Dialog", java.awt.Font.BOLD, 12),
					new java.awt.Color(51, 51, 51)));

			AddEditDeletePanel buttonPanel = new AddEditDeletePanel(
				new ActionListener() 
				{
					public void actionPerformed(ActionEvent e) { doAddInput(); }
				},
				new ActionListener() 
				{
					public void actionPerformed(ActionEvent e) { doEditInput();}
				},
				new ActionListener() 
				{
					public void actionPerformed(ActionEvent e) 
					{ doDeleteInput();}
				});
			inputParmPanel.add(buttonPanel, java.awt.BorderLayout.EAST);
			inputParmPanel.add(getInputParmScrollPane(), BorderLayout.CENTER);
		}
		return inputParmPanel;
	}

	/**
	 * method that creates a new message popup for retrieving input for adding
	 * an input time series
	 */
	public void doAddInput()
	{
		InputTimeSeries its = new InputTimeSeries("", "double", "i");
		InputTimeSeriesDialog dlg = new InputTimeSeriesDialog(its, this);
		getFrame().launchDialog(dlg);
		if (dlg.isOK())
			inputTableModel.add(its);
	}

	/**
	 * method that creates a new message popup for deleting an element in the
	 * input time series list
	 * 
	 */
	public void doDeleteInput()
	{
		int idx = getInputParmTable().getSelectedRow();
		if (idx >= 0)
			inputTableModel.deleteInputTimeSeries(idx);
		else
		{
			showError(
				labels.getString("AlgorithmWizard.deleteInputTSTableErr"));
			return;
		}
	}

	/**
	 * method that creates a new message popup for retrieving input for editing
	 * an input time series
	 */
	public void doEditInput()
	{
		int idx = getInputParmTable().getSelectedRow();
		if (idx < 0)
		{
			showError(labels.getString("AlgorithmWizard.editInputTSTableErr"));
			return;
		}

		InputTimeSeries its = (InputTimeSeries)inputTableModel.getRowObject(idx);
		InputTimeSeriesDialog dlg = new InputTimeSeriesDialog(its, this);
		getFrame().launchDialog(dlg);
		if (dlg.isOK())
			inputTableModel.fireTableDataChanged();
		dlg.dispose();
	}

	/**
	 * method returning the input time series class of the selected item in the
	 * table.
	 * 
	 * @return time series
	 */
//	private InputTimeSeries getSelectedInputTimeSeries()
//	{
//		int idx = getInputParmTable().getSelectedRow();
//		if (idx >= 0)
//			return inputTableModel.getInputTimeSeries(idx);
//		else
//			return null;
//	}

	private JScrollPane getInputParmScrollPane()
	{
		if (inputParmScrollPane == null)
		{
			inputParmScrollPane = new JScrollPane();
			inputParmScrollPane.setViewportView(getInputParmTable());
		}
		return inputParmScrollPane;
	}

	protected JTable getInputParmTable()
	{
		if (inputTableModel == null)
		{
			inputTableModel = new InputTimeSeriesTableModel();
			((InputTimeSeriesTableModel) inputTableModel).fill();
			inputParmTable = new SortingListTable(inputTableModel,
				inputTableModel.columnWidths);
		}
		return inputParmTable;
	}

	private JPanel getCodePanel()
	{
		if (codePanel == null)
		{
			codePanel = new JPanel();
			codePanel.setLayout(new BorderLayout());
			codePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(
					javax.swing.BorderFactory.createLineBorder(
						java.awt.Color.gray, 2), 
						labels.getString("AlgorithmWizard.codeSegments"),
					javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
					javax.swing.border.TitledBorder.DEFAULT_POSITION,
					new java.awt.Font("Dialog", java.awt.Font.BOLD, 12),
					new java.awt.Color(51, 51, 51)));
			codePanel.setSize(new java.awt.Dimension(Short.MAX_VALUE, 800));
			codePanel.setPreferredSize(new java.awt.Dimension(Short.MAX_VALUE,
					800));
			codePanel.setMaximumSize(new java.awt.Dimension(Short.MAX_VALUE,
					800));
			codePanel.add(getCodeSectionTabbedPane(), java.awt.BorderLayout.CENTER);
		}
		return codePanel;
	}

	/**
	 * This method initializes codeSectionTabbedPane
	 * 
	 * @return javax.swing.JTabbedPane
	 */
	private JTabbedPane getCodeSectionTabbedPane()
	{
		if (codeSectionTabbedPane == null)
		{
			codeSectionTabbedPane = new JTabbedPane();
			codeSectionTabbedPane.addTab(
					labels.getString("AlgorithmWizard.imports"),
				null, getImportsPane(),
				labels.getString("AlgorithmWizard.importsTT"));
			codeSectionTabbedPane.addTab(
				labels.getString("AlgorithmWizard.classVarsMethods"), 
				null, getLocalVarsAreaPane(), 
				labels.getString("AlgorithmWizard.classVarsMethodsTT"));
			codeSectionTabbedPane.addTab(
				labels.getString("AlgorithmWizard.oneTimeInit"), null, 
				getOneTimeInitPane(), 
				labels.getString("AlgorithmWizard.oneTimeInitTT"));
			codeSectionTabbedPane.addTab(
				labels.getString("AlgorithmWizard.beforeIterating"), null,
				getBeforeTimeSlicePane(), 
				labels.getString("AlgorithmWizard.beforeIteratingTT"));
			codeSectionTabbedPane.addTab(
				labels.getString("AlgorithmWizard.timeSlice"), null, 
				getTimeSlicePanel(), 
				labels.getString("AlgorithmWizard.timeSliceTT"));
			codeSectionTabbedPane.addTab(
				labels.getString("AlgorithmWizard.afterIterating"), null,
				getAfterTimeSlicePanel(), 
				labels.getString("AlgorithmWizard.afterIteratingTT"));
		}
		return codeSectionTabbedPane;
	}

	private JScrollPane getImportsPane()
	{
		importsArea = new JTextArea();
		importsArea.setTabSize(4);
		Font oldfont = importsArea.getFont();
		importsArea.setFont(
			new Font("Monospaced", Font.PLAIN, oldfont.getSize()));
		JScrollPane importsPane = new JScrollPane();
		importsPane.setViewportView(importsArea);
		return importsPane;
	}

	/**
	 * This method initializes localVarsPane
	 * 
	 * @return javax.swing.JScrollPane
	 */
	private JScrollPane getLocalVarsAreaPane()
	{
		if (localVarsPane == null)
		{
			localVarsPane = new JScrollPane();
			localVarsPane.setViewportView(getLocalVarsArea());
		}
		return localVarsPane;
	}

	/**
	 * This method initializes jTextArea1
	 * 
	 * @return javax.swing.JTextArea
	 */
	private JTextArea getLocalVarsArea()
	{
		if (localVarsArea == null)
		{
			localVarsArea = new JTextArea();
			localVarsArea.setTabSize(4);
			Font oldfont = localVarsArea.getFont();
			localVarsArea.setFont(
				new Font("Monospaced", Font.PLAIN, oldfont.getSize()));
		}
		return localVarsArea;
	}

	/**
	 * This method initializes oneTimeInitAreaPane
	 * 
	 * @return javax.swing.JScrollPane
	 */
	private JScrollPane getOneTimeInitPane()
	{
		if (oneTimeInitAreaPane == null)
		{
			oneTimeInitAreaPane = new JScrollPane();
			oneTimeInitAreaPane.setViewportView(getOneTimeInit());
		}
		return oneTimeInitAreaPane;
	}

	/**
	 * This method initializes jTextField
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextArea getOneTimeInit()
	{
		if (oneTimeInitArea == null)
		{
			oneTimeInitArea = new JTextArea();
			oneTimeInitArea.setTabSize(4);
			Font oldfont = oneTimeInitArea.getFont();
			oneTimeInitArea.setFont(
				new Font("Monospaced", Font.PLAIN, oldfont.getSize()));
		}
		return oneTimeInitArea;
	}

	/**
	 * This method initializes beforeTimeSlicePane
	 * 
	 * @return javax.swing.JScrollPane
	 */
	private JScrollPane getBeforeTimeSlicePane()
	{
		if (beforeTimeSlicePane == null)
		{
			beforeTimeSlicePane = new JScrollPane();
			beforeTimeSlicePane.setViewportView(getBeforeTimeSlice());
		}
		return beforeTimeSlicePane;
	}

	/**
	 * This method initializes jTextArea2
	 * 
	 * @return javax.swing.JTextArea
	 */
	private JTextArea getBeforeTimeSlice()
	{
		if (beforeTimeSliceArea == null)
		{
			beforeTimeSliceArea = new JTextArea();
			beforeTimeSliceArea.setTabSize(4);
			Font oldfont = beforeTimeSliceArea.getFont();
			beforeTimeSliceArea.setFont(
				new Font("Monospaced", Font.PLAIN, oldfont.getSize()));
		}
		return beforeTimeSliceArea;
	}

	private JScrollPane getTimeSlicePanel()
	{
		if (timeSlicePanel == null)
		{
			timeSlicePanel = new JScrollPane();
			timeSlicePanel.setViewportView(getTimeSlice());
		}
		return timeSlicePanel;
	}

	/**
	 * This method initializes jTextArea3
	 * 
	 * @return javax.swing.JTextArea
	 */
	private JTextArea getTimeSlice()
	{
		if (timeSliceArea == null)
		{
			timeSliceArea = new JTextArea();
			timeSliceArea.setTabSize(4);
			Font oldfont = timeSliceArea.getFont();
			timeSliceArea.setFont(
				new Font("Monospaced", Font.PLAIN, oldfont.getSize()));
		}
		return timeSliceArea;
	}

	private JScrollPane getAfterTimeSlicePanel()
	{
		if (afterTimeSlicePanel == null)
		{
			afterTimeSlicePanel = new JScrollPane();
			afterTimeSlicePanel.setViewportView(getAfterTimeSlice());
		}
		return afterTimeSlicePanel;
	}

	private JTextArea getAfterTimeSlice()
	{
		if (afterTimeSliceArea == null)
		{
			afterTimeSliceArea = new JTextArea();
			afterTimeSliceArea.setTabSize(4);
			Font oldfont = afterTimeSliceArea.getFont();
			afterTimeSliceArea.setFont(
				new Font("Monospaced", Font.PLAIN, oldfont.getSize()));
		}
		return afterTimeSliceArea;
	}


	@SuppressWarnings("serial")
	private void makeMenus()
	{
		menuBar = new JMenuBar();

		JMenu fileMenu = new JMenu(genericLabels.getString("file"));

		Action action = 
			new AbstractAction(
					labels.getString("AlgorithmWizard.openJavaMenuLabel"))
			{
				public void actionPerformed(ActionEvent evt) { doOpen(); }
			};
		JMenuItem menuOpen = new JMenuItem(action);
		menuOpen.setToolTipText(
				labels.getString("AlgorithmWizard.menuOpenTT"));
		fileMenu.add(menuOpen);


		action = 
			new AbstractAction(
					labels.getString("AlgorithmWizard.newMenuLabel"))
			{
				public void actionPerformed(ActionEvent evt) { doNew(); }
			};
		JMenuItem menuNew = new JMenuItem(action);
		menuNew.setToolTipText(
				labels.getString("AlgorithmWizard.menuNewTT"));
		fileMenu.add(menuNew);

		action = 
			new AbstractAction(
					labels.getString("AlgorithmWizard.saveJavaMenuLabel"))
			{
				public void actionPerformed(ActionEvent evt) { doSave(); }
			};
		JMenuItem menuSave = new JMenuItem(action);
		menuSave.setToolTipText(
				labels.getString("AlgorithmWizard.menuSaveTT"));
		fileMenu.add(menuSave);

		action = 
			new AbstractAction(
					labels.getString("AlgorithmWizard.saveJavaAsMenuLabel"))
			{
				public void actionPerformed(ActionEvent evt) { doSaveAs(); }
			};
		JMenuItem menuSaveAs = new JMenuItem(action);
		menuSaveAs.setToolTipText(
				labels.getString("AlgorithmWizard.menuSaveAsTT"));
		fileMenu.add(menuSaveAs);

		fileMenu.addSeparator();

		action = 
			new AbstractAction(genericLabels.getString("exit"))
			{
				public void actionPerformed(ActionEvent evt) { doExit(); }
			};
		JMenuItem menuExit = new JMenuItem(action);
		fileMenu.add(menuExit);

		menuBar.add(fileMenu);

		JMenu compileMenu = new JMenu(
				labels.getString("AlgorithmWizard.compileMenuLabel"));
		action = 
			new AbstractAction(
					labels.getString("AlgorithmWizard.compileJarMenuLabel"))
			{
				public void actionPerformed(ActionEvent evt) { doCompile(); }
			};
		JMenuItem menuTestCompile = new JMenuItem(action);
		menuTestCompile.setToolTipText(
				labels.getString("AlgorithmWizard.menuCompileJarTT"));
		compileMenu.add(menuTestCompile);

		menuBar.add(compileMenu);

		JMenu xmlMenu = new JMenu(
				labels.getString("AlgorithmWizard.xmlMenuLabel"));
		menuBar.add(xmlMenu);

		action = 
			new AbstractAction(
					labels.getString("AlgorithmWizard.saveAlgoXMLMenuLabel"))
			{
				public void actionPerformed(ActionEvent evt) { doSaveXml(); }
			};
		JMenuItem menuSaveXML = new JMenuItem(action);
		menuSaveXML.setToolTipText(
			labels.getString("AlgorithmWizard.menuSaveXMLTT"));
		xmlMenu.add(menuSaveXML);

		action = 
			new AbstractAction(
					labels.getString("AlgorithmWizard.overlayAlgoXMLMenuLabel"))
			{
				public void actionPerformed(ActionEvent evt) { doLoadXml(); }
			};
		JMenuItem menuLoadXML = new JMenuItem(action);
		menuLoadXML.setToolTipText(
			labels.getString("AlgorithmWizard.menuLoadXMLTT"));
		xmlMenu.add(menuLoadXML);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
		throws Exception
	{
		AlgorithmWizard app = new AlgorithmWizard();
		app.execute(args);
	}

	/** Have to overload execute() -- we don't want to connect to DB. */
	public void execute(String args[])
		throws Exception
	{
		addCustomArgs(cmdLineArgs);
		parseArgs(args);
		runApp();
	}

	public void runApp()
	{
		getMyLabelDescriptions();

		TopFrame algowizFrame = initTopFrame();
//		algowizFrame.centerOnScreen();
		algowizFrame.setVisible(true);
	}

	protected JPanel getPropPanel()
	{
		if (propPanel == null)
		{
			propPanel = new JPanel(new BorderLayout());
			propPanel.setBorder(
				javax.swing.BorderFactory.createTitledBorder(
					javax.swing.BorderFactory.createLineBorder(
						java.awt.Color.gray), 
						labels.getString("AlgorithmWizard.properties")));

			JPanel buttonPanel = new AddEditDeletePanel(
				new ActionListener() 
				{
					public void actionPerformed(ActionEvent e) { doAddProp(); }
				},
				new ActionListener() 
				{
					public void actionPerformed(ActionEvent e) { doEditProp(); }
				},
				new ActionListener() 
				{
					public void actionPerformed(ActionEvent e) {doDeleteProp();}
				});
			propPanel.add(buttonPanel, java.awt.BorderLayout.EAST);

			algoPropModel = new AlgoPropTableModel(this);
			algoPropTable = new SortingListTable(algoPropModel,
				algoPropModel.columnWidths);

			JScrollPane scrollPane = new JScrollPane();
			scrollPane.setViewportView(algoPropTable);
			propPanel.add(scrollPane, BorderLayout.CENTER);
		}
		return propPanel;
	}

	protected JPanel getOutputParmPanel()
	{
		if (outputParmPanel == null)
		{
			outputParmPanel = new JPanel(new GridBagLayout());
			outputParmPanel.setBorder(
				javax.swing.BorderFactory.createTitledBorder(
					javax.swing.BorderFactory.createLineBorder(
						java.awt.Color.gray, 2), 
						labels.getString("AlgorithmWizard.outputTimeSeries"),
					javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
					javax.swing.border.TitledBorder.DEFAULT_POSITION,
					new java.awt.Font("Dialog", java.awt.Font.BOLD, 12),
					new java.awt.Color(51, 51, 51)));

			AddEditDeletePanel buttonPanel = new AddEditDeletePanel(
				new ActionListener() 
				{
					public void actionPerformed(ActionEvent e) {doAddOutput();}
				},
				new ActionListener() 
				{
					public void actionPerformed(ActionEvent e) {doEditOutput();}
				},
				new ActionListener() 
				{
					public void actionPerformed(ActionEvent e) 
					{ doDeleteOutput();}
				});
			
			outputListModel = new DefaultListModel();
			outputParmList = new JList(outputListModel);

			JScrollPane outputParmScrollPane = new JScrollPane();
			outputParmScrollPane.setViewportView(outputParmList);

			outputParmPanel.add(outputParmScrollPane,
				new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, 
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(5, 5, 5, 5), 120, 80));

			outputParmPanel.add(buttonPanel,
				new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 
					GridBagConstraints.NORTH, GridBagConstraints.NONE,
					new Insets(5, 5, 5, 5), 0, 0));
			
			
			JLabel aggregateLabel = new JLabel(
				labels.getString("AlgorithmWizard.aggreatePeriodVar"));
			aggregateCombo = new JComboBox();
			aggregateCombo.setEnabled(false);
			
			outputParmPanel.add(aggregateLabel,
				new GridBagConstraints(0, 1, 2, 1, 1.0, 0.0, 
					GridBagConstraints.CENTER, GridBagConstraints.NONE,
					new Insets(3, 5, 3, 5), 0, 0));
			outputParmPanel.add(aggregateCombo,
				new GridBagConstraints(0, 2, 2, 1, 1.0, 0.0, 
					GridBagConstraints.CENTER, GridBagConstraints.NONE,
					new Insets(3, 5, 3, 5), 120, 0));


		}
		return outputParmPanel;
	}

	public void doAddOutput()
	{
		String v = JOptionPane.showInputDialog(
			labels.getString("AlgorithmWizard.newOutputTSName"));
		if (v == null)
			return;
		try { v = validateName(v); }
		catch(BadTimeSeriesException ex)
		{
			showError(
					labels.getString("AlgorithmWizard.outputParamNameErr") 
					+ ex.getMessage());
			return;
		}
		if (outputListModel.contains(v))
		{
			showError(labels.getString("AlgorithmWizard.outputParamExistsErr")
					+ labels.getString("AlgorithmWizard.cannotAddLabel"));
			return;
		}
		if (inputTableModel.findByName(v) != null)
		{
			showError(labels.getString("AlgorithmWizard.inputParamExistsErr")
					+ labels.getString("AlgorithmWizard.cannotAddLabel"));
			return;
		}
		if (algoPropModel.findByName(v) != null)
		{
			showError(labels.getString("AlgorithmWizard.propertyNameExistsErr")
					+ labels.getString("AlgorithmWizard.cannotAddLabel"));
			return;
		}
		aggregateCombo.addItem(v);
		outputListModel.addElement(v);
	}

	public void doDeleteOutput()
	{
		int idx = outputParmList.getSelectedIndex();
		if (idx == -1)
		{
			showError(
				labels.getString("AlgorithmWizard.deleteOutputTSTableErr"));
			return;
		}
		String v = (String)outputParmList.getSelectedValue();
		aggregateCombo.removeItem(v);
		outputListModel.removeElementAt(idx);
	}

	public void doEditOutput()
	{
		int idx = outputParmList.getSelectedIndex();
		if (idx == -1)
		{
			showError(
				labels.getString("AlgorithmWizard.editOutputTSTableErr"));
			return;
		}
		String v = (String)outputParmList.getSelectedValue();
		v = JOptionPane.showInputDialog(
				labels.getString("AlgorithmWizard.outputParamNameErr"), v);
		if (v == null)
			return;
		try { v = validateName(v); }
		catch(BadTimeSeriesException ex)
		{
			showError(labels.getString("AlgorithmWizard.outputParamNameErr")
					+ ex.getMessage());
			return;
		}
		if (outputListModel.contains(v))
		{
			showError(labels.getString("AlgorithmWizard.outputParamExistsErr")
				+ labels.getString("AlgorithmWizard.cannotChangeLabel"));
			return;
		}
		if (inputTableModel.findByName(v) != null)
		{
			showError(labels.getString("AlgorithmWizard.inputParamExistsErr")
				+ labels.getString("AlgorithmWizard.cannotChangeLabel"));
			return;
		}
		if (algoPropModel.findByName(v) != null)
		{
			showError(labels.getString("AlgorithmWizard.propertyNameExistsErr")
				+ labels.getString("AlgorithmWizard.cannotChangeLabel"));
			return;
		}
		outputListModel.set(idx, v);
	}

	private void doSaveAs()
	{
		if (fileChooser.showSaveDialog(algowizFrame) == JFileChooser.APPROVE_OPTION)
		{
			theFile = fileChooser.getSelectedFile();
			if (theFile != null)
			{
				try { algoWriter.saveToTheFile(theFile, this); }
				catch(AlgoIOException ex)
				{
					showError(
						labels.getString("AlgorithmWizard.errorSavingAlgoMsg")
						+ ex);
				}
			}
		}
	}

	private void doSave()
	{
		if (theFile == null)
			doSaveAs();
		else
		{
			try { algoWriter.saveToTheFile(theFile, this); }
			catch(AlgoIOException ex)
			{
				showError(
						labels.getString("AlgorithmWizard.errorSavingAlgoMsg")
						+ ex);
			}
		}
	}
	
	private void doCompile()
	{
		if (!compileDialog.isVisible())
		{
			compileDialog.clear();
			getFrame().launchDialog(compileDialog);
		}
		else
			compileDialog.toFront();
	}

	private void doNew()
	{
		theFile = null;
		URL myurl = ClassLoader.getSystemResource(templateFile);
		if (myurl == null)
		{
			showError(LoadResourceBundle.sprintf(
				labels.getString("AlgorithmWizard.cannotFindTemplateJarErr"),
				templateFile));
			return;
		}
		try
		{
			algoReader.readAlgo(myurl.openStream(), "Template", this);
			
			if (algoReader.getNumParseErrors() > 0)
				showError(LoadResourceBundle.sprintf(
				labels.getString("AlgorithmWizard.problemsParsingErr"),
				myurl.toString())
				+ algoReader.getParseErrors());
			setAlgorithmName("");
			setJavaClassName("");
			setJavaPackage("");
			setExtends("decodes.tsdb.algo.AW_AlgorithmBase");
			setImplements("");
		}
		catch(IOException ex)
		{
			showError(LoadResourceBundle.sprintf(
					labels.getString("AlgorithmWizard.cannotOpenFileErr"),
					myurl.toString()) + ex);
		}
	}

	private void doOpen()
	{
		if (fileChooser.showOpenDialog(algowizFrame) == JFileChooser.APPROVE_OPTION)
		{
			doNew();
			theFile = fileChooser.getSelectedFile();
			try
			{
				algoReader.readAlgo(new FileInputStream(theFile), 
					theFile.getName(), this);
				if (algoReader.getNumParseErrors() > 0)
					showError(LoadResourceBundle.sprintf(
						labels.getString("AlgorithmWizard.problemsParsingErr"),
						theFile.getName())
						+ algoReader.getParseErrors());
			}
			catch(IOException ex)
			{
				showError(LoadResourceBundle.sprintf(
						labels.getString("AlgorithmWizard.cannotOpenFileErr"),
						theFile.getPath()) + ex);
			}
		}
	}

	private void doAddProp()
	{
		AlgoProp ap = new AlgoProp("", "i", "");
		PropDialog dlg = new PropDialog(ap, this);
		getFrame().launchDialog(dlg);
		if (dlg.isOK())
			addAlgoProp(ap);
	}

	private void doEditProp()
	{
		int idx = algoPropTable.getSelectedRow();
		if (idx < 0)
		{
			showError(
				labels.getString("AlgorithmWizard.editPropertyTableErr"));
			return;
		}

		AlgoProp ap = (AlgoProp)algoPropModel.getRowObject(idx);
		PropDialog dlg = new PropDialog(ap, this);
		getFrame().launchDialog(dlg);
		if (dlg.isOK())
			algoPropModel.fireTableDataChanged();
		dlg.dispose();
	}

	private void doDeleteProp()
	{
		int idx = algoPropTable.getSelectedRow();
		if (idx >= 0)
			algoPropModel.deleteAt(idx);
		else
		{
			showError(
				labels.getString("AlgorithmWizard.deletePropertyTableErr"));
			return;
		}
	}

//	private String getTypeCode(String code)
//	{
//		if (code.equals("i"))
//		{
//			return "i: Simple Output";
//		}
//		else if (code.equals("id"))
//		{
//			return "id: Delta with implicit period";
//		}
//		else if (code.equals("idh"))
//		{
//			return "idh: Hourly Delta";
//		}
//		else if (code.equals("idd"))
//		{
//			return "idd: Daily Delta";
//		}
//		else if (code.equals("idld"))
//		{
//			return "idld: Delta from end of last day";
//		}
//		else if (code.equals("idlm"))
//		{
//			return "idlm: Delta from end of last month";
//		}
//		else if (code.equals("idly"))
//		{
//			return "idly: Delta from end of last year";
//		}
//		else
//		{
//			return "idlwy: Delta from end of last water-year";
//		}
//	}

	/**
	 * From AlgoData interface
	 */
	public String getAlgorithmName()
	{
		return algorithmField.getText().trim();
	}

	/**
	 * From AlgoData interface
	 */
	public void setAlgorithmName(String name)
	{
		algorithmField.setText(name);
	}

	/**
	 * From AlgoData interface
	 */
	public AWAlgoType getAlgorithmType()
	{
		String t = (String)typeCombo.getSelectedItem();
		if (t.charAt(0) == 'T')
			return AWAlgoType.TIME_SLICE;
		else if (t.charAt(0) == 'A')
			return AWAlgoType.AGGREGATING;
		else if (t.charAt(0) == 'R')
			return AWAlgoType.RUNNING_AGGREGATE;
		else return AWAlgoType.TIME_SLICE;
	}

	/**
	 * From AlgoData interface
	 */
	public void setAlgorithmType(AWAlgoType type)
	{
		if (type == AWAlgoType.TIME_SLICE)
			typeCombo.setSelectedIndex(0);
		else if (type == AWAlgoType.AGGREGATING)
			typeCombo.setSelectedIndex(1);
		else if (type == AWAlgoType.RUNNING_AGGREGATE)
			typeCombo.setSelectedIndex(2);
	}

	/**
	 * From AlgoData interface
	 */
	public String getJavaClassName()
	{
		return classField.getText().trim();
	}

	/**
	 * From AlgoData interface
	 */
	public void setJavaClassName(String name)
	{
		classField.setText(name);
	}

	/**
	 * From AlgoData interface
	 */
	public String getJavaPackage()
	{
		return packageField.getText().trim();
	}

	/**
	 * From AlgoData interface
	 */
	public void setJavaPackage(String name)
	{
		packageField.setText(name);
	}


	/**
	 * From AlgoData interface
	 */
	public String getComment()
	{
		return commentsArea.getText();
	}

	/**
	 * From AlgoData interface
	 */
	public void setComment(String s)
	{
		commentsArea.setText(s);
	}


	/**
	 * From AlgoData interface
	 */
	public ArrayList<InputTimeSeries> getAllInputTimeSeries()
	{
		return inputTableModel.myvector;
	}

	/**
	 * From AlgoData interface
	 */
	public void clearInputTimeSeries()
	{
		inputTableModel.clear();
	}

	/**
	 * From AlgoData interface
	 */
	public void addInputTimeSeries(InputTimeSeries its)
	{
		inputTableModel.add(its);
	}


	/**
	 * From AlgoData interface
	 */
	public ArrayList<String> getAllOutputTimeSeries()
	{
		ArrayList<String> ret = new ArrayList<String>();
		for(Enumeration en = outputListModel.elements(); en.hasMoreElements(); )
			ret.add((String)en.nextElement());
		return ret;
	}

	/**
	 * From AlgoData interface
	 */
	public void clearOutputTimeSeries()
	{
		outputListModel.clear();
	}

	/**
	 * From AlgoData interface
	 */
	public void addOutputTimeSeries(String ots)
	{
		outputListModel.addElement(ots);
		aggregateCombo.addItem(ots);
	}


	/**
	 * From AlgoData interface
	 */
	public ArrayList<AlgoProp> getAllAlgoProps()
	{
		return algoPropModel.theProps;
	}

	/**
	 * From AlgoData interface
	 */
	public void clearAlgoProps()
	{
		algoPropModel.clear();
	}

	/**
	 * From AlgoData interface
	 */
	public void addAlgoProp(AlgoProp ap)
	{
		algoPropModel.add(ap);
	}

	/**
	 * From AlgoData interface
	 */
	public String getImportsCode()
	{
		return importsArea.getText();
	}

	/**
	 * From AlgoData interface
	 */
	public void setImportsCode(String s)
	{
		importsArea.setText(s);
	}

	/**
	 * From AlgoData interface
	 */
	public String getLocalVarsCode()
	{
		return localVarsArea.getText();
	}

	/**
	 * From AlgoData interface
	 */
	public void setLocalVarsCode(String s)
	{
		localVarsArea.setText(s);
	}

	/**
	 * From AlgoData interface
	 */
	public String getOneTimeInitCode()
	{
		return oneTimeInitArea.getText();
	}

	/**
	 * From AlgoData interface
	 */
	public void setOneTimeInitCode(String s)
	{
		oneTimeInitArea.setText(s);
	}

	/**
	 * From AlgoData interface
	 */
	public String getBeforeIterCode()
	{
		return beforeTimeSliceArea.getText();
	}

	/**
	 * From AlgoData interface
	 */
	public void setBeforeIterCode(String s)
	{
		beforeTimeSliceArea.setText(s);
	}

	/**
	 * From AlgoData interface
	 */
	public String getTimeSliceCode()
	{
		return timeSliceArea.getText();
	}

	/**
	 * From AlgoData interface
	 */
	public void setTimeSliceCode(String s)
	{
		timeSliceArea.setText(s);
	}


	/**
	 * From AlgoData interface
	 */
	public String getAfterIterCode()
	{
		return afterTimeSliceArea.getText();
	}

	/**
	 * From AlgoData interface
	 */
	public void setAfterIterCode(String s)
	{
		afterTimeSliceArea.setText(s);
	}

	public String getExtends()
	{
		return extendsField.getText().trim();
	}

	public void setExtends(String ext)
	{
		extendsField.setText(ext);
	}

	public String getImplements()
	{
		return implementsField.getText().trim();
	}

	public void setImplements(String ext)
	{
		implementsField.setText(ext);
	}

	public String getAggPeriodOutput()
	{
		return (String)aggregateCombo.getSelectedItem();
	}

	public void setAggPeriodOutput(String s)
	{
		aggregateCombo.setSelectedItem(s);
	}

	public void showError(String msg)
	{
		getFrame().showError(msg);
	}

	/**
	 * Make sure the passed name is valid as a Java variable.
	 * @param nm the name to validate.
	 * @throws BadTimeSeriesException with problem description if not valid.
	 * @return the valid name (may be trimmed from original)
	 */
	public String validateName(String nm)
		throws BadTimeSeriesException
	{
		nm = nm.trim();
		if (nm.length() == 0)
			throw new BadTimeSeriesException(
					labels.getString("AlgorithmWizard.notBlankErr"));
		
		String syntax = 
			labels.getString("AlgorithmWizard.validateNameErr");

		if (!Character.isLetter(nm.charAt(0)))
			throw new BadTimeSeriesException(syntax);

		for(int i=0; i<nm.length(); i++)
		{
			char c = nm.charAt(i);
			if (!Character.isLetterOrDigit(c) && c != '_')
				throw new BadTimeSeriesException(syntax);
		}
		return nm;
	}

	private void doAlgoTypeSelected()
	{
		String t = (String)typeCombo.getSelectedItem();
		aggregateCombo.setEnabled(!t.startsWith("T"));
	}

	private void doExit()
	{
		algowizFrame.dispose();
		if (exitOnClose)
			System.exit(0);
	}

	private void doSaveXml()
	{
		File theFile = fileChooser.getSelectedFile();
		if (theFile != null)
		{
			String p = theFile.getPath();
			int idx = p.lastIndexOf(".");
			if (idx > 0)
				p = p.substring(0, idx);
			p = p + ".xml";
			fileChooser.setSelectedFile(new File(p));
		}
		if (fileChooser.showSaveDialog(algowizFrame) != JFileChooser.APPROVE_OPTION
		 || (theFile = fileChooser.getSelectedFile()) == null)
			return;

		// First build a DbCompAlgorithm with this meta-data.
		DbCompAlgorithm dca = new DbCompAlgorithm(Constants.undefinedId,
			getAlgorithmName(), 
			getJavaPackage() + "." + getJavaClassName(),
			getComment());

		for(InputTimeSeries its : getAllInputTimeSeries())
			dca.addParm(new DbAlgoParm(its.roleName, its.roleTypeCode));

		for(String ots : getAllOutputTimeSeries())
			dca.addParm(new DbAlgoParm(ots, "o"));

		for(AlgoProp ap : getAllAlgoProps())
		{
			String v = ap.defaultValue;
			if (v == null)
				v = "";
			if (v.startsWith("\""))
				v = v.substring(1);
			if (v.endsWith("\""))
				v = v.substring(0, v.length()-1);
			dca.setProperty(ap.name, v);
		}

		ArrayList<CompMetaData> metadata = new ArrayList<CompMetaData>();
		metadata.add(dca);
		CompXio compXio = new CompXio("algoedit", theDb);
		try { compXio.writeFile(metadata, theFile.getPath()); }
		catch(Exception ex)
		{
			String msg = LoadResourceBundle.sprintf(
					labels.getString("AlgorithmWizard.cannotSaveToErrMsg"),
					theFile.getPath()) + ex;
			showError(msg);
		}
	}

	private void doLoadXml()
	{
		File theFile = null;
		if (fileChooser.showOpenDialog(algowizFrame) != JFileChooser.APPROVE_OPTION
		 || (theFile = fileChooser.getSelectedFile()) == null)
			return;

		CompXio compXio = new CompXio("algoedit", theDb);
		try
		{
			ArrayList<CompMetaData> metadata = 
				compXio.readFile(theFile.getPath());
			DbCompAlgorithm dca = null;
			for(CompMetaData cmd : metadata)
				if (cmd instanceof DbCompAlgorithm)
				{
					dca = (DbCompAlgorithm)cmd;
					break;
				}
			if (dca == null)
			{
				
				String msg = LoadResourceBundle.sprintf(
						labels.getString(
						"AlgorithmWizard.compAlgoRecMissingErrMsg"),
						theFile.getPath());
				showError(msg);
				return;
			}
			setAlgorithmName(dca.getName());
			String ec = dca.getExecClass();
			int idx = ec.lastIndexOf(".");
			if (idx > 0)
			{
				setJavaClassName(ec.substring(idx+1));
				setJavaPackage(ec.substring(0, idx));
			}
			else
			{
				setJavaClassName(ec);
				setJavaPackage("");
			}
			setComment(dca.getComment());
			for(Iterator<DbAlgoParm> dit = dca.getParms(); dit.hasNext();)
			{
				DbAlgoParm dap = dit.next();
				String typ = dap.getParmType();
				if (typ != null && typ.toLowerCase().startsWith("o"))
					addOutputTimeSeries(dap.getRoleName());
				else
					addInputTimeSeries(
						new InputTimeSeries(dap.getRoleName(), "double", typ));
			}
			for(Enumeration en = dca.getPropertyNames(); en.hasMoreElements();)
			{
				String pname = (String)en.nextElement();
				addAlgoProp(
					new AlgoProp(pname, "double", dca.getProperty(pname)));
			}
		}
		catch(Exception ex)
		{
			String msg = LoadResourceBundle.sprintf(
					labels.getString(
					"AlgorithmWizard.cannotLoadFromErr"), 
					theFile.getPath()) + ex;
			showError(msg);
		}
	}
}
