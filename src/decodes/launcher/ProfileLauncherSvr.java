package decodes.launcher;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import decodes.util.CmdLineArgs;
import decodes.util.ResourceFactory;
import ilex.cmdline.IntegerToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.Logger;


public class ProfileLauncherSvr implements ProfileLauncher
{
	String profileName = null;
	static CmdLineArgs cmdLineArgs = new CmdLineArgs(true, "proflaunch.log");
	int registryPort = -1;
	 
	public ProfileLauncherSvr(String profileName)
	{
		this.profileName = profileName;
	}

	@Override
	public String buttonPush(String label) throws RemoteException
	{
		// TODO Auto-generated method stub
		Logger.instance().info(profileName + "buttonPush(" + label + ")");
		return "buttonPush(" + label + ")";
	}

	@Override
	public String exit() throws RemoteException
	{
		// TODO Auto-generated method stub
		Logger.instance().info(profileName + "exit()");
		return "exit()";
	}

	@Override
	public void unconditionalExit()
		throws RemoteException
	{
		// TODO Auto-generated method stub
		Logger.instance().info(profileName + " unconditionalExit()");
	}
	
	@Override
	public String getStatus() throws RemoteException
	{
		// TODO Auto-generated method stub
		Logger.instance().info(profileName + "status()");
		return profileName + "-status-here!";
	}

	public void setRegistryPort(int registryPort)
	{
		this.registryPort = registryPort;
	}
	
	public static void main(String args[])
	{
		IntegerToken registryPortArg = new IntegerToken("r", "Registry Port", "",
				TokenOptions.optSwitch, -1);
		cmdLineArgs.addToken(registryPortArg);
		StringToken profileNameArg = new StringToken("", "ProfileName", "", 
			TokenOptions.optRequired|TokenOptions.optArgument, null);
		cmdLineArgs.addToken(profileNameArg);
		cmdLineArgs.parseArgs(args);
		
		String regName = "Launcher:" + profileNameArg.getValue();
		
		Logger.instance().info(
			"ProfileLauncherSvr starting OpenDCS Version " + ResourceFactory.instance().startTag());
		Logger.instance().info("profName=" + profileNameArg.getValue() + ", regName='" + regName + "'");
		try
		{
			Logger.instance().info("Creating ProfileLauncherSvr");
			ProfileLauncherSvr svr = new ProfileLauncherSvr(profileNameArg.getValue());
			svr.setRegistryPort(registryPortArg.getValue());
			Logger.instance().info("Locating registry");
			Registry registry = LocateRegistry.getRegistry("localhost", registryPortArg.getValue());
			Logger.instance().info("Exporting object");
			ProfileLauncher stub = (ProfileLauncher)UnicastRemoteObject.exportObject(svr, 0);
			Logger.instance().info("Binding");
			registry.bind(regName, stub);
			Logger.instance().info(regName + " ready.");
		}
		catch (RemoteException e)
		{
			Logger.instance().warning("Error accessing registry: " + e);
			e.printStackTrace();
			System.exit(1);
		}
		catch (AlreadyBoundException e)
		{
			Logger.instance().warning("Service '" + regName + "' already bound.");
			System.exit(1);
		}
		finally
		{
			Logger.instance().info("Exiting.");
		}
		try { Thread.sleep(10000L); } catch(InterruptedException ex) {}
	}



}
