package lritdcs;

import ilex.util.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import ch.ethz.ssh2.Connection;

public class LritDcsConnection implements Observer

{

	private static LritDcsConnection _instance = null;

	private Connection connDom2A = null;
	private Connection connDom2B = null;
	private Connection connDom2C = null;

	LritDcsConfig cfg;
	private String dom2AHostName;
	private String dom2BHostName;
	private String dom2CHostName;
	private String dom2AUserName;
	private String dom2BUserName;
	private String dom2CUserName;
	public String connAStatus = "";
	public String connBStatus = "";
	public String connCStatus = "";
	public long lastDom2AConnAttempt = 0L;
	public long lastDom2BConnAttempt = 0L;
	public long lastDom2CConnAttempt = 0L;
	

	/**
	 * Singleton class LritDcsConnection is used to maintain connections and
	 * sessions with Domain 2(A,B,C) servers.
	 */
	private LritDcsConnection() 
	{
		cfg = LritDcsConfig.instance();

		dom2AHostName = cfg.getDom2AHostName();
		dom2AUserName = cfg.getDom2AUser();

		dom2BHostName = cfg.getDom2BHostName();
		dom2BUserName = cfg.getDom2BUser();

		dom2CHostName = cfg.getDom2CHostName();
		dom2CUserName = cfg.getDom2CUser();
		connectSessionDom2A();
		connectSessionDom2B();
		connectSessionDom2C();
	}

	/**
	 * This method implements singleton behaviour for class LritDcsConnection
	 */
	public static LritDcsConnection instance() {
		if (_instance == null)
			_instance = new LritDcsConnection();
		return _instance;
	}

	/**
	 * This method makes connection with Domain 2A
	 */
	public void connectSessionDom2A() 
	{
		dom2AHostName = cfg.getDom2AHostName();
		dom2AUserName = cfg.getDom2AUser();

		lastDom2AConnAttempt = System.currentTimeMillis();
		if (dom2AHostName == null || dom2AHostName.trim().length() == 0)
		{
			setConnDom2A(null);
			return;
		}
		
		File keyfile = new File(LritDcsMain.instance().getRsaKeyFile());
		if (!keyfile.canRead())
		{
			Logger.instance().failure("RSA Key File '" + keyfile.getPath()
				+ "' not readable. Edit lritdcs.conf and check permissions.");
			connAStatus = "Cannot read RSA Key File";
			return;
		}

		String keyfilePass = "";
		try 
		{
			/* Create a connection instance */
			connDom2A = new Connection(dom2AHostName);
			connDom2A.connect();
		}
		catch (Exception e)
		{
			setConnDom2A(null);
			connAStatus = dom2AHostName + " Connection Failed " + e.getMessage()+". "+e.getCause();
			Logger.instance().failure("Domain2A " + connAStatus);
			
			return;
		}
		
		try
		{
			/* Authenticate */
			Logger.instance().info("Authenticating with Domain 2A host="
				+ dom2AHostName + ", user=" + dom2AUserName
				+ ", keyfile='" + keyfile.getPath()+ "'");
			boolean isAuthenticated = connDom2A.authenticateWithPublicKey(
					dom2AUserName, keyfile, keyfilePass);

			if (isAuthenticated == false) {
				throw new IOException("Authentication failed for Domain A.");
			}
			setConnDom2A(connDom2A);
		}
		catch (IOException e)
		{
			setConnDom2A(null);
			connAStatus = dom2AHostName + " Authentication Failed " + e.getMessage()+". "+e.getCause();
			Logger.instance().failure("Domain2A " + connAStatus);
		}

	}

	/**
	 * This method makes connection with Domain 2B
	 */
	public void connectSessionDom2B()
	{
		dom2BHostName = cfg.getDom2BHostName();
		dom2BUserName = cfg.getDom2BUser();
		lastDom2AConnAttempt = System.currentTimeMillis();
		if (dom2BHostName == null || dom2BHostName.trim().length() == 0)
		{
			setConnDom2B(null);
			return;
		}

		File keyfile = new File(LritDcsMain.instance().getRsaKeyFile());
		if (!keyfile.canRead())
		{
			Logger.instance().failure("RSA Key File '" + keyfile.getPath()
				+ "' not readable. Edit lritdcs.conf and check permissions.");
			connBStatus = "Cannot read RSA Key File";
			return;
		}

		String keyfilePass = "";
		try 
		{
			/* Create a connection instance */
			connDom2B = new Connection(dom2BHostName);
			connDom2B.connect();
		}
		catch (Exception e)
		{
			setConnDom2B(null);
			connBStatus = dom2BHostName + " Connection Failed " + e.getMessage()+". "+e.getCause();
			Logger.instance().failure("Domain2B " + connBStatus);
			return;
		}
		try {

			/* Authenticate */
			Logger.instance().info("Authenticating with Domain 2B host="
				+ dom2BHostName + ", user=" + dom2BUserName
				+ ", keyfile='" + keyfile.getPath()+ "'");
			boolean isAuthenticated = connDom2B.authenticateWithPublicKey(
					dom2BUserName, keyfile, keyfilePass);

			if (isAuthenticated == false)
			{
				//Logger.instance().failure("ERROR !!! Authentication failed for Domain B" );
				throw new IOException("Authentication failed for Domain B.");
			}
			setConnDom2B(connDom2B);

		}
		catch (IOException e)
		{
			setConnDom2B(null);
			connBStatus = dom2BHostName + " Authentication Failed " + e.getMessage()+". "+e.getCause();
			Logger.instance().failure("Domain2B " + connBStatus);
		}

	}

	/**
	 * This method makes connection with Domain 2C
	 */
	public void connectSessionDom2C() 
	{
		dom2CHostName = cfg.getDom2CHostName();
		dom2CUserName = cfg.getDom2CUser();
		lastDom2CConnAttempt = System.currentTimeMillis();
		if (dom2CHostName == null || dom2CHostName.trim().length() == 0)
		{
			setConnDom2C(null);
			return;
		}
		
		File keyfile = new File(LritDcsMain.instance().getRsaKeyFile());
		if (!keyfile.canRead())
		{
			Logger.instance().failure("RSA Key File '" + keyfile.getPath()
				+ "' not readable. Edit lritdcs.conf and check permissions.");
			connCStatus = "Cannot read RSA Key File";
			return;
		}

		String keyfilePass = "";
		try 
		{
			/* Create a connection instance */
			connDom2C = new Connection(dom2CHostName);
			connDom2C.connect();
		}
		catch (Exception e)
		{
			setConnDom2C(null);
			connCStatus = dom2CHostName + " Connection Failed " + e.getMessage()+". "+e.getCause();
			Logger.instance().failure("Domain2C " + connCStatus);
			return;
		}
		try {
			/* Authenticate */
			Logger.instance().info("Authenticating with Domain 2C host="
				+ dom2CHostName + ", user=" + dom2CUserName
				+ ", keyfile='" + keyfile.getPath()+ "'");

			boolean isAuthenticated = connDom2C.authenticateWithPublicKey(
					dom2CUserName, keyfile, keyfilePass);

			if (isAuthenticated == false) {
				//Logger.instance().failure("ERROR !!! Authentication failed for Domain C" );
				throw new IOException("Authentication failed for Domain C.");
			}
			setConnDom2C(connDom2C);

		} catch (Exception e) {			
			setConnDom2C(null);
			connCStatus = dom2CHostName + " Authentication Failed " + e.getMessage()+". "+e.getCause();
			Logger.instance().failure("Domain2C " + connCStatus);
		}

	}

	/**
	 * Returns Connection with Domain 2A
	 * 
	 * @return the connDom2A
	 */
	public Connection getConnDom2A() {
		return connDom2A;
	}

	/**
	 * Sets value for Connection with Domain 2A
	 * 
	 * @param connDom2A
	 *            the connDom2A to set
	 */
	public void setConnDom2A(Connection connDom2A) {
		this.connDom2A = connDom2A;
	}

	/**
	 * Returns Connection with Domain 2B
	 * 
	 * @return the connDom2B
	 */
	public Connection getConnDom2B() {
		return connDom2B;
	}

	/**
	 * Sets value for Connection with Domain 2B
	 * 
	 * @param connDom2B
	 *            the connDom2B to set
	 */
	public void setConnDom2B(Connection connDom2B) {
		this.connDom2B = connDom2B;
	}

	/**
	 * Returns Connection with Domain 2C
	 * 
	 * @return the connDom2C
	 */
	public Connection getConnDom2C() {
		return connDom2C;
	}

	/**
	 * Sets value for Connection with Domain 2C
	 * 
	 * @param connDom2C
	 *            the connDom2C to set
	 */
	public void setConnDom2C(Connection connDom2C) {
		this.connDom2C = connDom2C;
	}

	protected void registerForConfigUpdates() {
		LritDcsConfig.instance().addObserver(this);
	}

	@Override
	public void update(Observable o, Object arg) {
		getConfigValues(LritDcsConfig.instance());

	}

	protected synchronized void getConfigValues(LritDcsConfig cfg) 
	{

		String strDomAName = cfg.getDom2AHostName();
		String strDomAUserName = cfg.getDom2AUser();

		String strDomBName = cfg.getDom2BHostName();
		String strDomBUserName = cfg.getDom2BUser();

		String strDomCName = cfg.getDom2CHostName();
		String strDomCUserName = cfg.getDom2CUser();

		if (!dom2AHostName.equals(strDomAName)
				|| !dom2AUserName.equals(strDomAUserName)) 
		{
			dom2AHostName = cfg.getDom2AHostName();
			dom2AUserName = cfg.getDom2AUser();
			if (connDom2A != null)
				connDom2A.close();
			connectSessionDom2A();
		}

		if (!dom2BHostName.equals(strDomBName)
				|| !dom2BUserName.equals(strDomBUserName))
		{
			dom2BHostName = cfg.getDom2BHostName();
			dom2BUserName = cfg.getDom2BUser();
			if (connDom2B != null)
				connDom2B.close();
			connectSessionDom2B();
		}

		if (!dom2CHostName.equals(strDomCName)
				|| !dom2CUserName.equals(strDomCUserName))
		{
			dom2CHostName = cfg.getDom2CHostName();
			dom2CUserName = cfg.getDom2CUser();
			if (connDom2C != null)
				connDom2C.close();
			connectSessionDom2C();
		}

	}

	/**
	 * Closes all connections with Domain 2 (A,B,C) servers.
	 */
	public void closeConnections() {
		if (connDom2A != null)
			connDom2A.close();

		if (connDom2B != null)
			connDom2B.close();

		if (connDom2C != null)
			connDom2C.close();
	}

	/**
	 * Opens connections with Domain 2 (A,B,C) servers.
	 */
	public void openConnections() 
	{
		if (connDom2A != null)
			connDom2A.close();
		connectSessionDom2A();

		if (connDom2B != null)
			connDom2B.close();
		connectSessionDom2B();

		if (connDom2C != null)
			connDom2C.close();
		connectSessionDom2C();
	}
}
