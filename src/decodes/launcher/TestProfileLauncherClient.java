package decodes.launcher;

import java.io.IOException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import ilex.util.Logger;


public class TestProfileLauncherClient
{
	private String profileName = null;
	private Registry registry = null;
	private String regName = null;
	private ProfileLauncher stub = null;
	private Process launcherProcess = null;
	
	
	/** Will be null if reg already running -- means we didn't start it. */
	private Process regProcess = null;
	
	public static int REGISTRY_PORT_START = 16500;
	public static int REGISTRY_PORT_RANGE = 200;
	private int registryPort = REGISTRY_PORT_START;



	
	public TestProfileLauncherClient(String profileName)
	{
		this.profileName = profileName;
	}
	
	private void createRegistry()
		throws RemoteException
	{
		RemoteException lastException = null;
		for(registryPort = REGISTRY_PORT_START; 
			registryPort < REGISTRY_PORT_START + REGISTRY_PORT_RANGE; 
			registryPort++)
		{
			try
			{
				registry = LocateRegistry.createRegistry(registryPort);
				return;
			}
			catch (RemoteException e)
			{
				lastException = e;
				Logger.instance().info("Failed to make registry on port " + registryPort + ": " + e);
				registry = null;
			}
		}
		throw lastException;
	}
	
//	/**
//	 * Connect to the registry and lookup the stub for the ProfileLauncher object
//	 * @throws RemoteException if problem connecting to registry
//	 * @throws AccessException if cannot access registry
//	 * @throws NotBoundException if specified service is not bound
//	 */
//	private void getStub()
//		throws RemoteException, AccessException, NotBoundException
//	{
//		Logger.instance().info("Connecting to rmiregistry on localhost");
//		registry = LocateRegistry.getRegistry("localhost");
//		Logger.instance().info("Registry reference created. Looking up '" + regName + "'");
//		stub = (ProfileLauncher) registry.lookup(regName);
//	}
	
	
//	public void spawnRegistry()
//		throws IOException
//	{
//		Logger.instance().info("Trying to start registry");
//		ProcessBuilder regBuilder = new ProcessBuilder("rmiregistry");
//		regProcess = regBuilder.start();
//		try { Thread.sleep(2000L); } catch(InterruptedException e2) {}
//	}

	public void spawnLauncher()
		throws IOException
	{
		Logger.instance().info("Trying to start ProfileLauncher service...");
			
		// Assume existence of script 'startProfileLauncher'
		ProcessBuilder profBuilder = new ProcessBuilder("startProfileLauncher", 
			"-r", "" + registryPort, profileName);
		launcherProcess = profBuilder.start();
		try { Thread.sleep(5000L); } catch(InterruptedException e2) {}
	}
	
	private boolean connectToProfileLauncherSvr()
	{
		try
		{
			Logger.instance().info("Looking up '" + regName + "'");
			stub = (ProfileLauncher) registry.lookup(regName);
			return true;
		}
		catch (AccessException ex)
		{
			Logger.instance().warning("Cannot access registry: " + ex);
			return false;
		}
		catch (RemoteException ex)
		{
			Logger.instance().warning("Cannot get remote registry: " + ex);
			return false;
		}
		catch (NotBoundException ex)
		{
			Logger.instance().warning("Service '" + regName + "' not registered: " + ex);
			try 
			{
				spawnLauncher();
			}
			catch(IOException ex2)
			{
				Logger.instance().warning("Cannot spawn launcher: " + ex2);
			}
			return false;
		}
	}

	private void listRegistry()
	{
		String names[] = null;
		try
		{
			names = registry.list();
		}
		catch (AccessException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (RemoteException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Names in registry: ");
		for(String name : names)
			System.out.println("\t'" + name + "'");
	}
		
		
	public void run()
		throws Exception
	{
		regName = "Launcher:" + profileName;

		System.out.println("Creating Registry");
		createRegistry();
		System.out.println("Registry running on port " + registryPort);
		
		System.out.println("Testing profile '" + profileName + "'");
		for(int i=0; i<2; i++)
			if (connectToProfileLauncherSvr())
				break;
		if (stub == null)
		{
			System.out.println("Unable to connect to server.");
			listRegistry();
			return;
		}

		String method = null;
        try
		{
        	method = "buttonPush(FOO)";
			System.out.println(method + " returned '" + stub.buttonPush("FOO"));
			method = "getStatus";
			System.out.println(method + " returned '" + stub.getStatus());
			method = "exit";
			System.out.println(method + " returned '" + stub.exit());
			method = "unconditionalExit";
			stub.unconditionalExit();		
		}
		catch (RemoteException ex)
		{
			System.err.println("Error in '" + method + "': " + ex);
		}
 	}

	public static void main(String[] args)
		throws Exception
	{
		TestProfileLauncherClient cl = new TestProfileLauncherClient(args[0]);
		cl.run();
		
	}
}
