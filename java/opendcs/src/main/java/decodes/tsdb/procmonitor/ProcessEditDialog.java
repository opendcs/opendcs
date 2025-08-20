package decodes.tsdb.procmonitor;

import java.awt.BorderLayout;

import decodes.gui.GuiDialog;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.compedit.CAPEdit;
import decodes.tsdb.compedit.ProcessEditPanel;

/**
 * A non-modal dialog wrapper around ProcessEditPanel with Commit and Close buttons.
 */
@SuppressWarnings("serial")
public class ProcessEditDialog extends GuiDialog
{
	private ProcessEditPanel mainPanel = null;
	private ProcessMonitorFrame parent = null;
	
	public ProcessEditDialog(ProcessMonitorFrame parent, String title)
	{
		super(parent, title, false);
		this.parent = parent;
		guiInit();
		pack();
	}

	private void guiInit()
	{
		getContentPane().setLayout(new BorderLayout());
		mainPanel = new ProcessEditPanel(null);
		mainPanel.setTopFrame(parent);
		mainPanel.setParentDialog(this);
		getContentPane().add(mainPanel, BorderLayout.CENTER);
	}
	
	public void setEditedObject(CompAppInfo appInfo)
	{
		mainPanel.setEditedObject(appInfo);
	}
	
	public CompAppInfo getEditedObject()
	{
		return mainPanel.getEditedObject();
	}

	public void closeDlg()
	{
		setVisible(false);
		dispose();
		parent.dialogClosed(this);
	}
}
