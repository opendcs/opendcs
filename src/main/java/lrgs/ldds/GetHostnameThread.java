/**
 *
 */
package lrgs.ldds;

import ilex.util.TextUtil;
import ilex.util.Logger;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;

import lrgs.lrgsmain.LrgsConfig;

/**
 * @author mjmaloney
 *
 */
public class GetHostnameThread
    extends Thread
{
    private static int max=20;
    private LinkedBlockingQueue<LddsThread> ltq
        = new LinkedBlockingQueue<LddsThread>();
    public static final String module = "GetHostnameThread";
    private static GetHostnameThread _instance = null;
    private String localIpMask = null;
    private int localIpMaskInt = 0;
    private int localIpAddr = 0;

    public static GetHostnameThread instance()
    {
        if (_instance == null)
		{
            _instance = new GetHostnameThread();
		}
        return _instance;
    }

    public GetHostnameThread()
    {
        super("GetHostnameThread");
        setLocalIpMask(LrgsConfig.instance().getMiscProp("localIpMask"));
    }

    private void setLocalIpMask(String lim)
    {
        localIpMask = lim;
        if (localIpMask == null || localIpMask.trim().length() == 0)
        {
            localIpMaskInt = 0;
            localIpAddr = 0;
        }
        else
        {
            String ipaddr = localIpMask.trim();
            int slash = ipaddr.indexOf('/');
            int nbits=32;
            if (slash > 0)
            {
                try
				{
					nbits = Integer.parseInt(ipaddr.substring(slash+1));
				}
                catch(Exception ex)
                {
                    Logger.instance().warning(module + " bad localIpMask setting '" + localIpMask + "': '"
                        + ex + " -- ignored.");
                    localIpMaskInt = 0;
                    localIpAddr = 0;
                    return;
                }
                ipaddr = ipaddr.substring(0, slash);
            }

            try
            {
                Inet4Address a;
                a = (Inet4Address) InetAddress.getByName(ipaddr);
                byte[] b = a.getAddress();
                int ai = ((b[0] & 0xFF) << 24) |
                    ((b[1] & 0xFF) << 16) |
                    ((b[2] & 0xFF) << 8)  |
                    ((b[3] & 0xFF) << 0);
                localIpMaskInt = -1<<(32-nbits);
                localIpAddr = ai & localIpMaskInt;
            }
            catch (UnknownHostException ex)
            {
                Logger.instance().warning(module + " unusable localIpMask setting '" + localIpMask + "': "
                    + ex + " -- ignored.");
                localIpMaskInt = 0;
                localIpAddr = 0;
                return;
            }
        }
    }


    public synchronized void enqueue(LddsThread lt)
    {
        String lim = LrgsConfig.instance().getMiscProp("localIpMask");
        if (!TextUtil.strEqualIgnoreCase(localIpMask, lim))
		{
            setLocalIpMask(lim);
		}

        if (localIpAddr != 0)
        {
            try
            {
                byte[] b = lt.getSocket().getInetAddress().getAddress();
                int ia = ((b[0] & 0xFF) << 24) |
                    ((b[1] & 0xFF) << 16) |
                    ((b[2] & 0xFF) << 8) |
                    ((b[3] & 0xFF) << 0);

                if ((ia & localIpMaskInt) == localIpAddr)
                {
                    int toDisplay = ia & (~localIpMaskInt);
                    String hostname = "local." + toDisplay;
                    lt.setHostName(hostname);
                    return;
                }
            }
            catch(Exception ex)
            {
                Logger.instance().warning(module + ".enqueue 1: " + ex);
            }
        }

        try
        {
            while (ltq.size() >= max)
            {
                ltq.take();
            }
            ltq.put(lt);
        }
        catch(InterruptedException ex)
        {
            Logger.instance().warning(module + ".enqueue 2: " + ex);
        }
    }

    private LddsThread dequeue()
    {
        try
        {
            Logger.instance().debug1(module +
                ".dequeue getting LddsThread qsize=" + ltq.size()
                + "...");
            return ltq.take();
        }
        catch (InterruptedException ex)
        {
            Logger.instance().warning(module + ".dequeue " + ex);
            return null;
        }
    }


    public void run()
    {
        while(true)
        {
            LddsThread lt = dequeue();
            if (lt != null)
            {
                Socket sock = lt.getSocket();
                InetAddress ia =
                    sock != null ? sock.getInetAddress() : null;
                Logger.instance().debug1(module +
                    " Trying name lookup for " + ia.toString());

                lt.setHostName(ia.getHostName());
                Logger.instance().debug1(module +
                    " Done. Set name to '" + lt.getHostName() + "'");
            }
            else
                Logger.instance().warning(module + ".dequeue returned null");
        }
    }
}
