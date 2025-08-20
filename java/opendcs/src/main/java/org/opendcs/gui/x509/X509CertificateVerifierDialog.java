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

/**
 * Dialog to present certificate chain information to the user
 * to allow accepting or rejecting of trust in the presented certificates.
 */
public class X509CertificateVerifierDialog extends JDialog
{

	private boolean accepted = false;
	
	public X509CertificateVerifierDialog(X509Certificate[] certChain, JFrame parent)
	{
		super(parent);
		setup(certChain);
	}

	public X509CertificateVerifierDialog(X509Certificate[] certChain, JDialog parent)
	{
		super(parent);
		setup(certChain);
	}

	private void setup(X509Certificate[]certChain)
	{
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

	public boolean getAccepted()
	{
		return accepted;
	}
	
	private void setStatusAndClose(boolean status)
	{
		accepted = status;
		setVisible(false);
	}	

	/**
	 * Primarily used in calls to {@link WebUtility.socketFactory} to allow GUI used to accept a certficate chain
	 * not already trusted.
	 * @param certChain
	 * @param parent JFrame derived parent
	 * @return whether or not the certificate chain was trusted.
	 */
	public static boolean acceptCertificate(X509Certificate[] certChain, JFrame parent)
	{
		X509CertificateVerifierDialog certDialog = new X509CertificateVerifierDialog(certChain, parent);
		return acceptCertificate(certDialog);
	}

	/**
	 * Primarily used in calls to {@link WebUtility.socketFactory} to allow GUI used to accept a certficate chain
	 * not already trusted.
	 * @param certChain
	 * @param parent JDialog derived parent
	 * @return whether or not the certificate chain was trusted.
	 */
	public static boolean acceptCertificate(X509Certificate[] certChain, JDialog parent)
	{
		X509CertificateVerifierDialog certDialog = new X509CertificateVerifierDialog(certChain, parent);
		return acceptCertificate(certDialog);
	}

	private static boolean acceptCertificate(X509CertificateVerifierDialog certDialog)
	{
		certDialog.setModal(true);
		certDialog.setVisible(true);
		return certDialog.getAccepted();
	}
}
