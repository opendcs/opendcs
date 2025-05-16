package org.opendcs.gui.x509;

import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.security.cert.X509Certificate;
import javax.swing.JButton;
import java.awt.FlowLayout;
import java.awt.Component;

import ilex.gui.WindowUtility;

import javax.swing.JPanel;
import javax.swing.JFrame;

public class X509CertificateVerifierDialog extends JDialog
{

	private boolean accepted = false;
	
	
	public boolean getAccepted()
	{
		return accepted;
	}

	public X509CertificateVerifierDialog(X509Certificate[] certChain, JFrame parent)
	{
		super(parent);
		getContentPane().setLayout(new BorderLayout(0, 0));
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		getContentPane().add(tabbedPane, BorderLayout.CENTER);
		
		JPanel panel = new JPanel();
		FlowLayout flowLayout = (FlowLayout) panel.getLayout();
		flowLayout.setAlignment(FlowLayout.RIGHT);
		getContentPane().add(panel, BorderLayout.SOUTH);
		
		JButton btnReject = new JButton("Reject");
		btnReject.setToolTipText("Do Not Trust the certificate");
		btnReject.addActionListener(e -> setStatusAndClose(false));
		panel.add(btnReject);
		btnReject.setAlignmentX(Component.RIGHT_ALIGNMENT);
		
		JButton btnAccept = new JButton("Accept");
		btnAccept.setToolTipText("Trust the certificate");
		btnAccept.addActionListener(e -> setStatusAndClose(true));
		panel.add(btnAccept);
		btnAccept.setAlignmentX(Component.RIGHT_ALIGNMENT);
		for(X509Certificate cert: certChain)
		{
			tabbedPane.add(cert.getSubjectX500Principal().getName(), new X509Display(cert));
		}
		pack();
		WindowUtility.center(this);
	}
	
	private void setStatusAndClose(boolean status)
	{
		accepted = status;
		setVisible(false);
	}	


	public static boolean acceptCertificate(X509Certificate[] certChain, JFrame parent)
	{
		X509CertificateVerifierDialog certDialog = new X509CertificateVerifierDialog(certChain, parent);
		certDialog.setModal(true);
		certDialog.setVisible(true);
		return certDialog.getAccepted();
	}
}
