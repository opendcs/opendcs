/*
*  $Id$
*
*  $Log$
*  Revision 1.2  2009/03/24 18:30:59  mjmaloney
*  Version Info
*
*  Revision 1.1  2008/04/04 18:21:05  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/12/12 20:52:24  mjmaloney
*  dev
*
*/
package decodes.syncgui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import ilex.util.EnvExpander;
import decodes.util.DecodesVersion;

/**
About box for the SYNC gui
*/
public class SyncGuiFrame_AboutBox extends JDialog implements ActionListener 
{

	JPanel panel1 = new JPanel();
	JPanel panel2 = new JPanel();
	JPanel insetsPanel1 = new JPanel();
	JPanel insetsPanel2 = new JPanel();
	JPanel insetsPanel3 = new JPanel();
	JButton button1 = new JButton();
	JLabel imageLabel = new JLabel();
	JLabel label1 = new JLabel();
	JLabel label2 = new JLabel();
	JLabel label3 = new JLabel();
	JLabel label4 = new JLabel();
	ImageIcon image1 = new ImageIcon();
	BorderLayout borderLayout1 = new BorderLayout();
	BorderLayout borderLayout2 = new BorderLayout();
	FlowLayout flowLayout1 = new FlowLayout();
	GridLayout gridLayout1 = new GridLayout();
	String product = "DECODES Database Hub GUI";
	String version = DecodesVersion.getVersion();
	String copyright = DecodesVersion.getModifyDate();
	String comments = "Ilex Engineering, Inc. (www.ilexeng.com)";

	public SyncGuiFrame_AboutBox(Frame parent) {
		super(parent);
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		try {
			jbInit();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	SyncGuiFrame_AboutBox() {
		this(null);
	}

	//Component initialization
	private void jbInit() throws Exception	{

    	image1 = new ImageIcon(
			EnvExpander.expand("$DECODES_INSTALL_DIR/icons/gears.png"), 
			"DECODES Icon");

		imageLabel.setIcon(image1);
		this.setTitle("About");
		panel1.setLayout(borderLayout1);
		panel2.setLayout(borderLayout2);
		insetsPanel1.setLayout(flowLayout1);
		insetsPanel2.setLayout(flowLayout1);
		insetsPanel2.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		gridLayout1.setRows(4);
		gridLayout1.setColumns(1);
		label1.setText(product);
		label2.setText(version);
		label3.setText(copyright);
		label4.setText(comments);
		insetsPanel3.setLayout(gridLayout1);
		insetsPanel3.setBorder(BorderFactory.createEmptyBorder(10, 60, 10, 10));
		button1.setText("Ok");
		button1.addActionListener(this);
		insetsPanel2.add(imageLabel, null);
		panel2.add(insetsPanel2, BorderLayout.WEST);
		this.getContentPane().add(panel1, null);
		insetsPanel3.add(label1, null);
		insetsPanel3.add(label2, null);
		insetsPanel3.add(label3, null);
		insetsPanel3.add(label4, null);
		panel2.add(insetsPanel3, BorderLayout.CENTER);
		insetsPanel1.add(button1, null);
		panel1.add(insetsPanel1, BorderLayout.SOUTH);
		panel1.add(panel2, BorderLayout.NORTH);
		setResizable(true);
	}

	//Overridden so we can exit when window is closed
	protected void processWindowEvent(WindowEvent e) {
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			cancel();
		}
		super.processWindowEvent(e);
	}

	//Close the dialog
	void cancel() {
		dispose();
	}

	//Close the dialog on a button event
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == button1) {
			cancel();
		}
	}
}
