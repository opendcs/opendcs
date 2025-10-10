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
package lrgs.ldds;

import ilex.util.TextUtil;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.lrgsmain.LrgsConfig;

/**
 * @author mjmaloney
 *
 */
public class GetHostnameThread extends Thread
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
            _instance.setDaemon(true); // Allow the JVM to just up and die even if this is still running.
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
                try { nbits = Integer.parseInt(ipaddr.substring(slash+1)); }
                catch(Exception ex)
                {
                    log.atWarn().setCause(ex).log("bad localIpMask setting '{}' --ignored.", localIpMask);
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
                log.atWarn().setCause(ex).log(" unusable localIpMask setting '{}' -- ignored.", localIpMask);
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
                log.atWarn().setCause(ex).log("Unable to set hostname.");
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
            log.atWarn().setCause(ex).log("Unable to clear thread queue.");
        }
    }

    private LddsThread dequeue()
    {
        try
        {
            log.debug(".dequeue getting LddsThread qsize={}...", ltq.size());
            return ltq.take();
        }
        catch (InterruptedException ex)
        {
            log.atWarn().setCause(ex).log("Unable to dequeue element.");
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
                InetAddress ia = sock != null ? sock.getInetAddress() : null;
                log.debug("Trying name lookup for {}", ia);
                if (ia != null)
                {
                    lt.setHostName(ia.getHostName());
                    log.debug("Done. Set name to '{}'", lt.getHostName());
                }
            }
            else
            {
                log.warn(".dequeue returned null");
            }
        }
    }
}