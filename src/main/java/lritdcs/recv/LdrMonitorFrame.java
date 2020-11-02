package lritdcs.recv;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.EOFException;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;

import lrgs.common.DcpMsg;

public class LdrMonitorFrame extends JFrame 
{
	JPanel contentPane;
	BorderLayout borderLayout1 = new BorderLayout();
	JPanel jPanel1 = new JPanel();
	JCheckBox snapCheck = new JCheckBox();
	FlowLayout flowLayout1 = new FlowLayout();
	JPanel jPanel2 = new JPanel();
	BorderLayout borderLayout2 = new BorderLayout();
	JScrollPane jScrollPane1 = new JScrollPane();
	JCheckBox wrapCheck = new JCheckBox();
	JTextArea msgArea = new JTextArea();

	SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	static final int MAX_LENGTH = 100000;
	static final int DEL_INC    =  25000;

	//Construct the frame
	public LdrMonitorFrame() 
	{
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		try {
			jbInit();
			msgArea.setEditable(false);
			Font oldfont = msgArea.getFont();
			msgArea.setFont(
				new Font("Monospaced", Font.PLAIN, oldfont.getSize()));
			snapCheck.setSelected(true);
			wrapCheck.setSelected(true);
			msgArea.setLineWrap(true);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	//Component initialization
	private void jbInit() throws Exception	
	{
		contentPane = (JPanel) this.getContentPane();
		contentPane.setLayout(borderLayout1);
		this.setSize(new Dimension(520, 481));
		this.setTitle("Lrit Dcs Recv Monitor");
		snapCheck.setHorizontalAlignment(SwingConstants.LEFT);
		snapCheck.setText("Snap to End");
		snapCheck.addActionListener(
			new LdrMonitorFrame_snapCheck_actionAdapter(this));
		jPanel1.setLayout(flowLayout1);
		jPanel2.setLayout(borderLayout2);
		jScrollPane1.setBorder(BorderFactory.createLoweredBevelBorder());
		wrapCheck.setText("Wrap Long Lines");
		wrapCheck.addActionListener(
			new LdrMonitorFrame_wrapCheck_actionAdapter(this));
		flowLayout1.setAlignment(FlowLayout.CENTER);
		flowLayout1.setHgap(40);
		jPanel1.add(wrapCheck, null);
		contentPane.add(jPanel1, BorderLayout.SOUTH);
		jPanel1.add(snapCheck, null);
		contentPane.add(jPanel2, BorderLayout.CENTER);
		jPanel2.add(jScrollPane1, BorderLayout.CENTER);
		jScrollPane1.getViewport().add(msgArea, null);
	}

	//Overridden so we can exit when window is closed
	protected void processWindowEvent(WindowEvent e) 
	{
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING) 
		{
			System.exit(0);
		}
	}

	void snapCheck_actionPerformed(ActionEvent e) 
	{

	}

	void wrapCheck_actionPerformed(ActionEvent e) 
	{
		msgArea.setLineWrap(wrapCheck.isSelected());
	}

	void init()
	{
		LdrMonitorUpdater updater = new LdrMonitorUpdater(this);
		updater.start();
	}

	void appendMsg(DcpMsg dcpMsg)
	{
		int caret = msgArea.getCaretPosition();
		msgArea.append("---\n"
			+ "RecvTime=" 
			+ sdf.format(dcpMsg.getLocalReceiveTime()) + "\n"
			+ (new String(dcpMsg.getData()))
			+ "\n");
		int len = msgArea.getText().length();
		if (len > MAX_LENGTH)
		{
			msgArea.replaceRange("", 0, DEL_INC);
			len -= DEL_INC;
			caret -= DEL_INC;
			if (caret < 0)
				caret = 0;
			else if (caret > len)
				caret = len-1;
		}
		msgArea.setCaretPosition(snapCheck.isSelected() ? len : caret);
	}
}

class LdrMonitorFrame_snapCheck_actionAdapter implements java.awt.event.ActionListener {
	LdrMonitorFrame adaptee;

	LdrMonitorFrame_snapCheck_actionAdapter(LdrMonitorFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.snapCheck_actionPerformed(e);
	}
}

class LdrMonitorFrame_wrapCheck_actionAdapter implements java.awt.event.ActionListener {
	LdrMonitorFrame adaptee;

	LdrMonitorFrame_wrapCheck_actionAdapter(LdrMonitorFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.wrapCheck_actionPerformed(e);
	}
}

class LdrMonitorUpdater
	extends Thread
{
	LdrMonitorFrame mon;
	LritDcsRecvConfig cfg;
	MsgFile msgFile;
	long loc;
	long start;

	public LdrMonitorUpdater(LdrMonitorFrame mon)
	{
		this.mon = mon;
		cfg = LritDcsRecvConfig.instance();
		msgFile = null;
		loc = 0L;
		start = 0L;
	}

	public void run()
	{
		while(true)
		{
			if (msgFile != null)
			{
				try 
				{
					final DcpMsg dcpMsg = msgFile.readMsg(loc);
					loc = msgFile.getLocation();
					SwingUtilities.invokeLater(
						new Runnable()
						{
							public void run()
							{
								mon.appendMsg(dcpMsg);
							}
						});
				}
				catch(EOFException eof)
				{
					checkMsgFile();
					try { sleep(1000L); }
					catch(InterruptedException iex) {}
				}
				catch(IOException ioex)
				{
					System.err.println("IO Error on msg file: " + ioex);
					msgFile.close();
					msgFile = null;
				}
			}
			else
			{
				System.out.println("No File - waiting");
				checkMsgFile();
				try { sleep(1000L); }
				catch(InterruptedException iex) {}
			}
		}
	}

	private void checkMsgFile()
	{
		long now = System.currentTimeMillis();
		if (msgFile != null && now - start > 3600000L)
		{
			System.out.println("Closing old message file.");
			msgFile.close();
			msgFile = null;
		}

		if (msgFile == null)
		{
			start = (now / 3600000L) * 3600000L;
			String fn = MsgPerArch.getFileName((int)(start/1000L));
			System.out.println("Opening '" + fn + "'");
			try 
			{
				File f = new File(fn);
				long len = f.length();
				msgFile = new MsgFile(f, false); 
				loc = 0L;
				if (len > (long)mon.MAX_LENGTH)
					loc = len - mon.MAX_LENGTH;
			}
			catch(IOException ioex)
			{
				System.out.println("Cannot open '" + fn + "': " + ioex);
				msgFile = null;
			}
		}
	}
}
