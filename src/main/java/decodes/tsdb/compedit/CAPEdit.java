/*
*  $Id: CAPEdit.java,v 1.7 2020/05/07 13:55:13 mmaloney Exp $
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package decodes.tsdb.compedit;

import ilex.cmdline.BooleanToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.LoadResourceBundle;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ResourceBundle;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

import lrgs.gui.DecodesInterface;
import decodes.db.Site;
import decodes.gui.TopFrame;
import decodes.tsdb.*;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;
import decodes.util.DecodesSettings;


public class CAPEdit 
	extends TsdbAppTemplate
{
	static CAPEdit _instance = null;

	private TopFrame topFrame = null;  //  @jve:decl-index=0:visual-constraint="62,-15"
	private JPanel jContentPane = null;
	private JTabbedPane mainTab = null;
	private JPanel computationsPanel = null;
	private JPanel algorithmsPanel = null;
	JTabbedPane computationsTab = null;
	private JPanel processesPanel = null;
	JTabbedPane algorithmsTab = null;
	JTabbedPane processesTab = null;
	ProcessesListPanel processesListPanel = null;
	AlgorithmsListPanel algorithmsListPanel = null;
	ComputationsListPanel computationsListPanel = null;
	private BooleanToken noCompFilterToken;
	private boolean exitOnClose = true;
	
	public ResourceBundle genericDescriptions=null;
	public ResourceBundle compeditDescriptions=null;
	
	
	
	private String listLabel;
	private String algorithmsLabel;
	private String computationsLabel;
	private String processesLabel;
	private String titleLabel;

	public static CAPEdit instance()
	{
		if (_instance==null)
			_instance = new CAPEdit();
		if (_instance.genericDescriptions==null
			|| _instance.compeditDescriptions==null)
			_instance.setupMyLabelDescriptions();
		return _instance; 
	}

	public CAPEdit()
	{
		super("compedit.log");
		_instance = this;	
		exitOnClose = true;
	}
	
	/**
	 * This method adds a command line argument to allow
	 * the user to turn off the Db Computations list filter.
	 */
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		noCompFilterToken = new BooleanToken("L", 
			"Disable Computation List filter (default=enabled)", "",
			TokenOptions.optSwitch, false);
		cmdLineArgs.addToken(noCompFilterToken);
		appNameArg.setDefaultValue("compedit");
	}

	public void runApp( )
	{
		noExitAfterRunApp = true;
		setupMyLabelDescriptions();
		fillLabels();
		
		TopFrame frame = getTopFrame();
//		frame.centerOnScreen();
		frame.setVisible(true);
		
		computationsListPanel.doRefresh();
	}

	/**
	 * This method initializes topFrame	
	 * 	
	 * @return javax.swing.JFrame	
	 */
	public TopFrame getTopFrame() 
	{
		if (topFrame == null) 
		{
			topFrame = new TopFrame();
			topFrame.setSize(new java.awt.Dimension(900,740));
			topFrame.setTitle(titleLabel);
			//topFrame.setDefaultCloseOperation(topFrame.EXIT_ON_CLOSE);
//			topFrame.setContentPane(getJContentPane());
			getJContentPane();
			topFrame.setDefaultCloseOperation(
									WindowConstants.DO_NOTHING_ON_CLOSE);
			topFrame.addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent e)
				{
					close();
				}
			});
			topFrame.trackChanges("CompEditFrame");
		}
		return topFrame;
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
		return topFrame;
	}
	
	private void close()
	{
		topFrame.dispose();
		if (exitOnClose)
			System.exit(0);
	}

	/**
	 * This method initializes jContentPane	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJContentPane() 
	{
		if (jContentPane == null) {
			jContentPane = (JPanel)topFrame.getContentPane();
//			jContentPane = new JPanel();
			jContentPane.setLayout(new BorderLayout());
			jContentPane.add(getMainTab(), java.awt.BorderLayout.CENTER);
		}
		return jContentPane;
	}

	/**
	 * This method initializes jTabbedPane	
	 * 	
	 * @return javax.swing.JTabbedPane	
	 */
	private JTabbedPane getMainTab() 
	{
		if (mainTab == null) 
		{
			mainTab = new JTabbedPane();

			// Need to construct comps first because algo uses it to get count.
			JPanel compPanel = getComputationsPanel();
			JPanel algoPanel = getAlgorithmsPanel();
			mainTab.addTab(algorithmsLabel, null, algoPanel, null);
			mainTab.addTab(computationsLabel, null, compPanel, null);
			mainTab.addTab(processesLabel, null, getProcessesPanel(), null);
		}
		return mainTab;
	}

	/**
	 * This method initializes jPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getComputationsPanel()
	{
		if (computationsPanel == null) {
			computationsPanel = new JPanel();
			computationsPanel.setLayout(new BorderLayout());
			computationsPanel.add(getComputationsTab(), java.awt.BorderLayout.CENTER);
		}
		return computationsPanel;
	}

	/**
	 * This method initializes jPanel1	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getAlgorithmsPanel() {
		if (algorithmsPanel == null) {
			algorithmsPanel = new JPanel();
			algorithmsPanel.setLayout(new BorderLayout());
			algorithmsPanel.add(getAlgorithmsTab(), java.awt.BorderLayout.CENTER);
		}
		return algorithmsPanel;
	}

	/**
	 * This method initializes jTabbedPane1	
	 * 	
	 * @return javax.swing.JTabbedPane	
	 */
	JTabbedPane getComputationsTab() 
	{
		if (computationsTab == null) 
		{
			computationsTab = new JTabbedPane();
			
			computationsListPanel = 
				new ComputationsListPanel(theDb, 
					!noCompFilterToken.getValue(), false, topFrame);
			computationsTab.addTab(listLabel, null, 
				computationsListPanel, null);
		}
		return computationsTab;
	}


	/**
	 * This method initializes jPanel4	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getProcessesPanel() {
		if (processesPanel == null) {
			processesPanel = new JPanel();
			processesPanel.setLayout(new BorderLayout());
			processesPanel.add(getProcessesTab(), java.awt.BorderLayout.CENTER);
		}
		return processesPanel;
	}

	JTabbedPane getAlgorithmsTab() 
	{
		if (algorithmsTab == null) 
		{
			algorithmsListPanel = new AlgorithmsListPanel();
			algorithmsTab = new JTabbedPane();
			algorithmsTab.addTab(listLabel, null, algorithmsListPanel, null);
		}
		return algorithmsTab;
	}

	public JTabbedPane getProcessesTab() {
		if (processesTab == null) {
			processesTab = new JTabbedPane();
			processesListPanel = new ProcessesListPanel();
			processesTab.addTab(listLabel, null, processesListPanel, null);
		}
		return processesTab;
	}

	public TimeSeriesDb getTimeSeriesDb()
	{
		return theDb;
	}

	public static void main(String[] args)
		throws Exception
	{
		DecodesInterface.setGUI(true);
		CAPEdit myedit = new CAPEdit();
		myedit.execute(args);
	}
	
	public void setupMyLabelDescriptions()
	{
		DecodesSettings settings = DecodesSettings.instance();
		//Load the generic properties file - includes labels that are used
		//in multiple screens
		genericDescriptions = LoadResourceBundle.getLabelDescriptions(
				"decodes/resources/generic",
				settings.language);
		//Return the main label descriptions for Ts Edit App
		compeditDescriptions =  LoadResourceBundle.getLabelDescriptions(
				"decodes/resources/compedit",
				settings.language);
	}
	
	public void fillLabels()
	{
		listLabel = compeditDescriptions.getString("CAPEdit.List");
		algorithmsLabel = compeditDescriptions.getString("CAPEdit.Algorithms");
		computationsLabel = compeditDescriptions.getString("CAPEdit.Computations");
		processesLabel = compeditDescriptions.getString("CAPEdit.Processes");
		titleLabel = compeditDescriptions.getString("CAPEdit.Title");
	}
	
	@Override
	public void initDecodes()
		throws DecodesException
	{
		decodesDb.initializeForEditing();
	}

}
