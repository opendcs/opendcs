package decodes.dupdcpgui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.table.TableColumn;

import lrgs.common.DcpAddress;

import decodes.db.Database;
import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import decodes.gui.SortingListTable;
import decodes.gui.TopFrame;

/**
 * This class creates all SWING components for the Duplicate DCP GUI.
 */
public class DuplicateDcpsFrame extends TopFrame
{
	public String module = "DuplicateDcpsListFrame";
	private DuplicateDcpsTableModel dupDcpsTableModel;
	private SortingListTable dupDcpsListTable;
	private JEnumCellEditor controllingDistCombo;
//	private ArrayList<String> groupNames;
//	private HashMap<DcpAddress,ControllingDistrict> controllingDistList;
	private static boolean infoSaved;
	private String frameTitle;
	private String infoMesage;
	private String saveButtonLabel;
	private String quitButtonLabel;
	private DuplicateIo dupIo;
	private DuplicateDcpsGUI mainObj = null;
	
	/** Constructor */
	public DuplicateDcpsFrame(DuplicateDcpsGUI parent)
	{
		this.mainObj = parent;
		infoSaved = true;
		setAllLabels();
		String groupsForCombo[] = getGroupNamesForCombo(parent.getGroups());
		controllingDistCombo = new JEnumCellEditor(groupsForCombo);
		dupIo = new DuplicateIo(null, null);
		dupIo.readControllingDist();
		dupDcpsTableModel = 
			new DuplicateDcpsTableModel(this, parent.getGroups(), dupIo.getHashMap());
		dupDcpsListTable = new SortingListTable(dupDcpsTableModel, 
						new int[] {98, 90, 95, 95, 85, 115, 270, 260, 260});
		dupDcpsListTable.getTableHeader().setReorderingAllowed(false);
		dupDcpsListTable.getSelectionModel().setSelectionMode(
				ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		TableColumn tc = dupDcpsListTable.getColumnModel().getColumn(2);
		tc.setCellEditor(controllingDistCombo);

		//Create Java Swing Components
		jbInit();
		//Default operation is to do nothing when user hits 'X' in
		// upper right to close the window. We will catch the closing
		// event and do the same thing as if user had hit close.
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				doClose();
			}
		});
		pack();
	}
	/** Create all SWING components */
	private void jbInit()
	{
		JPanel mainPanel = new JPanel();
		JPanel buttonsPanel = new JPanel();
		
		JTextArea instructionsTextArea = new JTextArea();
		JScrollPane dupDcpsJScrollPane = new JScrollPane();
		JButton saveButton = new JButton(saveButtonLabel);
		JButton quitButton = new JButton(quitButtonLabel);
		instructionsTextArea.setBackground(SystemColor.window);
		instructionsTextArea.setBorder(
				BorderFactory.createLineBorder(Color.black));
		instructionsTextArea.setMinimumSize(new Dimension(205, 40));
		instructionsTextArea.setPreferredSize(new Dimension(205, 40));
		instructionsTextArea.setEditable(false);
		instructionsTextArea.setText(infoMesage);
		instructionsTextArea.setLineWrap(true);
		instructionsTextArea.setWrapStyleWord(true);
		this.setTitle(frameTitle);
		this.setSize(850,600);
		this.setContentPane(mainPanel);
		saveButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				saveButton_actionPerformed(e);
			}
		});
		quitButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				doClose();
			}
		});
		buttonsPanel.setLayout(new GridBagLayout());
		
		mainPanel.setLayout(new GridBagLayout());
		dupDcpsJScrollPane.getViewport().add(dupDcpsListTable, null);
		dupDcpsListTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		dupDcpsJScrollPane.setHorizontalScrollBarPolicy(
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		
		mainPanel.add(instructionsTextArea, new GridBagConstraints(
				0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, 
				new Insets(20, 12, 4, 12), 0, 0));
		
		mainPanel.add(dupDcpsJScrollPane, new GridBagConstraints(
				0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(20, 12, 4, 12), 0, 0));
		mainPanel.add(buttonsPanel, new GridBagConstraints(0, 2, 1,
				1, 0.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.NONE,
				new Insets(10, 12, 20, 12), 0, 0));
		
		buttonsPanel.add(saveButton, new GridBagConstraints(0, 0, 1,
				1, 0.0, 0.0, GridBagConstraints.EAST,
				GridBagConstraints.NONE,
				new Insets(0, 12, 0, 0), 0, 0));
		buttonsPanel.add(quitButton, new GridBagConstraints(1, 0, 1,
				1, 0.0, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.NONE,
				new Insets(0, 20, 0, 12), 0, 0));
	}
	
	/**
	 * Parse out the group names found on the groupNames array list.
	 * For RiverGages the Group Names have the following format:
	 * 					MVP-RIVERGAGES-DAS  
	 * For the GUI Combo box we want to display only the district name
	 * in this case "MVP".
	 */
	private String[] getGroupNamesForCombo(ArrayList<NetworkList> groups)
	{
		String retString[] = new String[groups.size()+1];
		int x=0;
		for(NetworkList nl : groups)
		{
			String name = nl.name;
			int indx = name.indexOf(DuplicateDcpsGUI.GROUP_DELIMITER_NAME);
			if (indx != -1)
				name = name.substring(0,indx);	
			retString[x] = name;
			x++;
		}
		//Add the Unresolved value to the combo box
		retString[x] = "Unresolved";
		//Sort the list Alphabetically
		java.util.Arrays.sort(retString);
		return retString;
	}

	/**
	 * Extract out District name from group name.
	 * 
	 * @param networkListName
	 * @return
	 */
	private String parseDistrictName(String networkListName)
	{
		return DuplicateIo.parseDistrictName(networkListName);
	}
	
	private void saveButton_actionPerformed(ActionEvent e)
	{
		save();
	}
	
	private void save()
	{
		//Get all rows from the table
		HashMap<DcpAddress,ControllingDistrict> cdList = 
			dupIo.getControlDistList();
		ArrayList<DuplicateDcp> duplicateTableList = dupDcpsTableModel.getList();
		
		if (duplicateTableList == null)
			return;
		for (DuplicateDcp dupDcp: duplicateTableList)
		{
			DcpAddress dcpAddress = dupDcp.getDcpAddress();
			String controlDistrict = dupDcp.getControllingDist();
			if (controlDistrict != null 
			 &&	!controlDistrict.equalsIgnoreCase("Unresolved"))
				cdList.put(dcpAddress,
					new ControllingDistrict(dcpAddress, controlDistrict));
			else
				cdList.remove(dcpAddress);
		}
		//Save all Controlling District records to a text file in the
		//DECODES_INSTALL_DIR/dcptoimport
		if (dupIo.writeControllingDist() == false)
		{
			this.showError("Can not create controlling-district.txt file." +
					"Check DECODES_INSTALL_DIR/bin/DuplicateDcpsList.log" +
					"for more information");
		}
		else
		{
			//Save successfull - display info msg
			JOptionPane.showMessageDialog(this,
			"Information saved.", "Info!", 
			JOptionPane.INFORMATION_MESSAGE);
			infoSaved = true;
		}
		//Generate _____-TOIMPORT.nl files in the DECODES_INSTALL_DIR
//		StringBuffer nlBuffer;
//		Database db = Database.getDb();
//		//To create the TOIMPORT.nl file
//		boolean save = true;
//		for (String name : groupNames)//these are network list names
//		{
//			nlBuffer = new StringBuffer("");
//			NetworkList nl = db.networkListList.getNetworkList(name);
//			if(nl == null)
//				continue;
//			for(Iterator it = nl.iterator(); it.hasNext(); )
//			{
//				//select for districtX where transportid 
//				//not in controlling dist
//				NetworkListEntry nle = (NetworkListEntry)it.next();
//				if (nle != null)
//				{	//Get the dcp address
//					String dcpAddress = nle.transportId;
//					//nl.transportMediumType;
//					//nle.description
//					//nle.platformName;
//					//address:name description:type
//					if (cdList.get(dcpAddress) == null)//dcp not in dist list
//						nlBuffer.append(dcpAddress +"\n");
//				}
//			}
//			//Add all DCPs in controlling dist table where district = districtx
//			if (cdList.values().size() > 0)
//			{
//				for (ControllingDistrict cd2 : cdList.values())
//				{
//					String controlDist = cd2.getDistrict();
//					if (controlDist != null && 
//						controlDist.equalsIgnoreCase(parseDistrictName(name)))
//					{
//						nlBuffer.append(cd2.getDcpAddress() +"\n");
//					}
//				}	
//			}
//			if (!DuplicateIo.writeToImportNlFile(nlBuffer.toString(), 
//					parseDistrictName(name)))
//			{
//				//Error - display error msg
//				save = false;
//			}
//		}
//		if (save)
//		{
//			infoSaved = true;
//			//Save successfull - display info msg
//			JOptionPane.showMessageDialog(this,
//			"Information saved.", "Info!", 
//			JOptionPane.INFORMATION_MESSAGE);
//		}
//		else
//		{
//			//Error - display error msg
//			this.showError("Can not create toimport.nl files." +
//			"Check DECODES_INSTALL_DIR/bin/DuplicateDcpsList.log" +
//			"for more information");
//		}		
	}
	
	private void doClose()
	{
		if (infoSaved == false)
		{
			int r = JOptionPane.showConfirmDialog(this, "Save any changes?");
			if (r == JOptionPane.CANCEL_OPTION)
				return;
			else if (r == JOptionPane.YES_OPTION)
			{ 
				save();
			} else if (r == JOptionPane.NO_OPTION)
			{
			}	
		}
		dispose();
		//if (exitOnClose)
			System.exit(0);
	}
	
	private void setAllLabels()
	{
		frameTitle = "Duplicate DCPs";
		infoMesage = "The Following DCPs may be contained in the " +
				"'Rivergages-DAS' network list by" +
				" multiple districts. For each platform, " +
				"select the district that " + "'owns' the DCP.";
		saveButtonLabel = "Save";
		quitButtonLabel = "Quit";
	}
	
	/** Flag to keep track when user changed something */
	public static void setInfoSaved(boolean infoSaved)
	{
		DuplicateDcpsFrame.infoSaved = infoSaved;
	}
	public DuplicateDcpsGUI getMainObj()
	{
		return mainObj;
	}
}
