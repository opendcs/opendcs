package decodes.launcher;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ProfileLauncher
	extends Remote
{
	public String buttonPush(String label)
		throws RemoteException;
	
	public String exit()
		throws RemoteException;
	
	public void unconditionalExit()
		throws RemoteException;

	
	public String getStatus()
		throws RemoteException;
}
