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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

import javax.swing.SwingUtilities;

import ilex.net.BasicServer;
import ilex.net.BasicSvrThread;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
/**
 * This class is used by the main GUI launcher to keep connections to child launchers
 * that are handling different non-default profiles.
 */
public class ProfileLauncherConn extends BasicSvrThread
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    private String profileName = null;
    private ProfileLauncherConnState state = ProfileLauncherConnState.WAIT_PROFILE_NAME;
    private int cmdNum = 1;
    private BufferedReader input = null;
    private String module = "profLauncherIF";
    private PrintStream output = null;
    private String lastCmd = null;
    private LauncherFrame parentFrame = null;
    private boolean exitOk = false;
    private boolean receivedExitAnswer;

    protected ProfileLauncherConn(BasicServer parent, Socket socket, LauncherFrame frame)
    {
        super(parent, socket);
        parentFrame = frame;
        try
        {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            state = ProfileLauncherConnState.WAIT_PROFILE_NAME;
            output = new PrintStream(socket.getOutputStream());
        }
        catch(IOException ex)
        {
            log.atWarn().setCause(ex).log("IOException getting input stream.");
            state = ProfileLauncherConnState.DEAD;
        }

        // TODO In the code that calls the ctor, check the state afterward to make sure it isn't DEAD.
    }

    @Override
    protected void serviceClient()
    {
        String line = null;
        try { line = input.readLine(); }
        catch(IOException ex)
        {
            log.atWarn().setCause(ex).log("IOException on readLine.");
            disconnect();
            return;
        }
        if (line == null)
        {
            log.warn(module + " readLine returned null -- assuming disconnect.");
            disconnect();
            return;
        }
        if (line.trim().length() == 0)
        {
            // Shouldn't happen?
            log.warn("received empty line -- ignored.");
            return;
        }
        log.debug("received reply '{}'", line);

        switch(state)
        {
        case WAIT_PROFILE_NAME:
            setProfileName(line.trim());
            log.info("Received ID message from client. profileName={}", profileName);
            state = ProfileLauncherConnState.IDLE;
            break;
        case IDLE:
            log.warn("Received '{}' in IDLE state -- ignored.", line);
            break;
        case WAIT_CMD_RESPONSE:
            processReply(line);
            state = ProfileLauncherConnState.IDLE;
            break;
        case DEAD:
            log.warn("DEAD interface received reply '{}' -- ignored", line);
        }
    }

    /**
     * Process reply from the child launcher. Should be an echo of the command we sent:
     * <cmdnum> <cmd> [<arg>]
     * @param line
     */
    private void processReply(String line)
    {
        String words[] = line.split(" ");
        if (words == null || words.length < 2)
        {
            log.warn("processReply empty line '{}'", line);
            return;
        }

        // Check the cmd num
        try
        {
            int n = Integer.parseInt(words[0]);
            if (n != cmdNum)
            {
                // Are we out of sync with the client (e.g. we sent two commands before it could
                // reply to the first?
                log.warn("processReply expected reply to cmdNum={} but received {} -- ignored.", cmdNum, n);
                return;
            }
        }
        catch(NumberFormatException ex)
        {
            log.atWarn()
               .setCause(ex)
               .log("processReply received invalid line with no cmdNum '{}' -- ignored", line);
            return;
        }


        if (lastCmd.startsWith("exit"))
        {
            exitOk = !words[1].startsWith("error");
            receivedExitAnswer = true;
        }
        else if (words[1].startsWith("error"))
        {
            final String msg = "Error from " + module + ": " + line;
            log.warn(msg);
            SwingUtilities.invokeLater(
                new Runnable()
                {
                    public void run()
                    {
                        parentFrame.showError(msg);
                    }
                });
        }
        else
        {
            // No need to validate response. Already know it's a valid, non-error response to the
            // command I sent. It's a simple 'ACK'.
        }
    }

    public String getProfileName()
    {
        return profileName;
    }

    public void setProfileName(String profileName)
    {
        this.profileName = profileName;
        module = module + "(" + profileName + ")";
    }

    public void sendCmd(String cmd)
    {
        if (cmd.equals("exit"))
            receivedExitAnswer = false;
        lastCmd = cmd;
        String toSend = "" + (++cmdNum) + " " + cmd;
        output.println(toSend);
        log.info("sent '{}' toSend");
        state = ProfileLauncherConnState.WAIT_CMD_RESPONSE;
    }

    public ProfileLauncherConnState getIFState()
    {
        return state;
    }

    /**
     * Called after sending exit command. Wait up to 5 seconds for reply.
     * @return
     */
    public boolean isExitOk()
    {
        long start = System.currentTimeMillis();
        while(!receivedExitAnswer && System.currentTimeMillis() - start < 5000L)
            try { Thread.sleep(100L); } catch(InterruptedException ex) {}
        return !receivedExitAnswer ? true : exitOk;
    }

}
