package decodes.platwiz;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.util.ResourceBundle;
import java.util.Vector;

import decodes.gui.TopFrame;
import decodes.db.Database;

/**
The top-level frame for the platform wizard GUI.
*/
public class Frame1 extends TopFrame
{
	private static ResourceBundle genericLabels = null;
	private static ResourceBundle platwizLabels = null;
	JPanel contentPane;
	JLabel statusBar = new JLabel();
	BorderLayout borderLayout1 = new BorderLayout();
	JPanel topPanel = new JPanel();
	BorderLayout borderLayout2 = new BorderLayout();
	JPanel navButtonsPanel = new JPanel();
	JButton nextButton = new JButton();
	JButton prevButton = new JButton();
	JLabel jLabel1 = new JLabel();
	JButton quitButton = new JButton();
	FlowLayout flowLayout1 = new FlowLayout();
	JPanel panelsContainer = new JPanel();
	GridLayout gridLayout1 = new GridLayout();
	JPanel jPanel1 = new JPanel();
	JLabel panelTitle = new JLabel();

	//Array of panels in the wizard
	private Vector panels;
	private int curPanel;
	JPanel panelWrapper = new JPanel();
	BorderLayout borderLayout3 = new BorderLayout();
	JTextArea descriptionArea = new JTextArea();
	JLabel slideNumLabel = new JLabel();

	/** The StartPanel */
	StartPanel startPanel = null;

	/** Flag causing application to exit when this frame is closed. */
	public static boolean exitOnClose = true;

	/** default constructor. */
	public Frame1() 
	{
		genericLabels = PlatformWizard.getGenericLabels();
		platwizLabels = PlatformWizard.getPlatwizLabels();
		//enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		try {
			jbInit();
			initPanels();
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		// Default operation is to do nothing when user hits 'X' in upper
		// right to close the window. We will catch the closing event and
		// do the same thing as if user had hit Quit.
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(
			new WindowAdapter()
			{
				public void windowClosing(WindowEvent e)
				{
					quitButton_actionPerformed(null);
				}
			});

	}

	/** Component initialization */
	private void jbInit() throws Exception	{
		contentPane = (JPanel) this.getContentPane();
		contentPane.setLayout(borderLayout1);
		this.setSize(new Dimension(750, 652));//624, 652
		this.setTitle(platwizLabels.getString("frame1.frameTitle"));
		statusBar.setText(" ");
		topPanel.setDebugGraphicsOptions(0);
		topPanel.setLayout(borderLayout2);
		//nextButton.setMinimumSize(new Dimension(80, 23));
		nextButton.setPreferredSize(new Dimension(110, 23));
		nextButton.setText(platwizLabels.getString("frame1.next"));
		nextButton.addActionListener(new Frame1_nextButton_actionAdapter(this));
		prevButton.setEnabled(false);
		//prevButton.setMaximumSize(new Dimension(80, 23));
		//prevButton.setMinimumSize(new Dimension(80, 23));
		prevButton.setPreferredSize(new Dimension(110, 23));
		prevButton.setText(platwizLabels.getString("frame1.previous"));
		prevButton.addActionListener(new Frame1_prevButton_actionAdapter(this));
		jLabel1.setPreferredSize(new Dimension(80, 15));
		jLabel1.setText("");
		quitButton.setPreferredSize(new Dimension(80, 23));
		quitButton.setText(genericLabels.getString("quit"));
		quitButton.addActionListener(new Frame1_quitButton_actionAdapter(this));
		navButtonsPanel.setLayout(flowLayout1);
		flowLayout1.setAlignment(FlowLayout.RIGHT);
		flowLayout1.setHgap(10);
		panelsContainer.setLayout(gridLayout1);
		gridLayout1.setColumns(1);
		panelsContainer.setBorder(BorderFactory.createLoweredBevelBorder());
		jPanel1.setFont(new java.awt.Font("Dialog", 1, 14));
		jPanel1.setOpaque(true);
		jPanel1.setRequestFocusEnabled(true);
		jPanel1.setToolTipText("");
		panelTitle.setFont(new java.awt.Font("Dialog", 1, 14));
		panelTitle.setText(platwizLabels.getString("frame1.frameTitle"));
		panelWrapper.setLayout(borderLayout3);
		descriptionArea.setBackground(SystemColor.window);
		descriptionArea.setBorder(BorderFactory.createLineBorder(Color.black));
		descriptionArea.setMinimumSize(new Dimension(205, 60));
		descriptionArea.setPreferredSize(new Dimension(205, 60));
		descriptionArea.setEditable(false);
		descriptionArea.setText(
				platwizLabels.getString("frame1.panelDescHere"));
		descriptionArea.setLineWrap(true);
		descriptionArea.setWrapStyleWord(true);
		slideNumLabel.setText("(1/9)");
		contentPane.add(statusBar, BorderLayout.SOUTH);
		contentPane.add(topPanel, BorderLayout.CENTER);
		topPanel.add(navButtonsPanel, BorderLayout.SOUTH);
		navButtonsPanel.add(slideNumLabel, null);
		navButtonsPanel.add(prevButton, null);
		navButtonsPanel.add(nextButton, null);
		navButtonsPanel.add(jLabel1, null);
		navButtonsPanel.add(quitButton, null);
		topPanel.add(jPanel1,	BorderLayout.NORTH);
		jPanel1.add(panelTitle, null);
		topPanel.add(panelWrapper, BorderLayout.CENTER);
		panelWrapper.add(panelsContainer,	BorderLayout.CENTER);
		panelWrapper.add(descriptionArea, BorderLayout.NORTH);
	}

	//Overridden so we can exit when window is closed
	protected void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			;
		}
	}

	/**
	  Called when Next button is pressed.
	  @param e ignored
	*/
	void nextButton_actionPerformed(ActionEvent e)
	{
		for(int n = curPanel + 1; n < panels.size(); n++)
		{
			WizardPanel wp = (WizardPanel)panels.elementAt(n);
			if (!wp.shouldSkip())
			{
				switchPanel(n);
				return;
			}
		}
	}

	/**
	  Called when Previous button is pressed.
	  @param e ignored
	*/
	void prevButton_actionPerformed(ActionEvent e)
	{
		for(int n = curPanel - 1; n >= 0; n--)
		{
			WizardPanel wp = (WizardPanel)panels.elementAt(n);
			if (!wp.shouldSkip())
			{
				switchPanel(n);
				return;
			}
		}
	}

	/**
	  Called on Next or Previous buttons, switch from one panel to another.
	  Call the current panel's deactivate button to see if it's ok to leave.
	  Then switch and call the new panel's activate button.
	  @param n the number of the panel to switch to
	*/
	private void switchPanel(int n)
	{
		if (curPanel != -1)
		{
			JPanel jp = (JPanel)panels.elementAt(curPanel);
			try 
			{
				if (!((WizardPanel)jp).deactivate())
					return;
			}
			catch(PanelException ex)
			{
				System.err.println("Exception in deactivate: " + ex);
				ex.printStackTrace();
			}
			panelsContainer.setVisible(false);
			panelsContainer.remove(jp);
		}
		curPanel = n;
		JPanel jp = (JPanel)panels.elementAt(curPanel);
		try { ((WizardPanel)jp).activate(); }
		catch(PanelException ex)
		{
			System.err.println("Exception in activate: " + ex);
			ex.printStackTrace();
		}
		panelsContainer.add(jp);
		panelTitle.setText(((WizardPanel)jp).getPanelTitle());
		descriptionArea.setText(((WizardPanel)jp).getDescription());
		panelsContainer.setVisible(true);
		prevButton.setEnabled(curPanel > 0);
		nextButton.setEnabled(curPanel < panels.size()-1);
		slideNumLabel.setText("(" + (curPanel+1) + "/" + panels.size() + ")");
	}

	/**
	  Called when Quite button is pressed.
	  @param e ignored
	*/
	void quitButton_actionPerformed(ActionEvent e) 
	{
		if (!PlatformWizard.instance().saved)
		{
			int choice = JOptionPane.showConfirmDialog(
				TopFrame.instance(),
				platwizLabels.getString("frame1.changesNotSaved"),
				platwizLabels.getString("frame1.quitWithoutSave"),
				JOptionPane.YES_NO_OPTION);
			if (choice == JOptionPane.NO_OPTION)
				return;
		}
		if (exitOnClose)
		{
			Database db = Database.getDb();
			db.getDbIo().close();
			System.exit(0);
		}
		else
			dispose();
	}

	/**
	  Populates my internal vector with all of the panels.
	  Hard-coded. This could be used as a template for future wizard-type
	  GUIs.
	*/
	private void initPanels()
	{
		panels = new Vector();
		panels.add(startPanel = new StartPanel());
		//panels.add(new SampleDataPanel());
		panels.add(new SitePanel());
		panels.add(new DefineSensorsPanel());
		panels.add(new SelectDevicePanel());

		ScriptEditPanel sep = new ScriptEditPanel();
		sep.setNameType("ST", 
				platwizLabels.getString("frame1.selfTimedMsgDesc"));
		panels.add(sep);

		sep = new ScriptEditPanel();
		sep.setNameType("RD", 
				platwizLabels.getString("frame1.randomTimedMsgDesc"));
		panels.add(sep);

		sep = new ScriptEditPanel();
		sep.setNameType("EDL", 
				platwizLabels.getString("frame1.USGSEDLFilesDesc"));
		panels.add(sep);

		panels.add(new PlatformPanel());
		panels.add(new SavePanel());

		curPanel = -1;
		switchPanel(0);
	}
}

class Frame1_nextButton_actionAdapter implements java.awt.event.ActionListener {
	Frame1 adaptee;

	Frame1_nextButton_actionAdapter(Frame1 adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.nextButton_actionPerformed(e);
	}
}

class Frame1_prevButton_actionAdapter implements java.awt.event.ActionListener {
	Frame1 adaptee;

	Frame1_prevButton_actionAdapter(Frame1 adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.prevButton_actionPerformed(e);
	}
}

class Frame1_quitButton_actionAdapter implements java.awt.event.ActionListener {
	Frame1 adaptee;

	Frame1_quitButton_actionAdapter(Frame1 adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.quitButton_actionPerformed(e);
	}
}
