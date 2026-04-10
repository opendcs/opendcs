/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
package decodes.launcher;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import decodes.util.DecodesSettings;
import ilex.net.BasicServer;
import ilex.net.BasicSvrThread;
import ilex.util.EnvExpander;
import ilex.util.TextUtil;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

public class ProfileLauncherServer extends BasicServer implements Runnable
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
            log.atWarn().setCause(ex).log("Error on listening socket thread.");
            parentFrame.profileLauncherServer = null;
            shutdown();
        }
    }

    /**
     * Called from the GUI thread when a button has been pushed.
     * @param profileName profile name
     * @param cmd command
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

        log.warn("No profile launcher IF for '{}' and failed to start one!", profileName);
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
        log.info("Executing cmd '{}'", cmd);
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
        catch (IOException ex)
        {
            log.atError().setCause(ex).log("Unable to start profile launcher for {}", profileName);
        }

        // TODO Test whether the command works under windoze.

    }

}
