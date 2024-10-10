/*
*	$Id$
*
*	$Log$
*	Revision 1.2  2009/03/24 18:30:25  mjmaloney
*	Version Info
*	
*	Revision 1.1  2008/04/04 18:21:04  cvs
*	Added legacy code to repository
*	
*	Revision 1.7  2008/02/10 20:17:34  mmaloney
*	dev
*	
*	Revision 1.2  2008/02/01 15:20:40  cvs
*	modified files for internationalization
*	
*	Revision 1.6  2004/12/21 14:46:06  mjmaloney
*	Added javadocs
*	
*	Revision 1.5  2004/04/30 15:29:38  mjmaloney
*	6.1 beta release prep.
*	
*	Revision 1.4  2004/04/29 19:14:48  mjmaloney
*	6.1 release prep
*	
*	Revision 1.3  2004/04/20 17:05:54  mjmaloney
*	Implementation of RefListEditor
*	
*	Revision 1.2	2004/02/03 16:01:03	mjmaloney
*	prototype dev.
*
*	Revision 1.1	2004/02/02 22:12:58	mjmaloney
*	dev.
*
*/
package decodes.rledit;

import java.awt.*;
import java.awt.event.*;
import java.util.ResourceBundle;

import javax.swing.*;
import javax.swing.border.*;

import decodes.util.DecodesVersion;
import ilex.util.EnvExpander;

/**
The "about box" for the reference list editor.
*/
public class RefListFrame_AboutBox extends JDialog implements ActionListener 
{
	private static ResourceBundle genericLabels = 
		RefListEditor.getGenericLabels();
	private static ResourceBundle labels = RefListEditor.getLabels();
	private JPanel panel1 = new JPanel();
	private JPanel panel2 = new JPanel();
	private JPanel insetsPanel1 = new JPanel();
	private JPanel insetsPanel2 = new JPanel();
	private JPanel insetsPanel3 = new JPanel();
	private JButton button1 = new JButton();
	private JLabel imageLabel = new JLabel();
	private JLabel label1 = new JLabel();
	private JLabel label2 = new JLabel();
	private JLabel label3 = new JLabel();
	private JLabel label4 = new JLabel();
	private ImageIcon image1 = new ImageIcon();
	private BorderLayout borderLayout1 = new BorderLayout();
	private BorderLayout borderLayout2 = new BorderLayout();
	private FlowLayout flowLayout1 = new FlowLayout();
	private GridLayout gridLayout1 = new GridLayout();
	private String product = DecodesVersion.getAbbr();
	private String version = "Version " + DecodesVersion.getVersion();
	private String copyright = "Modified " + DecodesVersion.getModifyDate();
	private String comments = labels.getString(
							"RefListFrameAboutBox.comments");

	/**
	 * Constructor.
	 * @param parent the parent frame
	 */
	public RefListFrame_AboutBox(Frame parent) 
	{
		super(parent);
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		try {
			jbInit();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * No args constructor for JBuilder.
	 */
	RefListFrame_AboutBox() {
		this(null);
	}

	/** Initializes GUI components. */
	private void jbInit() throws Exception	{
		image1 = new ImageIcon(
			EnvExpander.expand("$DECODES_INSTALL_DIR/icons/gears.png"), "DECODES Icon");

		imageLabel.setIcon(image1);
		this.setTitle(genericLabels.getString("about"));
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
		button1.setText(genericLabels.getString("OK"));
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

	/**
	  Called when windows is closed.
	  @param e the window event.
	*/
	protected void processWindowEvent(WindowEvent e) {
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			cancel();
		}
		super.processWindowEvent(e);
	}

	/** Close the dialog. */
	void cancel() {
		dispose();
	}

	/** Close the dialog on a button event. */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == button1) {
			cancel();
		}
	}
}
