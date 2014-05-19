/*
*  $Id$
*/
package decodes.rledit;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;

//MJM commented this out -- can't refer to sutron in open-source
//import com.sutron.sutronwin.ClientList;
//import com.sutron.sutronwin.SWCDialog;
//import com.sutron.sutronwin.SwinClient;
//import com.sutron.sutronwin.SwinClientTableModel;

import java.util.*;

import ilex.util.*;
import decodes.db.*;
import decodes.gui.SortingListTable;

/**
RefListFrame is the GUI application for Reference List Editor.
This program allows you to edit DECODES enumerations, engineering
units, EU conversions and data type equivalencies.
*/
public class RefListFrame extends JFrame
{
	private static ResourceBundle genericLabels = 
		RefListEditor.getGenericLabels();
	private static ResourceBundle labels = RefListEditor.getLabels();
	private JPanel contentPane;
	private JMenuBar jMenuBar1 = new JMenuBar();
	private JMenu jMenuFile = new JMenu();
	private JMenuItem jMenuFileExit = new JMenuItem();
	private JMenu jMenuHelp = new JMenu();
	private JMenuItem jMenuHelpAbout = new JMenuItem();
	private JLabel statusBar = new JLabel();
	private BorderLayout borderLayout1 = new BorderLayout();
	private JTabbedPane jTabbedPane1 = new JTabbedPane();
	private JPanel EnumTab = new JPanel();
	private JPanel EUTab = new JPanel();
	private JPanel EuCnvtTab = new JPanel();
	private JPanel DataTypeTab = new JPanel();
	
	private JPanel SwinClientTab = new JPanel();
	
	private JMenuItem mi_saveToDb = new JMenuItem();
	private JTextArea jTextArea1 = new JTextArea();
	private BorderLayout borderLayout2 = new BorderLayout();
	private Border border1;
	
	private JPanel jPanel1 = new JPanel();
	private JScrollPane jScrollPane1 = new JScrollPane();
	private EnumTableModel enumTableModel = new EnumTableModel();
	private JTable enumTable = new SortingListTable(enumTableModel,
		new int[] { 9, 20, 41, 30 });
	private JButton addEnumValButton = new JButton();
	private JButton editEnumValButton = new JButton();
	private JButton deleteEnumValButton = new JButton();
	private JButton selectEnumValDefaultButton = new JButton();
	private JButton upEnumValButton = new JButton();
	private JButton downEnumValButton = new JButton();
	private JPanel jPanel2 = new JPanel();
	private FlowLayout flowLayout1 = new FlowLayout();
	private JLabel jLabel1 = new JLabel();
	private JComboBox enumComboBox = new JComboBox();
	private BorderLayout borderLayout3 = new BorderLayout();
	private JTextArea jTextArea2 = new JTextArea();
	private Border border2;
	
	private JPanel jPanel3 = new JPanel();
	private JScrollPane jScrollPane2 = new JScrollPane();
	private EUTableModel euTableModel = new EUTableModel();
	private JTable euTable = new SortingListTable(euTableModel,
		new int[] { 20, 30, 25, 25 });
	private JButton addEUButton = new JButton();
	private JButton editEUButton = new JButton();
	private JButton deleteEUButton = new JButton();
	private JButton undoDeleteEnumValButton = new JButton();
	private GridBagLayout gridBagLayout1 = new GridBagLayout();
	private JButton undoDeleteEUButton = new JButton();
	private GridBagLayout gridBagLayout2 = new GridBagLayout();
	private BorderLayout borderLayout4 = new BorderLayout();
	private JTextArea jTextArea3 = new JTextArea();
	private Border border3;
	
	private JPanel jPanel4 = new JPanel();
	private JScrollPane jScrollPane3 = new JScrollPane();
	private EUCnvTableModel ucTableModel = new EUCnvTableModel();
	private JTable ucTable = new SortingListTable(ucTableModel,
		new int[] {17, 17, 18, 8, 8, 8, 8, 8, 8 });
	private JButton addEUCnvtButton = new JButton();
	private JButton editEUCnvtButton = new JButton();
	private JButton deleteEUCnvtButton = new JButton();
	private JButton undoDelEuCnvtButton = new JButton();
	private GridBagLayout gridBagLayout3 = new GridBagLayout();
	private BorderLayout borderLayout5 = new BorderLayout();
	private JTextArea jTextArea4 = new JTextArea();
	private Border border4;
	
	private JPanel dtePanelCenter = new JPanel();
	private JScrollPane jScrollPane4 = new JScrollPane();
	private DTEquivTableModel dteTableModel = new DTEquivTableModel();
	private JTable dteTable = new SortingListTable(dteTableModel,null);
	private JButton addDTEButton = new JButton();
	private JButton editDTEButton = new JButton();
	private JButton deleteDTEButton = new JButton();
	private JButton undoDeleteDTEButton = new JButton();
	private GridBagLayout gridBagLayout4 = new GridBagLayout();
	private Border border5;
	
	private JPanel jPanel6 = new JPanel();
	private JScrollPane jScrollPane5 = new JScrollPane();
	
//MJM commented this out -- can't refer to sutron in open-source
//	private SwinClientTableModel swClientTableModel = new SwinClientTableModel();
//	private JTable swcTable = new SortingListTable(swClientTableModel, new int[] {25, 20, 12, 8});
//	private JButton addSWCButton = new JButton();
//	private JButton editSWCButton = new JButton();
//	private JButton deleteSWCButton = new JButton();
//	private JButton undoDeleteSWCButton = new JButton();
	private GridBagLayout gridBagLayout5 = new GridBagLayout();
	private BorderLayout borderLayout6 = new BorderLayout();
	private Border border6;
	private Border border7;

	//================================================
	private boolean enumsChanged = false;
	private boolean unitsChanged = false;
	private boolean convertersChanged = false;
	private boolean dtsChanged = false;
	private boolean clientsChanged = false;
	private EnumValue deletedEnumValue = null;
	private EngineeringUnit deletedEU = null;
	private UnitConverterDb deletedConverter = null;
	private String []deletedDte = null;
	
//MJM commented this out -- can't refer to sutron in open-source
//	private SwinClient deletedSwc = null;

	/**
	 * No args constructor for JBuilder.
	 */
	public RefListFrame()
	{
//		System.out.println("RefListFrame() >>>>>>>>> ");
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		UIManager.put("TextArea.font", new Font("Serif", Font.ITALIC, 14));
		UIManager.put("TextArea.foreground", Color.green);
		try {
			jbInit();
			initControls();
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		// Default operation is to do nothing when user hits 'X' in upper
		// right to close the window. We will catch the closing event and
		// do the same thing as if user had hit File - Exit.
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(
			new WindowAdapter()
			{
				public void windowClosing(WindowEvent e)
				{
      				jMenuFileExit_actionPerformed(null);
				}
			});
	}

	/**
	 * Initializes the controls in the frames.
	 * Calls the init methods in other GUI objects, table models, etc.
	 */
	private void initControls()
	{
//		System.out.println("initControls >>>>>>>>>>>>>>>>>> ");
		Vector v = new Vector();
		for(Iterator enumIt = Database.getDb().enumList.iterator();
			enumIt.hasNext(); )
		{
			decodes.db.DbEnum en = (decodes.db.DbEnum)enumIt.next();
			if (en.enumName.equals("EquationScope")
			 || en.enumName.equals("DataOrder")
			 || en.enumName.equals("UnitFamily")
			 || en.enumName.equals("LookupAlgorithm")
			 || en.enumName.equals("RecordingMode")
			 || en.enumName.equals("EquipmentType"))
				continue;
			String s = TextUtil.capsExpand(en.enumName);
			v.add(s);
		}
		Collections.sort(v);
		for(int i=0; i<v.size(); i++)
			enumComboBox.addItem(v.elementAt(i));
		enumTable.setRowHeight(20);
		enumTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		euTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		euTable.setRowHeight(20);
		ucTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		ucTable.setRowHeight(20);
		dteTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		dteTable.setRowHeight(20);

//MJM commented this out -- can't refer to sutron in open-source
//		swcTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//		swcTable.setRowHeight(20);
	}

	/** Initializes GUI components. */
	private void jbInit() throws Exception	{
//		System.out.println("jbInit >>>>>>>>>>>>>>>>>>");
		contentPane = (JPanel) this.getContentPane();
		border1 = BorderFactory.createMatteBorder(6,6,6,6,Color.darkGray);
		border2 = BorderFactory.createMatteBorder(6,6,6,6,Color.darkGray);
		border3 = BorderFactory.createMatteBorder(6,6,6,6,Color.darkGray);
		border4 = BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED,Color.white,Color.white,new Color(124, 124, 124),new Color(178, 178, 178)),BorderFactory.createEmptyBorder(6,6,6,6));
		border5 = BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED,Color.white,Color.white,new Color(124, 124, 124),new Color(178, 178, 178)),BorderFactory.createEmptyBorder(6,6,6,6));
	    border6 = BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED,Color.white,Color.white,new Color(124, 124, 124),new Color(178, 178, 178)),BorderFactory.createEmptyBorder(6,6,6,6));
	    border7 = BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED,Color.white,Color.white,new Color(124, 124, 124),new Color(178, 178, 178)),BorderFactory.createEmptyBorder(6,6,6,6));
	    contentPane.setLayout(borderLayout1);
		this.setFont(new java.awt.Font("Serif", 0, 16));
    this.setSize(new Dimension(800, 536));
		this.setTitle(labels.getString("RefListFrame.frameTitle"));
		statusBar.setText(" ");
		jMenuFile.setText(genericLabels.getString("file"));
		jMenuFileExit.setText(genericLabels.getString("exit"));
		jMenuFileExit.addActionListener(new RefListFrame_jMenuFileExit_ActionAdapter(this));
		jMenuHelp.setText(genericLabels.getString("help"));
		jMenuHelpAbout.setText(genericLabels.getString("about"));
		jMenuHelpAbout.addActionListener(new RefListFrame_jMenuHelpAbout_ActionAdapter(this));
		jTabbedPane1.setTabPlacement(JTabbedPane.TOP);
		mi_saveToDb.setText(labels.getString("RefListFrame.saveToDB"));
		mi_saveToDb.addActionListener(new RefListFrame_mi_saveToDb_actionAdapter(this));
		
		jTextArea1.setBackground(Color.white);
		jTextArea1.setFont(new java.awt.Font("Serif", 0, 14));
		jTextArea1.setBorder(border5);
		jTextArea1.setEditable(false);
		jTextArea1.setText(labels.getString("RefListFrame.enumerationsTab"));
		jTextArea1.setLineWrap(true);
		jTextArea1.setRows(3);
		jTextArea1.setWrapStyleWord(true);
		EnumTab.setLayout(borderLayout2);
		jPanel1.setLayout(gridBagLayout1);
		addEnumValButton.setMaximumSize(new Dimension(122, 23));
		addEnumValButton.setMinimumSize(new Dimension(122, 23));
		addEnumValButton.setPreferredSize(new Dimension(122, 23));
		addEnumValButton.setText(genericLabels.getString("add"));
		addEnumValButton.addActionListener(new RefListFrame_addEnumValButton_actionAdapter(this));
		editEnumValButton.setMaximumSize(new Dimension(122, 23));
		editEnumValButton.setMinimumSize(new Dimension(122, 23));
		editEnumValButton.setPreferredSize(new Dimension(122, 23));
		editEnumValButton.setText(genericLabels.getString("edit"));
		editEnumValButton.addActionListener(new RefListFrame_editEnumValButton_actionAdapter(this));
		deleteEnumValButton.setMaximumSize(new Dimension(122, 23));
		deleteEnumValButton.setMinimumSize(new Dimension(122, 23));
		deleteEnumValButton.setPreferredSize(new Dimension(122, 23));
		deleteEnumValButton.setText(genericLabels.getString("delete"));
		deleteEnumValButton.addActionListener(new RefListFrame_deleteEnumValButton_actionAdapter(this));
//		selectEnumValDefaultButton.setMaximumSize(new Dimension(122, 23));
//		selectEnumValDefaultButton.setMinimumSize(new Dimension(122, 23));
//		selectEnumValDefaultButton.setPreferredSize(new Dimension(122, 23));
		selectEnumValDefaultButton.setText(labels.getString("RefListFrame.setDefault"));
		selectEnumValDefaultButton.addActionListener(new RefListFrame_selectEnumValDefaultButton_actionAdapter(this));
		upEnumValButton.setMaximumSize(new Dimension(122, 23));
		upEnumValButton.setMinimumSize(new Dimension(122, 23));
		upEnumValButton.setPreferredSize(new Dimension(122, 23));
		upEnumValButton.setText(labels.getString("RefListFrame.moveUp"));
		upEnumValButton.addActionListener(new RefListFrame_upEnumValButton_actionAdapter(this));
		downEnumValButton.setMaximumSize(new Dimension(122, 23));
		downEnumValButton.setMinimumSize(new Dimension(122, 23));
		downEnumValButton.setPreferredSize(new Dimension(122, 23));
		downEnumValButton.setText(labels.getString("RefListFrame.moveDown"));
		downEnumValButton.addActionListener(new RefListFrame_downEnumValButton_actionAdapter(this));
		jPanel2.setLayout(flowLayout1);
		jLabel1.setText(labels.getString("RefListFrame.enumeration"));
		enumComboBox.setMinimumSize(new Dimension(160, 19));
		enumComboBox.setPreferredSize(new Dimension(160, 19));
		enumComboBox.addActionListener(new RefListFrame_enumComboBox_actionAdapter(this));
		
		EUTab.setLayout(borderLayout3);
		jTextArea2.setFont(new java.awt.Font("Serif", 0, 14));
		jTextArea2.setBorder(border6);
		jTextArea2.setEditable(false);
		jTextArea2.setText(labels.getString("RefListFrame.engineeringTabDesc"));
		jTextArea2.setLineWrap(true);
		jTextArea2.setRows(3);
		jTextArea2.setWrapStyleWord(true);
		jPanel3.setLayout(gridBagLayout2);
		addEUButton.setMaximumSize(new Dimension(122, 23));
		addEUButton.setMinimumSize(new Dimension(122, 23));
		addEUButton.setPreferredSize(new Dimension(122, 23));
		addEUButton.setText(genericLabels.getString("add"));
		addEUButton.addActionListener(new RefListFrame_addEUButton_actionAdapter(this));
		editEUButton.setMaximumSize(new Dimension(122, 23));
		editEUButton.setMinimumSize(new Dimension(122, 23));
		editEUButton.setPreferredSize(new Dimension(122, 23));
		editEUButton.setText(genericLabels.getString("edit"));
		editEUButton.addActionListener(new RefListFrame_editEUButton_actionAdapter(this));
		deleteEUButton.setMaximumSize(new Dimension(122, 23));
		deleteEUButton.setMinimumSize(new Dimension(122, 23));
		deleteEUButton.setPreferredSize(new Dimension(122, 23));
		deleteEUButton.setText(genericLabels.getString("delete"));
		deleteEUButton.addActionListener(new RefListFrame_deleteEUButton_actionAdapter(this));
		undoDeleteEnumValButton.setEnabled(false);
//		undoDeleteEnumValButton.setMaximumSize(new Dimension(122, 23));
//		undoDeleteEnumValButton.setMinimumSize(new Dimension(122, 23));
//		undoDeleteEnumValButton.setPreferredSize(new Dimension(122, 23));
		undoDeleteEnumValButton.setText(labels.getString("RefListFrame.undoDelete"));
		undoDeleteEnumValButton.addActionListener(new RefListFrame_undoDeleteEnumValButton_actionAdapter(this));
		undoDeleteEUButton.setEnabled(false);
//		undoDeleteEUButton.setMaximumSize(new Dimension(122, 23));
//		undoDeleteEUButton.setMinimumSize(new Dimension(122, 23));
//		undoDeleteEUButton.setPreferredSize(new Dimension(122, 23));
		undoDeleteEUButton.setText(labels.getString("RefListFrame.undoDelete"));
		undoDeleteEUButton.addActionListener(new RefListFrame_undoDeleteEUButton_actionAdapter(this));
		
		EuCnvtTab.setLayout(borderLayout4);
		jTextArea3.setFont(new java.awt.Font("Serif", 0, 14));
		jTextArea3.setBorder(border7);
		jTextArea3.setEditable(false);
		jTextArea3.setText(labels.getString("RefListFrame.euconversionsDesc"));
		jTextArea3.setLineWrap(true);
		jTextArea3.setRows(3);
		jTextArea3.setWrapStyleWord(true);
		jPanel4.setLayout(gridBagLayout3);
		addEUCnvtButton.setMaximumSize(new Dimension(122, 23));
		addEUCnvtButton.setMinimumSize(new Dimension(122, 23));
		addEUCnvtButton.setPreferredSize(new Dimension(122, 23));
		addEUCnvtButton.setText(genericLabels.getString("add"));
		addEUCnvtButton.addActionListener(new RefListFrame_addEUCnvtButton_actionAdapter(this));
		editEUCnvtButton.setMaximumSize(new Dimension(122, 23));
		editEUCnvtButton.setMinimumSize(new Dimension(122, 23));
		editEUCnvtButton.setPreferredSize(new Dimension(122, 23));
		editEUCnvtButton.setRequestFocusEnabled(true);
		editEUCnvtButton.setText(genericLabels.getString("edit"));
		editEUCnvtButton.addActionListener(new RefListFrame_editEUCnvtButton_actionAdapter(this));
		deleteEUCnvtButton.setMaximumSize(new Dimension(122, 23));
		deleteEUCnvtButton.setMinimumSize(new Dimension(122, 23));
		deleteEUCnvtButton.setPreferredSize(new Dimension(122, 23));
		deleteEUCnvtButton.setText(genericLabels.getString("delete"));
		deleteEUCnvtButton.addActionListener(new RefListFrame_deleteEUCnvtButton_actionAdapter(this));
//		undoDelEuCnvtButton.setMaximumSize(new Dimension(122, 23));
//		undoDelEuCnvtButton.setMinimumSize(new Dimension(122, 23));
//		undoDelEuCnvtButton.setPreferredSize(new Dimension(122, 23));
		undoDelEuCnvtButton.setText(labels.getString("RefListFrame.undoDelete"));
		undoDelEuCnvtButton.addActionListener(new RefListFrame_undoDelEuCnvtButton_actionAdapter(this));
		
		DataTypeTab.setLayout(borderLayout5);
		jTextArea4.setFont(new java.awt.Font("Serif", 0, 14));
    jTextArea4.setBorder(border4);
		jTextArea4.setEditable(false);
		jTextArea4.setText(labels.getString("RefListFrame.dataTypeEquDesc"));
		jTextArea4.setLineWrap(true);
		jTextArea4.setRows(3);
		jTextArea4.setWrapStyleWord(true);
		DataTypeTab.setDebugGraphicsOptions(0);
		
		dtePanelCenter.setLayout(gridBagLayout4);
		addDTEButton.setMaximumSize(new Dimension(122, 23));
		addDTEButton.setMinimumSize(new Dimension(122, 23));
		addDTEButton.setPreferredSize(new Dimension(122, 23));
		addDTEButton.setText(labels.getString("RefListFrame.addEquiv"));
		addDTEButton.addActionListener(new RefListFrame_addDTEButton_actionAdapter(this));
		editDTEButton.setMaximumSize(new Dimension(122, 23));
		editDTEButton.setMinimumSize(new Dimension(122, 23));
		editDTEButton.setPreferredSize(new Dimension(122, 23));
		editDTEButton.setMargin(new Insets(2, 14, 2, 14));
		editDTEButton.setText(genericLabels.getString("edit"));
		editDTEButton.addActionListener(new RefListFrame_editDTEButton_actionAdapter(this));
		deleteDTEButton.setMaximumSize(new Dimension(122, 23));
		deleteDTEButton.setMinimumSize(new Dimension(122, 23));
		deleteDTEButton.setPreferredSize(new Dimension(122, 23));
		deleteDTEButton.setText(labels.getString("RefListFrame.deleteEquiv"));
		deleteDTEButton.addActionListener(new RefListFrame_deleteDTEButton_actionAdapter(this));
		undoDeleteDTEButton.setEnabled(false);
//		undoDeleteDTEButton.setMaximumSize(new Dimension(122, 23));
//		undoDeleteDTEButton.setMinimumSize(new Dimension(122, 23));
//		undoDeleteDTEButton.setPreferredSize(new Dimension(122, 23));
		undoDeleteDTEButton.setText(labels.getString("RefListFrame.undoDelete"));
		undoDeleteDTEButton.addActionListener(new RefListFrame_undoDeleteDTEButton_actionAdapter(this));

//MJM commented this out -- can't refer to sutron in open-source
//		swcTable.setLayout(borderLayout6);
//		swcTable.setDebugGraphicsOptions(0);
//		jPanel6.setLayout(gridBagLayout5);
//		addSWCButton.setMaximumSize(new Dimension(122, 23));
//		addSWCButton.setMinimumSize(new Dimension(122, 23));
//		addSWCButton.setPreferredSize(new Dimension(122, 23));
//		addSWCButton.setText("Add");
//		addSWCButton.addActionListener(new RefListFrame_addSWCButton_actionAdapter(this));
//		editSWCButton.setMaximumSize(new Dimension(122, 23));
//		editSWCButton.setMinimumSize(new Dimension(122, 23));
//		editSWCButton.setPreferredSize(new Dimension(122, 23));
////		editSWCButton.setMargin(new Insets(2, 14, 2, 14));
//		editSWCButton.setText("Edit");
//		editSWCButton.addActionListener(new RefListFrame_editSWCButton_actionAdapter(this));
//		deleteSWCButton.setMaximumSize(new Dimension(122, 23));
//		deleteSWCButton.setMinimumSize(new Dimension(122, 23));
//		deleteSWCButton.setPreferredSize(new Dimension(122, 23));
//		deleteSWCButton.setText("Delete");
//		deleteSWCButton.addActionListener(new RefListFrame_deleteSWCButton_actionAdapter(this));
//		undoDeleteSWCButton.setEnabled(false);
////		undoDeleteSWCButton.setMaximumSize(new Dimension(122, 23));
////		undoDeleteSWCButton.setMinimumSize(new Dimension(122, 23));
////		undoDeleteSWCButton.setPreferredSize(new Dimension(122, 23));
//		undoDeleteSWCButton.setText("Undo Delete");
//		undoDeleteSWCButton.addActionListener(new RefListFrame_undoDeleteSWCButton_actionAdapter(this));
		
		contentPane.setFont(new java.awt.Font("Dialog", 0, 14));
	EuCnvtTab.setBorder(BorderFactory.createEmptyBorder());
	jMenuFile.add(mi_saveToDb);
		jMenuFile.add(jMenuFileExit);
		jMenuHelp.add(jMenuHelpAbout);
		jMenuBar1.add(jMenuFile);
		jMenuBar1.add(jMenuHelp);
		this.setJMenuBar(jMenuBar1);
		contentPane.add(statusBar, BorderLayout.SOUTH);
		contentPane.add(jTabbedPane1, BorderLayout.CENTER);
		
		jTabbedPane1.add(EnumTab,	 labels.getString("RefListFrame.enumTab"));
		EnumTab.add(jTextArea1, BorderLayout.NORTH);
		EnumTab.add(jPanel1, BorderLayout.CENTER);
		jPanel1.add(jScrollPane1,	 new GridBagConstraints(0, 1, 1, 7, 1.0, 1.0
						,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 8, 12, 0), 10, -98));
		jScrollPane1.getViewport().add(enumTable, null);
		
		jTabbedPane1.add(EUTab,	labels.getString("RefListFrame.EngUnitsTab"));
		EUTab.add(jTextArea2, BorderLayout.NORTH);
		EUTab.add(jPanel3, BorderLayout.CENTER);
		jPanel3.add(jScrollPane2,	new GridBagConstraints(0, 0, 1, 4, 1.0, 1.0
						,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(18, 10, 13, 0), 18, -56));
		jScrollPane2.getViewport().add(euTable, null);
		
		jTabbedPane1.add(EuCnvtTab,	labels.getString("RefListFrame.euConvTab"));
		EuCnvtTab.add(jTextArea3, BorderLayout.NORTH);
		EuCnvtTab.add(jPanel4, BorderLayout.CENTER);
		jPanel4.add(jScrollPane3,	new GridBagConstraints(0, 0, 1, 4, 1.0, 1.0
						,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(21, 11, 10, 0), 25, -90));
		jScrollPane3.getViewport().add(ucTable, null);
		
		jTabbedPane1.add(DataTypeTab,	labels.getString("RefListFrame.dataTypeEquivTab"));
		DataTypeTab.add(jTextArea4, BorderLayout.NORTH);
		DataTypeTab.add(dtePanelCenter, BorderLayout.CENTER);
		dtePanelCenter.add(jScrollPane4,	 new GridBagConstraints(0, 0, 1, 4, 1.0, 1.0,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH, 
			new Insets(17, 15, 10, 0), 27, -77));
		jScrollPane4.getViewport().add(dteTable, null);

//MJM commented this out -- can't refer to sutron in open-source
//		jTabbedPane1.add(SwinClientTab, "SutronWIN Client");
//		SwinClientTab.add(jPanel6, BorderLayout.CENTER);
//		jPanel6.add(jScrollPane5, new GridBagConstraints(0, 0, 1, 4, 1.0, 1.0
//						,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(20, 10, 10, 10), 20, -69));
//		jScrollPane5.getViewport().add(swcTable, null);
		
		
		jPanel1.add(editEnumValButton,		new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 20, 5, 20), 0, 0));
		jPanel1.add(deleteEnumValButton,	 new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 20, 5, 20), 0, 0));
		jPanel1.add(jPanel2,	 new GridBagConstraints(0, 0, 2, 1, 1.0, 0.1
						,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(14, 18, 14, 12), 0, 7));
		
		jPanel2.add(jLabel1, null);
		jPanel2.add(enumComboBox, null);
		
		jPanel1.add(addEnumValButton,		new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 20, 5, 20), 0, 0));
		jPanel1.add(downEnumValButton,	 new GridBagConstraints(1, 7, 1, 1, 0.0, 0.0
						,GridBagConstraints.NORTH, GridBagConstraints.NONE, new Insets(5, 20, 5, 20), 0, 0));
		jPanel1.add(selectEnumValDefaultButton,	 new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 20, 5, 20), 0, 0));
		jPanel1.add(upEnumValButton,	 new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 20, 5, 20), 0, 0));
		jPanel1.add(undoDeleteEnumValButton,	 new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 20, 5, 20), 0, 0));
		
		jPanel3.add(addEUButton,		new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(15, 20, 5, 20), 0, 0));
		jPanel3.add(undoDeleteEUButton,	 new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
						,GridBagConstraints.NORTH, GridBagConstraints.NONE, new Insets(5, 20, 5, 20), 0, 0));
		jPanel3.add(deleteEUButton,	 new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 20, 5, 20), 0, 0));
		jPanel3.add(editEUButton,	 new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 20, 5, 20), 0, 0));
		
		jPanel4.add(addEUCnvtButton,		new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(15, 16, 5, 12), 0, 0));
		jPanel4.add(editEUCnvtButton,	 new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 16, 5, 12), 0, 0));
		jPanel4.add(deleteEUCnvtButton,	 new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 16, 5, 12), 0, 0));
		jPanel4.add(undoDelEuCnvtButton,	 new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
						,GridBagConstraints.NORTH, GridBagConstraints.NONE, new Insets(5, 16, 5, 12), 0, 0));
		
		dtePanelCenter.add(addDTEButton, 
			new GridBagConstraints(1, 0, 1, 1, 0.0, 0.5,
				GridBagConstraints.SOUTH, GridBagConstraints.NONE, 
				new Insets(5, 20, 5, 20), 0, 0));
		dtePanelCenter.add(editDTEButton,
			new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE, 
				new Insets(5, 20, 5, 20), 0, 0));
		dtePanelCenter.add(deleteDTEButton,
			new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(5, 20, 5, 20), 0, 0));
		dtePanelCenter.add(undoDeleteDTEButton,
			new GridBagConstraints(1, 3, 1, 1, 0.0, .5,
				GridBagConstraints.NORTH, GridBagConstraints.NONE,
				new Insets(5, 20, 5, 20), 0, 0));
		
//MJM commented this out -- can't refer to sutron in open-source
//		jPanel6.add(addSWCButton,			new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
//				,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(20, 20, 5, 20), 0, 0));
//		jPanel6.add(editSWCButton,	 new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
//				,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 20, 5, 20), 0, 0));
//		jPanel6.add(deleteSWCButton,		new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
//				,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 20, 5, 20), 0, 0));
//		jPanel6.add(undoDeleteSWCButton,		new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
//				,GridBagConstraints.NORTH, GridBagConstraints.NONE, new Insets(5, 20, 5, 20), 0, 0));
	}

	/** 
	 * File | Exit action performed.
	 * @param e ignored
	 */
	public void jMenuFileExit_actionPerformed(ActionEvent e) 
	{
		if (enumsChanged || unitsChanged || convertersChanged || dtsChanged)
		{
			int r = JOptionPane.showConfirmDialog(this,
				labels.getString("RefListFrame.unsavedChangesQues"), 
				labels.getString("RefListFrame.confirmExit"),
				JOptionPane.YES_NO_OPTION);
			if (r != JOptionPane.YES_OPTION)
				return;
		}
		System.exit(0);
	}

	/**
	 * Help | About action performed.
	 * @param e ignored
	 */
	public void jMenuHelpAbout_actionPerformed(ActionEvent e) {
		RefListFrame_AboutBox dlg = new RefListFrame_AboutBox(this);
		Dimension dlgSize = dlg.getPreferredSize();
		Dimension frmSize = getSize();
		java.awt.Point loc = getLocation();
		dlg.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x, (frmSize.height - dlgSize.height) / 2 + loc.y);
		dlg.setModal(true);
		dlg.pack();
		dlg.show();
	}

	/**
	 * Selects an Enumeration to be displayed in the table.
	 * The table is repopulated and the 'Undo' button is disabled.
	 * @param e ActionEvent
	 */
	void enumComboBox_actionPerformed(ActionEvent e)
	{
		// todo: Populate table from selected enum.
		String s = (String)enumComboBox.getSelectedItem();
		s = TextUtil.removeAllSpace(s);
		enumTableModel.setEnum(s);
		deletedEnumValue = null;
		deletedEU = null;
		deletedConverter = null;
		undoDeleteEnumValButton.setEnabled(false);
	}


	/**
	 * Displays modal enum value dialog. If OK pressed, results are added
	 * to table.
	 * Sets modified flag.
	 * @param e ActionEvent
	 */
	void addEnumValButton_actionPerformed(ActionEvent e)
	{
		String s = TextUtil.removeAllSpace(
			(String)enumComboBox.getSelectedItem());
		decodes.db.DbEnum en = Database.getDb().getDbEnum(s);

		EnumValueDialog evd = new EnumValueDialog();
		EnumValue ev = new EnumValue(en, "", "", "", "");
		evd.fillValues(ev);
		launchDialog(evd);
		if (evd.wasChanged())
		{
			en.replaceValue(ev.value, ev.description, ev.execClassName, "");
			enumTableModel.fireTableDataChanged();
			enumsChanged = true;
		}
	}


	/**
	* Displays modal enum-value dialog with selected EV. If OK pressed
	* the dialog contents are added to the table.
	* The 'modified' flag is set.
	* @param e ActionEvent
	*/
	void editEnumValButton_actionPerformed(ActionEvent e)
	{
		int row = enumTable.getSelectedRow();
		if (row == -1)
		{
			showError(labels.getString("RefListFrame.enumSelectInfo"));
			return;
		}

		String s = TextUtil.removeAllSpace(
			(String)enumComboBox.getSelectedItem());
		decodes.db.DbEnum en = Database.getDb().getDbEnum(s);
		EnumValue ev = enumTableModel.getEnumValueAt(row);
		EnumValueDialog evd = new EnumValueDialog();
		evd.fillValues(ev);
		launchDialog(evd);
		if (evd.wasChanged())
		{
			en.replaceValue(ev.value, ev.description, ev.execClassName, ev.editClassName);
			enumTableModel.fireTableDataChanged();
			enumsChanged = true;
		}
	}

	/**
	 * Deletes the selected enum-value from the table and places it in the
	 * undo buffer. Enables the Undo Delete button.
	 * @param e ActionEvent
	 */
	void deleteEnumValButton_actionPerformed(ActionEvent e)
	{
		int row = enumTable.getSelectedRow();
		if (row == -1)
		{
			showError(labels.getString("RefListFrame.enumDeleteInfo"));
			return;
		}

		String s = TextUtil.removeAllSpace(
			(String)enumComboBox.getSelectedItem());
		decodes.db.DbEnum en = Database.getDb().getDbEnum(s);
		deletedEnumValue = enumTableModel.getEnumValueAt(row);
		en.removeValue(deletedEnumValue.value);
		enumTableModel.fireTableDataChanged();
		undoDeleteEnumValButton.setEnabled(true);
		enumsChanged = true;
	}

	/**
	 * Adds the deleted enum value back into the table.
	 * Disables the undo button.
	 * @param e ActionEvent
	 */
	void undoDeleteEnumValButton_actionPerformed(ActionEvent e)
	{
		if (deletedEnumValue != null)
		{
			String s = TextUtil.removeAllSpace(
				(String)enumComboBox.getSelectedItem());
			decodes.db.DbEnum en = Database.getDb().getDbEnum(s);
			en.replaceValue(deletedEnumValue.value,
				deletedEnumValue.description,
				deletedEnumValue.execClassName, "");
			deletedEnumValue = null;
			enumTableModel.fireTableDataChanged();
		}
		undoDeleteEnumValButton.setEnabled(false);
		enumsChanged = true;
	}

	/**
	 * Marks the currently selected enumeration value as the 'default' value.
	 * @param e ActionEvent
	 */
	void selectEnumValDefaultButton_actionPerformed(ActionEvent e)
	{
		int row = enumTable.getSelectedRow();
		if (row == -1)
		{
			showError(labels.getString("RefListFrame.enumDefaultInfo"));
			return;
		}

		String s = TextUtil.removeAllSpace(
			(String)enumComboBox.getSelectedItem());
		decodes.db.DbEnum en = Database.getDb().getDbEnum(s);
		EnumValue ev = enumTableModel.getEnumValueAt(row);
		enumTableModel.fireTableDataChanged();
		en.setDefault(ev.value);
		enumsChanged = true;
	}

	/**
	 * Moves the currently selected enumeration value up in the table.
	 * When written to the database, the sort order will be set to the
	 * currently displayed order. This and the down button allow you to set
	 * the desired order.
	 * @param e ActionEvent
	 */
	void upEnumValButton_actionPerformed(ActionEvent e)
	{
		int row = enumTable.getSelectedRow();
		if (row == -1)
		{
			showError(labels.getString("RefListFrame.enumMovUpInfo"));
			return;
		}
		if (enumTableModel.moveUp(row))
			enumTable.setRowSelectionInterval(row-1, row-1);
		enumsChanged = true;
	}

	/**
	 * Moves the currently selected enumeration value down in the table.
	 * When written to the database, the sort order will be set to the
	 * currently displayed order. This and the up button allow you to set
	 * the desired order.
	 * @param e ActionEvent
	 */
	void downEnumValButton_actionPerformed(ActionEvent e)
	{
		int row = enumTable.getSelectedRow();
		if (row == -1)
		{
			showError(labels.getString("RefListFrame.enumMovDnInfo"));
			return;
		}
		if (enumTableModel.moveDown(row))
			enumTable.setRowSelectionInterval(row+1, row+1);
		enumsChanged = true;
	}

	/**
	 * Displays modal EU dialog. If OK pressed, results are added to the
	 * table. Sets modified flag.
	 * @param e ActionEvent
	 */
	void addEUButton_actionPerformed(ActionEvent e)
	{
		EUDialog dlg = new EUDialog();
		launchDialog(dlg);
		if (dlg.wasChanged())
		{
			String abbr = dlg.getAbbr();
			if (abbr == null || abbr.length() == 0)
			{
				showError(labels.getString("RefListFrame.euAbbrErr"));
				return;
			}

			EngineeringUnit eu = 
				Database.getDb().engineeringUnitList.getByAbbr(abbr);
			if (eu != null)
			{
				showError(LoadResourceBundle.sprintf(
						labels.getString("RefListFrame.euAlreadyExistErr"),
						abbr));
				return;
			}

			eu = EngineeringUnit.getEngineeringUnit(abbr);
			eu.setName(dlg.getName());
			eu.family = dlg.getFamily();
			eu.measures = dlg.getMeasures();
			unitsChanged = true;
			euTableModel.rebuild();
			euTableModel.fireTableDataChanged();
		}
	}

	/**
	* Displays model EU dialog with the currently selected EU. If OK pressed,
	* any modifications are copied back into the object in the table. Sets
	* modified flag.
	* @param e ActionEvent
	*/
	void editEUButton_actionPerformed(ActionEvent e)
	{
		int row = euTable.getSelectedRow();
		if (row == -1)
		{
			showError(labels.getString("RefListFrame.engEditInfo"));
			return;
		}
		EngineeringUnit eu = (EngineeringUnit)euTableModel.getRowObject(row);
		String oldAbbr = eu.abbr;
		EUDialog dlg = new EUDialog();
		dlg.fillValues(eu);
		launchDialog(dlg);
		if (dlg.wasChanged())
		{
			EngineeringUnitList eul = Database.getDb().engineeringUnitList;
			String abbr = dlg.getAbbr();
			if (abbr == null || abbr.length() == 0)
			{
				showError(labels.getString("RefListFrame.euAbbrErr"));
				return;
			}
			if (!oldAbbr.equalsIgnoreCase(abbr))
			{
				// Abbr was changed, make sure it doesn't clash with another EU.
				EngineeringUnit otherEU = eul.getByAbbr(abbr);
				if (otherEU != null)
				{
					showError(LoadResourceBundle.sprintf(
							labels.getString("RefListFrame.cantChangeAbbr"),
							abbr));
					return;
				}
			}

			eul.remove(eu);  // Remove hash entry for old abbr & name.
			eu.abbr = abbr;
			eu.setName(dlg.getName());
			eu.family = dlg.getFamily();
			eu.measures = dlg.getMeasures();
			eul.add(eu);     // Re-add with correct hash entries.

			unitsChanged = true;
			euTableModel.rebuild();
			euTableModel.fireTableDataChanged();
		}
	}

	/**
	 * Deletes the currently selected EU and adds it to the undo buffer.
	 * Enables the undo-delete button.
	 * @param e ActionEvent
	 */
	void deleteEUButton_actionPerformed(ActionEvent e)
	{
		int row = euTable.getSelectedRow();
		if (row == -1)
		{
			showError(labels.getString("RefListFrame.engDeleteInfo"));
			return;
		}
		deletedEU = (EngineeringUnit)euTableModel.getRowObject(row);
		Database.getDb().engineeringUnitList.remove(deletedEU);
		unitsChanged = true;
		euTableModel.rebuild();
		euTableModel.fireTableDataChanged();
		undoDeleteEUButton.setEnabled(true);
	}

	/**
	 * Re-adds the currently selected EU back into the table.
	 * Disables the undo-delete button.
	 * @param e ActionEvent
	 */
	void undoDeleteEUButton_actionPerformed(ActionEvent e)
	{
		undoDeleteEUButton.setEnabled(false);
		if (deletedEU == null)
			return;
		Database.getDb().engineeringUnitList.add(deletedEU);
		deletedEU = null;
		unitsChanged = true;
		euTableModel.rebuild();
		euTableModel.fireTableDataChanged();
	}

	/**
	 * Displays the modal EU Conversion dialog. If OK pressed, the results
	 * are added to the table as a new conversion.
	 * @param e ActionEvent
	 */
	void addEUCnvtButton_actionPerformed(ActionEvent e)
	{
		UnitConverterDb uc = new UnitConverterDb("", "");
		uc.algorithm = "";
		EUCnvEditDialog dlg = new EUCnvEditDialog();
		dlg.fillValues(this, uc);
		launchDialog(dlg);
		if (dlg.wasChanged())
		{
			UnitConverterSet ucs = Database.getDb().unitConverterSet;
			ucs.addDbConverter(uc);
			convertersChanged = true;
			ucTableModel.rebuild();
			ucTableModel.fireTableDataChanged();
		}
	}

	/**
	 * Displays the model EU Conversion dialog with the selected conversion.
	 * If OK pressed, the results are added to the table.
	 * @param e ActionEvent
	 */
	void editEUCnvtButton_actionPerformed(ActionEvent e)
	{
		int row = ucTable.getSelectedRow();
		if (row == -1)
		{
			showError(labels.getString("RefListFrame.unitConvEditInfo"));
			return;
		}

		UnitConverterDb uc = (UnitConverterDb)ucTableModel.getRowObject(row);
		String oldFrom = uc.fromAbbr;
		String oldTo = uc.toAbbr;
		EUCnvEditDialog dlg = new EUCnvEditDialog();
		dlg.fillValues(this, uc);

		launchDialog(dlg);
		if (dlg.wasChanged())
		{
			UnitConverterSet ucs = Database.getDb().unitConverterSet;

			/*
			  Converters are hashed based on from/to abbreviations. So
			  if either abbr is changed, delete from set & re-add.
			  Note: Dialog makes sure there is no clash if abbrs are changed.
			*/
			if (!oldFrom.equals(uc.fromAbbr) || !oldTo.equals(uc.toAbbr))
			{
				ucs.removeDbConverter(oldFrom, oldTo);
				ucs.addDbConverter(uc);
			}

			convertersChanged = true;
			ucTableModel.rebuild();
			ucTableModel.fireTableDataChanged();
		}
	}

	/**
	 * Deletes the selected EU conversion & adds it to the undo buffer.
	 * Enables the undo button.
	 * @param e ActionEvent
	 */
	void deleteEUCnvtButton_actionPerformed(ActionEvent e)
	{
		int row = ucTable.getSelectedRow();
		if (row == -1)
		{
			showError(labels.getString("RefListFrame.unitConvDeleteInfo"));
			return;
		}
		deletedConverter = (UnitConverterDb)ucTableModel.getRowObject(row);
		Database.getDb().unitConverterSet.removeDbConverter(
			deletedConverter.fromAbbr, deletedConverter.toAbbr);
		convertersChanged = true;
		ucTableModel.rebuild();
		ucTableModel.fireTableDataChanged();
		undoDelEuCnvtButton.setEnabled(true);
	}

	/**
	 * Re-adds the deleted EU Conversion back into the table.
	 * Disables the undo button.
	 * @param e ActionEvent
	 */
	void undoDelEuCnvtButton_actionPerformed(ActionEvent e)
	{
		if (deletedConverter == null)
			return;
		Database.getDb().unitConverterSet.addDbConverter(deletedConverter);
		deletedConverter = null;
		convertersChanged = true;
		ucTableModel.rebuild();
		ucTableModel.fireTableDataChanged();
		undoDelEuCnvtButton.setEnabled(false);
	}

	/**
	 * Displays the DataType Equivalence dialog. If OK pressed, adds a
	 * new DTE to the table.
	 * @param e ActionEvent
	 */
	void addDTEButton_actionPerformed(ActionEvent e)
	{
		undoDeleteDTEButton.setEnabled(false);
		deletedDte = null;

		DTEDialog dlg = new DTEDialog(this);
		dlg.fillValues(dteTableModel, -1);
		launchDialog(dlg);
		if (dlg.wasChanged())
		{
			dtsChanged = true;
			dteTableModel.rebuild();
			dteTableModel.fireTableDataChanged();
		}
	}

	/**
	 * Displays the modal DataType Equivalence dialog with the selected
	 * element in the table.
	 * @param e ActionEvent
	 */
	void editDTEButton_actionPerformed(ActionEvent e)
	{
		undoDeleteDTEButton.setEnabled(false);
		deletedDte = null;

		int row = dteTable.getSelectedRow();
		if (row == -1)
		{
			showError(labels.getString("RefListFrame.selectRowEditInfo"));
			return;
		}
		DTEDialog dlg = new DTEDialog(this);
		dlg.fillValues(dteTableModel, row);
		launchDialog(dlg);
		if (dlg.wasChanged())
		{
			dtsChanged = true;
			dteTableModel.rebuild();
			dteTableModel.fireTableDataChanged();
		}
	}

	/**
	 * Restores the deleted data type equivalence to the table.
	 * Disables the undo button.
	 * @param e ActionEvent
	 */
	void undoDeleteDTEButton_actionPerformed(ActionEvent e)
	{
		if (deletedDte != null)
		{
			DataType lastDT = null;
			for(int i=0; i<deletedDte.length; i++)
			{
				String v = deletedDte[i];
				if (v != null && v.length() > 0)
				{
					String std = dteTableModel.getColumnName(i);
					DataType ndt = DataType.getDataType(std, v);
					if (lastDT != null)
						lastDT.assertEquivalence(ndt);
					lastDT = ndt;
				}
			}
			dteTableModel.rebuild();
			dteTableModel.fireTableDataChanged();
		}
		dtsChanged = true;
		deletedDte = null;
		undoDeleteDTEButton.setEnabled(false);
	}

	/**
	 * Deletes the selected data type equivalence and adds it to the undo buffer.
	 * Enables the undo button.
	 * @param e ActionEvent
	 */
	void deleteDTEButton_actionPerformed(ActionEvent e)
	{
		int row = dteTable.getSelectedRow();
		if (row == -1)
		{
			showError(labels.getString("RefListFrame.selectRowDeleteInfo"));
			return;
		}
		deletedDte = (String[])dteTableModel.getRowObject(row);
		DataTypeSet dts = Database.getDb().dataTypeSet;
		for(int i=0; i<dteTableModel.getColumnCount(); i++)
		{
			String v = deletedDte[i];
			if (v != null && v.length() > 0)
			{
				String std = dteTableModel.getColumnName(i);
				DataType dt = dts.get(std, v);
				if (dt != null)
					dt.deAssertEquivalence();
			}
		}
		undoDeleteDTEButton.setEnabled(true);
		dteTableModel.rebuild();
		dteTableModel.fireTableDataChanged();
		dtsChanged = true;
	}

	/**
	 * Called when save to DB menu item selected.
	 * @param e ActionEvent
	 */
	void mi_saveToDb_actionPerformed(ActionEvent e)
	{
		Database db = Database.getDb();

		String what = "";
		try
		{
			if (enumsChanged)
			{
				what = "Enumerations";
				db.enumList.write();
				enumsChanged = false;
			}
			if (unitsChanged || convertersChanged)
			{
				what = "Engineering Units";
				db.engineeringUnitList.write();
				unitsChanged = convertersChanged = false;
			}
			if (dtsChanged)
			{
				what = "Data Types";
				db.dataTypeSet.write();
				dtsChanged = false;
			}
			
//MJM commented this out -- can't refer to sutron in open-source
//			if(clientsChanged)
//			{
//				what = "Swin Clients";
//				db.clientList.write();
//				clientsChanged = false;
//			}
			JOptionPane.showConfirmDialog(this, labels.getString("RefListFrame.changesWritten"),
				"Info", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
		}
		catch(DatabaseException ex)
		{
			showError(LoadResourceBundle.sprintf(labels.getString(
					"RefListFrame.writingErr"),what) + ex);
			ex.printStackTrace();
		}
	}

	/**
	  Launches the passed modal dialog at a reasonable position on the screen.
	  @param dlg the dialog.
	*/
	protected void launchDialog(JDialog dlg)
	{
		dlg.setModal(true);
		java.awt.Point loc = this.getLocation();
		Dimension frmSize = this.getSize();
		Dimension dlgSize = dlg.getPreferredSize();
		int x = (frmSize.width - dlgSize.width) / 2 + loc.x;
		int y = (frmSize.height - dlgSize.height) / 2 + loc.y;
		dlg.setLocation(x, y);
		//dlg.setVisible(true);
		dlg.show();
	}

	/**
	 * Shows an error message in a JOptionPane and prints it to stderr.
	 * @param msg the error message.
	 */
	public void showError(String msg)
	{
		System.err.println(msg);
		JOptionPane.showMessageDialog(this,
			AsciiUtil.wrapString(msg, 60), "Error!", JOptionPane.ERROR_MESSAGE);
	}

	public void deleteSWCButton_actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void undoDeleteSWCButton_actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}

//MJM commented this out -- can't refer to sutron in open-source
//	public void editSWCButton_actionPerformed(ActionEvent e) 
//	{
//		int row = swcTable.getSelectedRow();
//		if (row == -1)
//		{
//			showError(labels.getString("RefListFrame.engEditInfo"));
//			return;
//		}
//		SwinClient swc = (SwinClient)swClientTableModel.getRowObject(row);
//		String oldName = swc.getOrgName();
//		SWCDialog dlg = new SWCDialog(swc);
//		dlg.fillValues(swc);
//		launchDialog(dlg);
//		if (dlg.wasChanged() || dlg.userDlgChanged)
//		{
//			ClientList cl = Database.getDb().clientList;
//			String newName = dlg.getName();
//			
//			if (newName == null || newName.length() == 0)
//			{
//				showError(labels.getString("RefListFrame.euAbbrErr"));
//				return;
//			}
//			
//			if (!oldName.equalsIgnoreCase(newName))
//			{
//				// Name was changed, make sure it doesn't clash with another EU.
//				SwinClient otherSC = cl.getByName(newName);
//				if (otherSC != null)
//				{
//					showError(LoadResourceBundle.sprintf(
//							labels.getString("RefListFrame.cantChangeAbbr"),
//							newName));
//					return;
//				}
//			}
//
//			cl.remove(swc);  // Remove hash entry for old name.
////			swClientTableModel.rebuild();
//			swc.setOrgName(newName);
//			swc.setOrgPhone(dlg.getPhone());
//			swc.setAddress(dlg.getAddr());
//			swc.setTsGroup(dlg.getTSGroup());
//			swc.setLat(dlg.getLat());
//			swc.setLon(dlg.getLon());
//			swc.setZoom(dlg.getZoom());
//			swc.setPresentationGroupName(dlg.getPresentation());
//			swc.setTimeZone(dlg.getTimeZone());
////			cl.add(swc);     // Re-add with correct hash entries.
//
//			clientsChanged = true;
//			swClientTableModel.rebuild();
//			swClientTableModel.fireTableDataChanged();
//		}
//		
//	}
//
//	public void addSWCButton_actionPerformed(ActionEvent e) 
//	{
//		SWCDialog dlg = new SWCDialog();
//		launchDialog(dlg);
//		if (dlg.wasChanged())
//		{
//			String name = dlg.getName();
//			if (name == null || name.length() == 0)
//			{
//				showError(labels.getString("RefListFrame.euAbbrErr"));
//				return;
//			}
//
//			SwinClient sc = 
//				Database.getDb().clientList.getByName(name);
//			
//			if (sc != null)
//			{
//				showError(LoadResourceBundle.sprintf(
//						labels.getString("RefListFrame.euAlreadyExistErr"),
//						name));
//				return;
//			}
//
//			sc = SwinClient.getSwinClient(name);
//			sc.setClientId(swClientTableModel.getCnv().size()+1);
//			sc.setOrgName(name);
//			sc.setOrgPhone(dlg.getPhone());
//			sc.setAddress(dlg.getAddr());
//			sc.setTsGroup(dlg.getTSGroup());
//			sc.setLat(dlg.getLat());
//			sc.setLon(dlg.getLon());
//			sc.setPresentationGroupName(dlg.getPresentation());
//			sc.setTimeZone(dlg.getTimeZone());
//			clientsChanged = true;
//			swClientTableModel.rebuild();
//			swClientTableModel.fireTableDataChanged();
//		}
//		
//	}

	//============== Stub Methods - Override in Sub-Class ===========

}
class RefListFrame_jMenuFileExit_ActionAdapter implements ActionListener {
	RefListFrame adaptee;

	RefListFrame_jMenuFileExit_ActionAdapter(RefListFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.jMenuFileExit_actionPerformed(e);
	}
}

class RefListFrame_jMenuHelpAbout_ActionAdapter implements ActionListener {
	RefListFrame adaptee;

	RefListFrame_jMenuHelpAbout_ActionAdapter(RefListFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.jMenuHelpAbout_actionPerformed(e);
	}
}

class RefListFrame_enumComboBox_actionAdapter implements java.awt.event.ActionListener {
	RefListFrame adaptee;

	RefListFrame_enumComboBox_actionAdapter(RefListFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.enumComboBox_actionPerformed(e);
	}
}

class RefListFrame_addEUButton_actionAdapter implements java.awt.event.ActionListener {
	RefListFrame adaptee;

	RefListFrame_addEUButton_actionAdapter(RefListFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.addEUButton_actionPerformed(e);
	}
}

class RefListFrame_addEnumValButton_actionAdapter implements java.awt.event.ActionListener {
	RefListFrame adaptee;

	RefListFrame_addEnumValButton_actionAdapter(RefListFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.addEnumValButton_actionPerformed(e);
	}
}

class RefListFrame_editEnumValButton_actionAdapter implements java.awt.event.ActionListener {
	RefListFrame adaptee;

	RefListFrame_editEnumValButton_actionAdapter(RefListFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.editEnumValButton_actionPerformed(e);
	}
}

class RefListFrame_deleteEnumValButton_actionAdapter implements java.awt.event.ActionListener {
	RefListFrame adaptee;

	RefListFrame_deleteEnumValButton_actionAdapter(RefListFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.deleteEnumValButton_actionPerformed(e);
	}
}

class RefListFrame_undoDeleteEnumValButton_actionAdapter implements java.awt.event.ActionListener {
	RefListFrame adaptee;

	RefListFrame_undoDeleteEnumValButton_actionAdapter(RefListFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.undoDeleteEnumValButton_actionPerformed(e);
	}
}

class RefListFrame_selectEnumValDefaultButton_actionAdapter implements java.awt.event.ActionListener {
	RefListFrame adaptee;

	RefListFrame_selectEnumValDefaultButton_actionAdapter(RefListFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.selectEnumValDefaultButton_actionPerformed(e);
	}
}

class RefListFrame_upEnumValButton_actionAdapter implements java.awt.event.ActionListener {
	RefListFrame adaptee;

	RefListFrame_upEnumValButton_actionAdapter(RefListFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.upEnumValButton_actionPerformed(e);
	}
}

class RefListFrame_downEnumValButton_actionAdapter implements java.awt.event.ActionListener {
	RefListFrame adaptee;

	RefListFrame_downEnumValButton_actionAdapter(RefListFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.downEnumValButton_actionPerformed(e);
	}
}

class RefListFrame_editEUButton_actionAdapter implements java.awt.event.ActionListener {
	RefListFrame adaptee;

	RefListFrame_editEUButton_actionAdapter(RefListFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.editEUButton_actionPerformed(e);
	}
}

class RefListFrame_deleteEUButton_actionAdapter implements java.awt.event.ActionListener {
	RefListFrame adaptee;

	RefListFrame_deleteEUButton_actionAdapter(RefListFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.deleteEUButton_actionPerformed(e);
	}
}

class RefListFrame_undoDeleteEUButton_actionAdapter implements java.awt.event.ActionListener {
	RefListFrame adaptee;

	RefListFrame_undoDeleteEUButton_actionAdapter(RefListFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.undoDeleteEUButton_actionPerformed(e);
	}
}

class RefListFrame_addEUCnvtButton_actionAdapter implements java.awt.event.ActionListener {
	RefListFrame adaptee;

	RefListFrame_addEUCnvtButton_actionAdapter(RefListFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.addEUCnvtButton_actionPerformed(e);
	}
}

class RefListFrame_editEUCnvtButton_actionAdapter implements java.awt.event.ActionListener {
	RefListFrame adaptee;

	RefListFrame_editEUCnvtButton_actionAdapter(RefListFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.editEUCnvtButton_actionPerformed(e);
	}
}

class RefListFrame_deleteEUCnvtButton_actionAdapter implements java.awt.event.ActionListener {
	RefListFrame adaptee;

	RefListFrame_deleteEUCnvtButton_actionAdapter(RefListFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.deleteEUCnvtButton_actionPerformed(e);
	}
}

class RefListFrame_undoDelEuCnvtButton_actionAdapter implements java.awt.event.ActionListener {
	RefListFrame adaptee;

	RefListFrame_undoDelEuCnvtButton_actionAdapter(RefListFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.undoDelEuCnvtButton_actionPerformed(e);
	}
}

class RefListFrame_addDTEButton_actionAdapter implements java.awt.event.ActionListener {
	RefListFrame adaptee;

	RefListFrame_addDTEButton_actionAdapter(RefListFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.addDTEButton_actionPerformed(e);
	}
}

class RefListFrame_editDTEButton_actionAdapter implements java.awt.event.ActionListener {
	RefListFrame adaptee;

	RefListFrame_editDTEButton_actionAdapter(RefListFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.editDTEButton_actionPerformed(e);
	}
}

class RefListFrame_undoDeleteDTEButton_actionAdapter implements java.awt.event.ActionListener {
	RefListFrame adaptee;

	RefListFrame_undoDeleteDTEButton_actionAdapter(RefListFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.undoDeleteDTEButton_actionPerformed(e);
	}
}

class RefListFrame_deleteDTEButton_actionAdapter implements java.awt.event.ActionListener {
	RefListFrame adaptee;

	RefListFrame_deleteDTEButton_actionAdapter(RefListFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.deleteDTEButton_actionPerformed(e);
	}
}

//MJM commented this out -- can't refer to sutron in open-source
//class RefListFrame_addSWCButton_actionAdapter implements java.awt.event.ActionListener {
//	RefListFrame adaptee;
//
//	RefListFrame_addSWCButton_actionAdapter(RefListFrame adaptee) {
//		this.adaptee = adaptee;
//	}
//	public void actionPerformed(ActionEvent e) {
//		adaptee.addSWCButton_actionPerformed(e);
//	}
//}
//
//class RefListFrame_editSWCButton_actionAdapter implements java.awt.event.ActionListener {
//	RefListFrame adaptee;
//
//	RefListFrame_editSWCButton_actionAdapter(RefListFrame adaptee) {
//		this.adaptee = adaptee;
//	}
//	public void actionPerformed(ActionEvent e) {
//		adaptee.editSWCButton_actionPerformed(e);
//	}
//}
//
//class RefListFrame_undoDeleteSWCButton_actionAdapter implements java.awt.event.ActionListener {
//	RefListFrame adaptee;
//
//	RefListFrame_undoDeleteSWCButton_actionAdapter(RefListFrame adaptee) {
//		this.adaptee = adaptee;
//	}
//	public void actionPerformed(ActionEvent e) {
//		adaptee.undoDeleteSWCButton_actionPerformed(e);
//	}
//}
//
//class RefListFrame_deleteSWCButton_actionAdapter implements java.awt.event.ActionListener {
//	RefListFrame adaptee;
//
//	RefListFrame_deleteSWCButton_actionAdapter(RefListFrame adaptee) {
//		this.adaptee = adaptee;
//	}
//	public void actionPerformed(ActionEvent e) {
//		adaptee.deleteSWCButton_actionPerformed(e);
//	}
//}

class RefListFrame_mi_saveToDb_actionAdapter implements java.awt.event.ActionListener {
	RefListFrame adaptee;

	RefListFrame_mi_saveToDb_actionAdapter(RefListFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.mi_saveToDb_actionPerformed(e);
	}
}
