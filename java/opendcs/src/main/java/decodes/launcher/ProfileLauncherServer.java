package decodes.launcher;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import decodes.util.DecodesSettings;
import ilex.net.BasicServer;
import ilex.net.BasicSvrThread;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.TextUtil;

public class ProfileLauncherServer
    extends BasicServer
    implements Runnable
{
    private LauncherFrame parentFrame;
    private Thread listenThread = null;
    private String module = "ProfileLauncherServer";

    public ProfileLauncherServer(LauncherFrame parentFrame)
        throws UnknownHostException, IOException
    {
        super(DecodesSettings.instance().profileLauncherPort, InetAddress.getLocalHost());
        listenThread = new Thread(this);
        listenThread.start();
    }

    @Override
    protected BasicSvrThread newSvrThread(Socket sock)
        throws IOException
    {
        return new ProfileLauncherConn(this, sock, parentFrame);
    }

    @Override
    public void run()
    {
        try { listen(); }
        catch(Exception ex)
        {
            Logger.instance().warning(module + " Error on listening socket thread: " + ex);
            parentFrame.profileLauncherServer = null;
            shutdown();
        }
    }

    /**
     * Called from the GUI thread when a button has been pushed.
     * @param profileName
     * @return
     */
    public void sendCommandTo(String profileName, String cmd)
    {
        long start = System.currentTimeMillis();
        boolean pliStarted = false;

        do
        {
            for(Object bst : getAllSvrThreads())
            {
                ProfileLauncherConn pli = (ProfileLauncherConn)bst;
                if (TextUtil.strEqual(pli.getProfileName(), profileName))
                {
                    pli.sendCmd(cmd);
                    return;
                }
            }

            // Fell through. No current profile launcher for this profile
            if (!pliStarted)
            {
                spawnProfileLauncher(profileName);
                pliStarted = true;
            }

            try { Thread.sleep(200L); } catch (InterruptedException e) {}

        } while(System.currentTimeMillis() - start < 5000L);

        Logger.instance().warning(module + " No profile launcher IF for '" + profileName
            + "' and failed to start one!");
    }

    private void spawnProfileLauncher(String profileName)
    {
        String filesep = System.getProperty("file.separator");
        String cmd = EnvExpander.expand("$DCSTOOL_HOME") + filesep + "bin" + filesep + "launcher_start";
        String args[] = new String[5];
        args[0] = cmd;
        args[1] = "-P";
        args[2] = profileName + ".profile";
        args[3] = "-pp";
        args[4] = "" + portNum;

        //TODO NO: Use ilex.util.ProcWaiterThread
        Logger.instance().info("Executing cmd '"  + cmd);
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(args);
        try
        {
            Process child = pb.start();

            child.getErrorStream(); //stderr
            child.getInputStream(); //stdout

            // TODO - what to do with the streams???
            // TODO keep a collection of Process objects with the associated stderr/stdout input streams
            // Periodically check process.isAlive()
            // Before launcher exits call process.destroy()
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // TODO Test whether the command works under windoze.

    }

}
