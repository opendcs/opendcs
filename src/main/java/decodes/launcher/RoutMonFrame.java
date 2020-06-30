package decodes.launcher;

import ilex.util.EnvExpander;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.lobobrowser.gui.*;
import org.lobobrowser.*;

import decodes.gui.TopFrame;
import decodes.util.DecodesSettings;

import javax.swing.*;
import javax.swing.border.EtchedBorder;

import lrgs.rtstat.RtStatFrame;


public class RoutMonFrame extends TopFrame
{
	private static boolean firstCall = true;
	
	public static void main(String[] args) 
		throws Exception 
	{
		// Create frame with a specific size.
		TopFrame frame = new RoutMonFrame();
		frame.setExitOnClose(true);
		frame.setVisible(true);
	}

	public RoutMonFrame() 
		throws Exception 
	{
		exitOnClose = false;
		setTitle("Retrieval and Decoding");
		
		String url = EnvExpander.expand(DecodesSettings.instance().routingMonitorUrl);

//		final BrowserPanel bp = new BrowserPanel();
//		bp.setBorder(BorderFactory.createTitledBorder(new EtchedBorder(EtchedBorder.RAISED), "Embedded browser"));
//		bp.navigate(url);
//		this.getContentPane().add(bp);
//		
//		this.addWindowListener(new java.awt.event.WindowAdapter() {
//			public void windowClosing(WindowEvent e) {
//				// This needs to be called in order
//				// to inform extensions about the
//				// window closing.
//				bp.windowClosing();
//			}
//		});
		
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		final TopFrame myframe = this;
		addWindowListener(
			new WindowAdapter()
			{
				public void windowClosed(WindowEvent e)
				{
					if (myframe.getExitOnClose())
						System.exit(0);
				}
			});

		
		FramePanel framePanel = new FramePanel();
		this.setSize(1200, 600);
		this.getContentPane().add(framePanel);
		framePanel.navigate(url);
	}
}
