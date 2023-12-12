package decodes.launcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

import javax.swing.SwingUtilities;

import ilex.net.BasicServer;
import ilex.net.BasicSvrThread;
import ilex.util.Logger;

/**
 * This class is used by the main GUI launcher to keep connections to child launchers
 * that are handling different non-default profiles.
 */
public class ProfileLauncherConn extends BasicSvrThread
{
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
            Logger.instance().warning(module + " IOException getting input stream: " + ex);
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
            Logger.instance().warning(module + " IOException on readLine: " + ex);
            disconnect();
            return;
        }
        if (line == null)
        {
            Logger.instance().warning(module + " readLine returned null -- assuming disconnect.");
            disconnect();
            return;
        }
        if (line.trim().length() == 0)
        {
            // Shouldn't happen?
            Logger.instance().warning(module + " received empty line -- ignored.");
            return;
        }
        Logger.instance().debug1(module + " received reply '" + line + "'");

        switch(state)
        {
        case WAIT_PROFILE_NAME:
            setProfileName(line.trim());
            Logger.instance().info(module + " Received ID message from client. profileName=" + profileName);
            state = ProfileLauncherConnState.IDLE;
            break;
        case IDLE:
            Logger.instance().warning(module + " received '" + line + "' in IDLE state -- ignored.");
            break;
        case WAIT_CMD_RESPONSE:
            processReply(line);
            state = ProfileLauncherConnState.IDLE;
            break;
        case DEAD:
            Logger.instance().warning(module + " DEAD interface received reply '" + line + "' -- ignored");
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
            Logger.instance().warning(module + " processReply empty line '" + line + "'");
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
                Logger.instance().warning(module + " processReply expected reply to cmdNum=" + cmdNum
                    + " but received " + n + " -- ignored.");
                return;
            }
        }
        catch(NumberFormatException ex)
        {
            Logger.instance().warning(module + " processReply received invalid line with no cmdNum '"
                + line + "' -- ignored");
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
            Logger.instance().warning(msg);
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
        Logger.instance().info(module + " sent '" + toSend + "'");
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
