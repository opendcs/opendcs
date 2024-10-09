package lrgs.rtstat;
import javax.swing.SwingUtilities;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JFrame;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Point;

import javax.swing.JLabel;
import java.awt.GridBagConstraints;
import javax.swing.JTextField;
import java.awt.Insets;

import lrgs.common.SearchCriteria;
import lrgs.statusxml.LrgsStatusSnapshotExt;
import lrgs.statusxml.TopLevelXio;
import lrgs.ldds.*;
import ilex.gui.*;
import ilex.net.BasicClient;
import java.awt.event.*;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.UnknownHostException;

public class LrgsConnectionTest{

	private static final long serialVersionUID = 1L;


	private JPanel detailPanel = null;

	private JLabel hostnameLabel = null;

	private JTextField hostnameText = null;

	private JLabel ipAddressLabel = null;

	private JTextField ipAddressField = null;

	private JLabel portLabel = null;

	private JTextField portField = null;

	private JLabel userLabel = null;

	private JTextField userField = null;
	
	
	//private object containing the client to connect to.
	private LddsClient myClient=null;  //  @jve:decl-index=0:
	
	private BasicClient myDrgsClient=null;  //  @jve:decl-index=0:
	
	//private string holding the user name
	private String myUser;  //  @jve:decl-index=0:
	
	//private string holding the user's password
	private String myPassword;  //  @jve:decl-index=0:
	
	//private frame passed in
	private JFrame myParent=null;
	
	private JDialog myDialogParent=null;
	
	private JobDialog myDialog = null;
	
	private Thread backgroundJob = null;  //  @jve:decl-index=0:

	/**
	 * This method initializes detailPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getDetailPanel() {
		if (detailPanel == null) {
			GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
			gridBagConstraints7.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints7.gridy = 1;
			gridBagConstraints7.weightx = 1.0;
			gridBagConstraints7.anchor = GridBagConstraints.WEST;
			gridBagConstraints7.insets = new Insets(5, 5, 5, 5);
			gridBagConstraints7.gridx = 3;
			GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
			gridBagConstraints6.gridx = 2;
			gridBagConstraints6.insets = new Insets(5, 5, 5, 5);
			gridBagConstraints6.gridy = 1;
			userLabel = new JLabel();
			userLabel.setText("User:");
			GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
			gridBagConstraints5.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints5.gridy = 0;
			gridBagConstraints5.weightx = 1.0;
			gridBagConstraints5.anchor = GridBagConstraints.WEST;
			gridBagConstraints5.insets = new Insets(5, 5, 5, 5);
			gridBagConstraints5.gridx = 3;
			GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
			gridBagConstraints4.gridx = 2;
			gridBagConstraints4.weightx = 0.0D;
			gridBagConstraints4.anchor = GridBagConstraints.EAST;
			gridBagConstraints4.insets = new Insets(5, 5, 5, 5);
			gridBagConstraints4.gridy = 0;
			portLabel = new JLabel();
			portLabel.setText("Port:");
			GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
			gridBagConstraints3.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints3.gridy = 1;
			gridBagConstraints3.weightx = 1.0;
			gridBagConstraints3.anchor = GridBagConstraints.WEST;
			gridBagConstraints3.insets = new Insets(5, 5, 5, 5);
			gridBagConstraints3.gridx = 1;
			GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
			gridBagConstraints2.gridx = 0;
			gridBagConstraints2.anchor = GridBagConstraints.EAST;
			gridBagConstraints2.insets = new Insets(5, 5, 5, 5);
			gridBagConstraints2.gridy = 1;
			ipAddressLabel = new JLabel();
			ipAddressLabel.setText("IP Address:");
			GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.anchor = GridBagConstraints.EAST;
			gridBagConstraints1.insets = new Insets(5, 5, 5, 5);
			gridBagConstraints1.weightx = 1.0D;
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints.gridy = 0;
			gridBagConstraints.weightx = 1.0;
			gridBagConstraints.anchor = GridBagConstraints.WEST;
			gridBagConstraints.insets = new Insets(5, 5, 5, 5);
			gridBagConstraints.gridx = 1;
			hostnameLabel = new JLabel();
			hostnameLabel.setText("Hostname:");
			detailPanel = new JPanel();
			detailPanel.setLayout(new GridBagLayout());
			detailPanel.add(hostnameLabel, gridBagConstraints1);
			detailPanel.add(getHostnameText(), gridBagConstraints);
			detailPanel.add(ipAddressLabel, gridBagConstraints2);
			detailPanel.add(getIpAddressField(), gridBagConstraints3);
			detailPanel.add(portLabel, gridBagConstraints4);
			detailPanel.add(getPortField(), gridBagConstraints5);
			detailPanel.add(userLabel, gridBagConstraints6);
			detailPanel.add(getUserField(), gridBagConstraints7);
		}
		return detailPanel;
	}

	/**
	 * This method initializes hostnameText	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getHostnameText() {
		if (hostnameText == null) {
			hostnameText = new JTextField();
			hostnameText.setPreferredSize(new Dimension(150, 20));
			hostnameText.setEditable(false);
		}
		return hostnameText;
	}

	/**
	 * This method initializes ipAddressField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getIpAddressField() {
		if (ipAddressField == null) {
			ipAddressField = new JTextField();
			ipAddressField.setPreferredSize(new Dimension(150, 20));
			ipAddressField.setEditable(false);
		}
		return ipAddressField;
	}

	/**
	 * This method initializes portField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getPortField() {
		if (portField == null) {
			portField = new JTextField();
			portField.setPreferredSize(new Dimension(80, 20));
			portField.setEditable(false);
		}
		return portField;
	}

	/**
	 * This method initializes userField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getUserField() {
		if (userField == null) {
			userField = new JTextField();
			userField.setPreferredSize(new Dimension(80, 20));
			userField.setEditable(false);
		}
		return userField;
	}

	private void closePressed()
	{
		if(myClient.isConnected())
		{
			try { myClient.sendGoodbye(); } catch(Exception ex) {}
			myClient.disconnect();
		}
		//this.setVisible(false);
	}
	
	private void drgsClosePressed()
	{
		if(myDrgsClient.isConnected())
		{
			myDrgsClient.disconnect();
		}
		//this.setVisible(false);
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				//LddsClient tmpclient = new LddsClient("www.ilexengineering.com");
				//LrgsConnectionTest thisClass = new LrgsConnectionTest(new JFrame(),tmpclient,"ilex",null);
				
				BasicClient drgsClient = new BasicClient("dev4",1234);
				LrgsConnectionTest thisClass = new LrgsConnectionTest(new JFrame(),drgsClient);
				
				thisClass.startConnect();
			}
		});
	}

	public LrgsConnectionTest(JFrame parent, LddsClient tmp, String name, String password)
	{
		myParent = parent;
		myDialog = new JobDialog(parent, "Testing Connection With " + tmp.getHost(),true);
		myDialog.setCanCancel(true);
		
		
		setUp(tmp,name,password);
	}
	
	public LrgsConnectionTest(JDialog parent, LddsClient tmp, String name, String password)
	{
		myDialogParent = parent;
		myDialog = new JobDialog(parent, "Testing Connection With " + tmp.getHost(),true);
		myDialog.setCanCancel(true);
		
		
		setUp(tmp,name,password);
	}
	
	public LrgsConnectionTest(JFrame parent, BasicClient tmp)
	{
		myParent = parent;
		myDialog = new JobDialog(parent, "Testing DRGS Connection", true);
		myDialog.setCanCancel(true);
		
		
		setUpDrgs(tmp);
	}
	
	public LrgsConnectionTest(JDialog parent, BasicClient tmp)
	{
		myDialogParent = parent;
		myDialog = new JobDialog(parent, "Testing DRGS Connection", true);
		myDialog.setCanCancel(true);
		
		
		setUpDrgs(tmp);
	}
	
	private void setUpDrgs(BasicClient tmp) 
	{
		myDrgsClient = tmp;
		
		//Fill fields to be put into the north panel of the job dialog.
		fillDrgsFields();
		myDialog.setNorthPanel(getDetailPanel());
		
		backgroundJob =
			new Thread()
			{
				public void run()
				{
					myDialog.addToProgress("Begining Connect to " + myDrgsClient.getHost());
					try {
						myDrgsClient.connect();
					} catch (UnknownHostException e) {
						myDialog.addToProgress("Failed to Connect, Unknown Host");
						myDialog.finishedJob();
						return;
					} catch (IOException e) {
						myDialog.addToProgress("Failed to Connect, Bad Connection");
						myDialog.finishedJob();
						return;
					}
					
					if(myDrgsClient.isConnected())
					{
						myDialog.addToProgress("Connection Successful");
						
						myDialog.addToProgress("Begining Retrieval of Messages");
					
						getIpAddressField().setText(myDrgsClient.getSocket().getInetAddress().getHostAddress());
						
						InputStream mystream = myDrgsClient.getInputStream();
						
						for(int pos=0; pos<1000&&myDrgsClient.isConnected();pos++)
						{
							String feed="";
							try{feed = String.valueOf((char)mystream.read());}
							catch(Exception e)
							{
								myDialog.addToProgress("Error in message retrieval");
								myDialog.finishedJob();
								return;
							}
							myDialog.addToProgressNLF(feed);
						}
					}
				}
			};
}
	
	/**
	 * This is the default constructor
	 */
	private void setUp(LddsClient tmp, String name, String password) {
		
		myClient = tmp;
		myUser = name;
		myPassword = password;
		
		
		//Fill fields to be put into the north panel of the job dialog.
		fillFields();
		myDialog.setNorthPanel(getDetailPanel());
		
		backgroundJob =
			new Thread()
			{
				public void run()
				{
					myDialog.addToProgress("Begining Connect to " + myClient.getHost());
					try{myClient.connect();}
					catch(Exception e)
					{
						myDialog.addToProgress("Connection Failed");
						myDialog.finishedJob();
						return;
					}
					if(myClient.isConnected())
					{
						if(myPassword==null||myPassword.length()==0)
						{
							try{myClient.sendHello(myUser);}
							catch(Exception e)
							{
								myDialog.addToProgress("Username Unknown, Connection Failed: " + e.getMessage());
								myDialog.finishedJob();
								return;
							}
							myDialog.addToProgress("Connection Successful");
						}
						else
						{
							try{myClient.sendAuthHello(myUser, myPassword);}
							catch(Exception e)
							{
								myDialog.addToProgress("Username/Password Authorization Failed: " + e.getMessage());
								myDialog.finishedJob();
								return;
							}
							if(myClient.isAuthenticated())
							{
								myDialog.addToProgress("Connection Successful");
							}
							else
							{
								myDialog.addToProgress("Username/Password Authorization Failed");
								myDialog.finishedJob();
								return;
							}
						}
							
					}
					
					//Connection established, time to get messages
					
					if(myClient.isConnected())
					{
						//updates the translated IP address once connected
						getIpAddressField().setText(myClient.getSocket().getInetAddress().getHostAddress());
						
						
						SearchCriteria searchCriteria = new SearchCriteria();
						searchCriteria.setLrgsSince("now - 1 hour");
						searchCriteria.setLrgsUntil("now");
						try{myClient.sendSearchCrit(searchCriteria);}catch(Exception e){}
						
						//this.update(this.getGraphics());
						myDialog.addToProgress("Retrieving Status");
						
						byte[] status;
						TopLevelXio statusParser=null;
						LrgsStatusSnapshotExt lsse=null;
						try { statusParser = new TopLevelXio(); }
						catch(Exception ex)
						{
							System.err.println("Cannot construct XML parser: " + ex);
						}
						try{status = myClient.getStatus();}
						catch(Exception e)
						{
							myDialog.addToProgress("Failed status retreival " + e.getMessage() );
							myDialog.finishedJob();
							return;
						}
						try{lsse = statusParser.parse(status,
								0, status.length, "LRGS-Status");}
						catch(Exception e)
						{
							myDialog.addToProgress("Failed Status Parse "+ e.getMessage());
							myDialog.finishedJob();
							return;
						}
						if(lsse.isUsable)
						{
							myDialog.addToProgress("Sucessful Status Retrieval");
						}
						else
						{
							myDialog.addToProgress("Unuseable LRGS");
							myDialog.finishedJob();
							return;
						}
						
						
						
						myDialog.addToProgress("Retreiving messages");
						
						for(int pos=0;pos<100;pos++)
						{
							//this.update(this.getGraphics());
							try{myDialog.addToProgress((pos+1) + ": "+myClient.getDcpMsgSingle(1000));}
							catch(Exception e)
							{
								myDialog.addToProgress("Error Receiving Message: "+e.getMessage());
								myDialog.finishedJob();
								return;
							}
							
						}
						myDialog.addToProgress("Finished Retreiving");
						myDialog.addToProgress("All Tests Successful");
						myDialog.finishedJob();
					}
					
				}
			};
	}
	
	
	

	
	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	public void startConnect() {
		backgroundJob.start();
		launch(myDialog);
	}
	
	private void launch(JDialog dlg)
	{
		Dimension frameSize;
		Point frameLoc;
		if(myParent!=null)
		{
			frameSize = myParent.getSize();
			frameLoc = myParent.getLocation();
			
		}
		else
		{
			frameSize = myDialogParent.getSize();
			frameLoc = myDialogParent.getLocation();
		}
		Dimension dlgSize = dlg.getPreferredSize();
		int xo = (frameSize.width - dlgSize.width) / 2;
		if (xo < 0) xo = 0;
		int yo = (frameSize.height - dlgSize.height) / 2;
		if (yo < 0) yo = 0;
		
		dlg.setLocation(frameLoc.x + xo, frameLoc.y + yo);
		dlg.setVisible(true);
		
		if(myClient!=null)
		{
			closePressed();
		}
		else
		{
			drgsClosePressed();
		}
	}
	
	
	/**
	 * This method fills the fields with the client information
	 */
	private void fillFields()
	{
		getHostnameText().setText(myClient.getHost());
		getPortField().setText(String.valueOf(myClient.getPort()));
		getUserField().setText(myUser);
	}

	/**
	 * This method fills the fields with the client information
	 */
	private void fillDrgsFields()
	{
		getHostnameText().setText(myDrgsClient.getHost());
		getPortField().setText(String.valueOf(myDrgsClient.getPort()));
	}
	
}  //  @jve:decl-index=0:visual-constraint="10,10"
