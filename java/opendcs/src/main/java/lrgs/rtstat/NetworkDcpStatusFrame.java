/**
 * 
 */
package lrgs.rtstat;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.text.DefaultCaret;

/**
 * @author mjmaloney
 *
 */
public class NetworkDcpStatusFrame
    extends JFrame
{
	RtStatFrame parent;
	JEditorPane htmlPanel;
	JButton closeButton;
	
	public NetworkDcpStatusFrame(RtStatFrame parent)
	{
		super("Network DCP Detail");
		this.parent = parent;
		
		JPanel contentPane = (JPanel)this.getContentPane();
		contentPane.setLayout(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.
			HORIZONTAL_SCROLLBAR_NEVER);
		htmlPanel = new JEditorPane();
		htmlPanel.setEditable(false);
		htmlPanel.setText("");
		htmlPanel.setContentType("text/html");
		htmlPanel.setPreferredSize(new Dimension(800, 400));
		((DefaultCaret)htmlPanel.getCaret()).setUpdatePolicy(
			DefaultCaret.NEVER_UPDATE);

		scrollPane.getViewport().add(htmlPanel, null);
		contentPane.add(scrollPane, BorderLayout.CENTER);
		
		closeButton = new JButton(
			RtStat.getGenericLabels().getString("close"));
		JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		southPanel.add(closeButton);
		contentPane.add(southPanel, BorderLayout.SOUTH);
		closeButton.addActionListener(
			new java.awt.event.ActionListener()
			{
	    		public void actionPerformed(ActionEvent e)
	    		{
	    			closePressed();
	    		}
			});
		addWindowListener(
			new WindowAdapter()
			{
				public void windowClosed(WindowEvent e)
				{
//					dispose();
					notifyParent();
				}
				public void windowClosing(WindowEvent e)
				{
//					dispose();
					notifyParent();
				}
			});
		pack();
	}
	
	public void updateStatus(String htmlstat)
	{
		htmlPanel.setText(htmlstat);
	}
	
	private void closePressed()
	{
		this.dispose();
		notifyParent();
	}
	private void notifyParent()
	{
		parent.networkDcpStatusFrameClosed();
	}
}
