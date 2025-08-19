package decodes.dbeditor;

import java.awt.*;
import javax.swing.*;
import javax.swing.text.DefaultCaret;

import java.awt.event.*;
import java.util.ResourceBundle;

/**
Dialog to show the trace events for decoding.
Used in dbedit, platwiz, and decwiz.
*/
public class TraceDialog extends JDialog 
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	private JPanel panel1 = new JPanel();
	private BorderLayout borderLayout1 = new BorderLayout();
	private JPanel jPanel1 = new JPanel();
	private JButton closeButton = new JButton();
	private JToggleButton autoScroll = new JToggleButton("autoScroll",true);
	private JPanel jPanel2 = new JPanel();
	private FlowLayout flowLayout1 = new FlowLayout();
	private JLabel jLabel1 = new JLabel();
	private JScrollPane eventScrollPane = new JScrollPane();
	private JTextArea eventArea = new JTextArea();
	
	private int maxMessages = 20000;
	private int numMessages = 0;
	private String closeText = null;

	/**
	 * Constructor for Dialog parent.
	 * @param owner the owning component
	 * @param modal true if this dialog is to be modal.
	 */
	public TraceDialog(JDialog owner, boolean modal)
	{
		super(owner, 
			dbeditLabels.getString("TraceDialog.title"), modal);
		try 	
		{
			jbInit();
			pack();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void setTraceType(String type)
	{
		setTitle(type + " Trace");
		jLabel1.setText("Log Messages from last " + type);
	}

	/**
	 * Constructor for Frame parent.
	 * @param owner the owning component
	 * @param modal true if this dialog is to be modal.
	 */
	public TraceDialog(Frame owner, boolean modal)
	{
		super(owner, 
			dbeditLabels.getString("TraceDialog.title"), modal);
		try 	
		{
			jbInit();
			pack();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	private void jbInit() throws Exception 
	{
		//this.setSize(new Dimension(900, 400));
		panel1.setLayout(borderLayout1);
		this.setTitle(
			dbeditLabels.getString("TraceDialog.title"));
		closeButton.setText(
			genericLabels.getString("close"));
		closeButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				closeButton_actionPerformed(e);
			}
		});
		autoScroll.setSelected(true);
		jPanel2.setLayout(flowLayout1);
		jLabel1.setText(
			dbeditLabels.getString("TraceDialog.logMsgs"));
		eventArea.setEditable(false);
		eventArea.setText("");
		getContentPane().add(panel1);
		panel1.add(jPanel1, BorderLayout.SOUTH);
		jPanel1.add(closeButton, null);
		jPanel1.add(autoScroll, null);
		panel1.add(jPanel2, BorderLayout.NORTH);
		jPanel2.add(jLabel1, null);
		panel1.add(eventScrollPane, BorderLayout.CENTER);
		eventScrollPane.getViewport().add(eventArea, null);
		//eventScrollPane.setVerticalScrollBarPolicy(
		//	JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		this.setPreferredSize(new Dimension(370, 680));
	}

	void closeButton_actionPerformed(ActionEvent e) 
	{
		setVisible(false);
	}

	/**
	 * Text which if seen in addText will automatically close the dialog.
	 * @param text exact text to check for.
	 */
	void setCloseText(String text)
	{
		this.closeText = text;
	}

	/**
	 * Adds text to the dialog.
	 * @param text the text.
	 */
	public void addText(String text)
	{
		if (closeText != null && closeText.equals(text))
		{
			this.setVisible(false);
		}
		if (numMessages < maxMessages)
		{
			SwingUtilities.invokeLater(() ->
			{
				eventArea.append(text);
				if (autoScroll.isSelected())
				{
					eventArea.setCaretPosition(eventArea.getDocument().getLength());
				}
			});
			numMessages++;
		}
	}

	/**
	 * Clears the text in the scroll pane.
	 */
	public void clear()
	{
		eventArea.setText("");
		numMessages = 0;
	}
}
