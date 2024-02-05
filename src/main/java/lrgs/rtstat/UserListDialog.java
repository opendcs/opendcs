/*
* $Id$
*/
package lrgs.rtstat;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.ResourceBundle;

import ilex.util.AsciiUtil;
import ilex.util.AuthException;
import ilex.util.LoadResourceBundle;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import lrgs.ldds.DdsUser;

public class UserListDialog
	extends JDialog
{
	private static ResourceBundle labels = 
		RtStat.getLabels();
	private static ResourceBundle genericLabels = 
		RtStat.getGenericLabels();
	JPanel panel1 = new JPanel();
	BorderLayout borderLayout1 = new BorderLayout();
	private JPanel southPanel = new JPanel();
	private JButton okButton = new JButton();
	private JPanel eastPanel = new JPanel();
	private JButton addButton = new JButton();
	private GridBagLayout gridBagLayout1 = new GridBagLayout();
	private JButton editButton = new JButton();
	private JButton deleteButton = new JButton();
	private JScrollPane userScrollPane = new JScrollPane();
	private JTable userTable;

	private ArrayList userList;
	private UserListTableModel tableModel;
	private EditUserDialog editUserDialog;
	private String host;

	private DdsClientIf ddsClientIf = null;

	public UserListDialog(Frame owner, String title, boolean modal)
	{
		super(owner, title, modal);
		try
		{
			//setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			tableModel = new UserListTableModel();
			userTable = new SortingListTable(tableModel, UserListTableModel.colwidths);
			jbInit();
			userTable.getSelectionModel().setSelectionMode(
            	ListSelectionModel.SINGLE_SELECTION);
			
			// Note: the only way to get a user list is by being an administrator,
			// so set isAdmin=true.
			editUserDialog = new EditUserDialog(this, 
					labels.getString("UserListDialog.modUserDataTitle"), true, true);
			setPreferredSize(new Dimension(1000, 600));
			pack();
			
			userTable.addMouseListener(
				new MouseAdapter()
				{
					public void mouseClicked(MouseEvent e)
					{
						if (e.getClickCount() == 2)
							editButtonPressed();
					}
				});

		}
		catch (Exception exception)
		{
			exception.printStackTrace();
		}
		userList = null;
		getRootPane().setDefaultButton(okButton);
	}

	public UserListDialog()
	{
		this(new Frame(), "UserListDialog", false);
		try
		{
			jbInit();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public void setHost(String host)
	{
		this.host = host;
		this.setTitle(LoadResourceBundle.sprintf(
				labels.getString("RtStatFrame.ddsUserTitle"),
				host));
	}

	public void setDdsClientIf(DdsClientIf ddsClientIf)
	{
		this.ddsClientIf = ddsClientIf;
	}

	private void jbInit()
		throws Exception
	{
		panel1.setLayout(borderLayout1);
		this.setModal(true);
		okButton.setText(genericLabels.getString("OK"));
		okButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				okButton_actionPerformed(e);
			}
		});
		addButton.setToolTipText(
				labels.getString("UserListDialog.addNewUserTT"));
		addButton.setText(genericLabels.getString("add"));
		addButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				addButton_actionPerformed(e);
			}
		});
		eastPanel.setLayout(gridBagLayout1);
		editButton.setToolTipText(labels.getString(
				"UserListDialog.editInfoUserTT"));
		editButton.setText(genericLabels.getString("edit"));
		editButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				editButtonPressed();
			}
		});
		deleteButton.setToolTipText(labels.getString(
				"UserListDialog.deleteUserTT"));
		deleteButton.setText(genericLabels.getString("delete"));
		deleteButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				deleteButton_actionPerformed(e);
			}
		});
		userScrollPane.setBorder(BorderFactory.createLoweredBevelBorder());
		getContentPane().add(panel1);
		panel1.add(southPanel, java.awt.BorderLayout.SOUTH);
		southPanel.add(okButton);
		panel1.add(eastPanel, java.awt.BorderLayout.EAST);
		eastPanel.add(addButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.5
			, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
			new Insets(5, 5, 3, 5), 0, 0));
		eastPanel.add(editButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
			, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
			new Insets(3, 5, 3, 5), 0, 0));
		eastPanel.add(deleteButton, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.5
			, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
			new Insets(3, 5, 5, 5), 0, 0));
		panel1.add(userScrollPane, java.awt.BorderLayout.CENTER);
		userScrollPane.getViewport().add(userTable);
	}

	public void addButton_actionPerformed(ActionEvent e)
	{
		DdsUser newUser = new DdsUser();
		newUser.addPerm("dds");
		editUserDialog.set(host, newUser, true);
		boolean done = false;
		int tries=0;
		while(!done && tries++ < 5)
		{
			launch(editUserDialog);
			if (editUserDialog.okPressed())
			{
				try 
				{
					tableModel.addUser(newUser);
					ddsClientIf.modUser(newUser, editUserDialog.getPassword()); 
					tableModel.resort();
					done = true;
				}
				catch(AuthException ex)
				{
					JOptionPane.showMessageDialog(this,
	            		AsciiUtil.wrapString(ex.toString(),60),
						"Error!", JOptionPane.ERROR_MESSAGE);
					tableModel.deleteObject(newUser);
					done = false;
				}
			}
			else
				done = true;
		}
	}

	private void editButtonPressed()
	{
		int r = userTable.getSelectedRow();
		if (r == -1)
		{
			JOptionPane.showMessageDialog(this,
            	AsciiUtil.wrapString(labels.getString(
            			"UserListDialog.selectEditUserErr"), 60), 
				"Error!", JOptionPane.ERROR_MESSAGE);
			return;
		}
		int modelRow = userTable.convertColumnIndexToView(r);
		DdsUser ddsUserOrig = (DdsUser)tableModel.getRowObject(modelRow);
		DdsUser ddsUserCopy = new DdsUser(ddsUserOrig);
//System.out.println("UserListDialog.editButtonPressed orig.goodOnly=" + ddsUserOrig.goodOnly + ", copy.goodOnly=" + ddsUserCopy.goodOnly);
		editUserDialog.set(host, ddsUserCopy, false);

		boolean done = false;
		int tries = 0;
		while(!done && tries++ < 5)
		{
			launch(editUserDialog);
			if (editUserDialog.okPressed())
			{
				try 
				{
					ddsClientIf.modUser(ddsUserCopy, editUserDialog.getPassword());
					tableModel.deleteObject(ddsUserOrig);
					tableModel.addUser(ddsUserCopy);
					tableModel.resort();
					done = true;
				}
				catch(AuthException ex)
				{
					JOptionPane.showMessageDialog(this,
	            		AsciiUtil.wrapString(ex.toString(),60),
						"Error!", JOptionPane.ERROR_MESSAGE);
					done = false;
				}
			}
			else
				done = true;
		}
	}

	public void deleteButton_actionPerformed(ActionEvent e)
	{
		int r = userTable.getSelectedRow();
		if (r == -1)
		{
			JOptionPane.showMessageDialog(this,
            	AsciiUtil.wrapString(labels.getString(
            			"UserListDialog.selectDeleteUserErr"), 60), 
				"Error!", JOptionPane.ERROR_MESSAGE);
			return;
		}
		int modelRow = userTable.convertRowIndexToModel(r);
		DdsUser ddsUser = (DdsUser)tableModel.getRowObject(modelRow);
		try 
		{
			ddsClientIf.rmUser(ddsUser.userName);
			tableModel.deleteObject(ddsUser);
			tableModel.resort();
		}
		catch(AuthException ex)
		{
			JOptionPane.showMessageDialog(this,
           		AsciiUtil.wrapString(ex.toString(),60),
				"Error!", JOptionPane.ERROR_MESSAGE);
			return;
		}
	}

	public void okButton_actionPerformed(ActionEvent e)
	{
		setVisible(false);
	}

	public void setUsers(ArrayList<DdsUser> userList)
	{
		tableModel.setList(userList);
	}

	private void launch(JDialog dlg)
	{
		Dimension frameSize = this.getSize();
		Point frameLoc = this.getLocation();
		Dimension dlgSize = dlg.getPreferredSize();
		int xo = (frameSize.width - dlgSize.width) / 2;
		if (xo < 0) xo = 0;
		int yo = (frameSize.height - dlgSize.height) / 2;
		if (yo < 0) yo = 0;
		
		dlg.setLocation(frameLoc.x + xo, frameLoc.y + yo);
		dlg.setVisible(true);
	}
}



class UserListTableModel extends AbstractTableModel
	implements SortingListTableModel
{
	private static ResourceBundle labels = 
		RtStat.getLabels();
	private String colNames[] = null;
	private int lastSortColumn = -1;
	private ArrayList<DdsUser> userList;
	static int[] colwidths = new int[] { 12, 14, 12, 14, 10, 10, 10, 6, 6, 6 };

	public UserListTableModel()
	{
		super();
		colNames = new String[10];
		colNames[0] = labels.getString("UserListDialog.userNameColumn");
		colNames[1] = "Organization";
		colNames[2] = "Last Name";
		colNames[3] = "Email";
		colNames[4] = labels.getString("UserListDialog.permissionsColumn");
		colNames[5] = labels.getString("UserListDialog.ipAddrColumn");
		colNames[6] = labels.getString("UserListDialog.DCPLimColumn");
		colNames[7] = labels.getString("UserListDialog.localUserColumn");
		colNames[8] = "PW?";
		colNames[9] = "Susp?";
		userList = null;
	}

	public void setList(ArrayList<DdsUser> theList)
	{
		this.userList = theList;
		fireTableDataChanged();
	}

	public int getRowCount()
	{
		return userList == null ? 0 : userList.size();
	}

	DdsUser getObjectAt(int r)
	{
		return (DdsUser)getRowObject(r);
	}

	public Object getRowObject(int r)
	{
		if (r >= 0 && r < getRowCount())
			return userList.get(r);
		else return null;
	}

	void addUser(DdsUser newUser)
		throws AuthException
	{
		for(int i=0; i<userList.size(); i++)
		{
			DdsUser du = userList.get(i);
			if (du.userName.equals(newUser.userName))
				throw new AuthException(LoadResourceBundle.sprintf(
					labels.getString("UserListDialog.userNameExistsErr"),
					newUser.userName));
		}
		userList.add(newUser);
		fireTableDataChanged();
	}

	void deleteObject(DdsUser du)
	{
		userList.remove(du);
		fireTableDataChanged();
	}

	public int getColumnCount()
	{
		int r = colNames.length;
		return r;
	}

	public String getColumnName(int c)
	{
		return colNames[c];
	}

	public boolean isCellEditable(int r, int c) { return false; }

	public Object getValueAt(int r, int c)
	{
		DdsUser du = getObjectAt(r);
		if (du == null)
			return "";
		else
			return getDuColumn(du, c);
	}

	public static String getDuColumn(DdsUser du, int c)
	{
		switch(c)
		{
		case 0: return du.userName;
		case 1: return du.getOrg() == null ? "" : du.getOrg();
		case 2: return du.getLname() == null ? "" : du.getLname();
		case 3: return du.getEmail() == null ? "" : du.getEmail();
		case 4: return du.permsString();
		case 5: return du.getIpAddr() == null ? "" : du.getIpAddr();
		case 6: return du.dcpLimit == -1 ? "" : ("" + du.dcpLimit);
		case 7: return du.isLocal ? "*" : "";
		case 8:	return du.hasPassword ? "Y" : "N";
		case 9: return du.isSuspended() ? "*" : "";
		default: return "";
		}
	}

	public void resort()
	{
		if (lastSortColumn != -1)
			sortByColumn(lastSortColumn);
	}

	public void sortByColumn(int c)
	{
		lastSortColumn = c;
		Collections.sort(userList, new DdsUserComparator(c));
		fireTableDataChanged();
	}
}

class DdsUserComparator implements Comparator
{
	int column;

	public DdsUserComparator(int column)
	{
		this.column = column;
	}

	/**
	 * Compare the eqMod names of the specified type.
	 */
	public int compare(Object ob1, Object ob2)
	{
		if (ob1 == ob2)
			return 0;
		DdsUser du1 = (DdsUser)ob1;
		DdsUser du2 = (DdsUser)ob2;

		String s1 = UserListTableModel.getDuColumn(du1, column);
		String s2 = UserListTableModel.getDuColumn(du2, column);

		return s1.compareToIgnoreCase(s2);
	}

	public boolean equals(Object ob)
	{
		return false;
	}
}
