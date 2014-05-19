package decodes.cwms;

import ilex.gui.LoginDialog;
import ilex.util.StringPair;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import decodes.gui.TopFrame;

public class CwmsGuiLogin 
{
	private static CwmsGuiLogin _instance = null;
	private LoginDialog dlg = null;
	private boolean loginSuccess = false;
	private String dbOfficeId = null;
	private String dbOfficePrivilege = null;
	
	public static CwmsGuiLogin instance()
	{
		if (_instance == null)
			_instance = new CwmsGuiLogin();
		return _instance;
	}
	
	public String getUserName()
	{
		return dlg == null ? null : dlg.getName();
	}
	public char[] getPassword()
	{
		return dlg == null ? null : dlg.getPassword();
	}
	
	public void doLogin(JFrame frm)
		throws Exception
	{
		if (frm == null)
			frm = TopFrame.instance();
		dlg = new LoginDialog(frm, "CWMS Database Login");
		Dimension dlgSize = dlg.getPreferredSize();
		int x = 0;
		int y = 0;
		if (frm != null)
		{
			Point loc = frm.getLocation();
			Dimension frmSize = frm.getSize();
			x = (frmSize.width - dlgSize.width) / 2 + loc.x;
			y = (frmSize.height - dlgSize.height) / 2 + loc.y;
		}
		else
		{
			java.awt.Toolkit toolkit = java.awt.Toolkit.getDefaultToolkit();
			Dimension scrSize = toolkit.getScreenSize();
			x = (scrSize.width - dlgSize.width) / 2;
			y = (scrSize.height - dlgSize.height) / 2;
		}

		dlg.setLocation(x, y);
		dlg.setVisible(true);

		loginSuccess = dlg.isOK();
	}

	public boolean isLoginSuccess() 
	{
		return loginSuccess;
	}

	public void setLoginSuccess(boolean loginSuccess) 
	{
		this.loginSuccess = loginSuccess;
	}
	
	/**
	 * Allow user to select from office IDs for which she has privilege.
	 * Sets internal office ID and privilege and returns an index into the
	 * passed array, or -1 if none selected.
	 * @param frm the frame
	 * @param officePrivileges Array of string pairs officeId/privilege
	 * @param currentId the default or currently selected office ID.
	 * @return index into array of string pairs or -1 if none selected.
	 */
	public int selectOfficeId(JFrame frm, ArrayList<StringPair> officePrivileges, String currentId)
	{
		if (frm == null)
			frm = TopFrame.instance();
		int defaultIdx = -1;
		if (officePrivileges == null || officePrivileges.size() == 0)
		{
			dbOfficeId = null;
			return -1;
		}
		if (officePrivileges.size() == 1)
		{
			dbOfficeId = officePrivileges.get(0).first;
			dbOfficePrivilege = officePrivileges.get(0).second;
			return 0;
		}
		if (currentId != null)
			for(int idx = 0; idx < officePrivileges.size(); idx++)
				if (currentId.equalsIgnoreCase(officePrivileges.get(idx).first))
				{
					defaultIdx = idx;
					break;
				}

			String ids[] = new String[officePrivileges.size()];
		for(int idx = 0; idx < ids.length; idx++)
			ids[idx] = officePrivileges.get(idx).first + " : " + officePrivileges.get(idx).second;
		String ret = (String)JOptionPane.showInputDialog(frm, 
			"Select Office ID:", "Office ID Selection", 
			JOptionPane.QUESTION_MESSAGE, null, ids, currentId);
		if (ret != null)
			for(int idx = 0; idx < ids.length; idx++)
				if (ret == ids[idx])
				{
					dbOfficeId = officePrivileges.get(idx).first;
					dbOfficePrivilege = officePrivileges.get(idx).second;
					return idx;
				}
		
		return defaultIdx;
	}
	
	public boolean isOfficeIdSelected()
	{
		return dbOfficeId != null;
	}
	
	public String getDbOfficeId() { return dbOfficeId; }

	public String getDbOfficePrivilege()
	{
		return dbOfficePrivilege;
	}
}
