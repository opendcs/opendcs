package org.opendcs.gui.x509;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.security.cert.X509Certificate;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

/**
 * Display X509 Certificate Information. For now this is just the X509Certificate::toString
 * which provides the contents in a format those familiar with X509 certificates can understand.
 * It is debatable if we can improve on this.
 */
public class X509Display extends JPanel
{

	private static final long serialVersionUID = 1L;

	/**
	 * Create the panel.
	 */
	public X509Display(X509Certificate cert)
	{
		setLayout(new BorderLayout(0, 0));
		
		JScrollPane scrollPane = new JScrollPane();
		add(scrollPane, BorderLayout.CENTER);
		
		JTextArea textArea = new JTextArea();
		textArea.setColumns(80);
		textArea.setRows(30);
		textArea.setWrapStyleWord(true);
		textArea.setText(cert.toString());
		scrollPane.setViewportView(textArea);

	}

}
