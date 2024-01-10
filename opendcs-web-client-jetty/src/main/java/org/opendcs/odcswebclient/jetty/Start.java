/*
 *  Copyright 2023 OpenDCS Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcswebclient.jetty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.StdErrLog;

/**
 * Represents a Start.  This will start a jetty server.
 *
 * @author Will Jonassen
 *
 */
public class Start
{
    public static void main(String[] args) 
            throws Exception
    {
        Log.setLog(new StdErrLog());

        String warFilePath = null;
        int port = 8080;
        String contextPath = "/";
        String apiFileDetailsPath = null;
        // Initialize the JETTY server and servlet holders.
        //Server server = new Server(apiCmdLineArgs.getPort());
        ArgParser argParse = new ArgParser(args);
        String portString = argParse.switchValue("-p", "8083");
        System.out.println("Using Port: " + portString);
        try {
            warFilePath = argParse.switchValue("-w", null);
        }
        catch (Exception e)
        {
            System.out.println("War File Not Provided!");
            System.exit(1);
        }

        try {
            apiFileDetailsPath = argParse.switchValue("-f",  null);
        }
        catch (Exception e)
        {
            System.out.println("No Api File Details Path File Provided.  Using defaults.");
        }

        contextPath = argParse.switchValue("-c", "/");
        if (!contextPath.startsWith("/"))
        {
            contextPath = "/" + contextPath;
        }
        port = Integer.parseInt(portString);

        contextPath = argParse.switchValue("-c", "/");
        if (!contextPath.startsWith("/"))
        {
            contextPath = "/" + contextPath;
        }




        Server server = new Server(port);




        /******* Controlling Headers ******************/
        for(Connector y : server.getConnectors()) {
            for(ConnectionFactory x  : y.getConnectionFactories()) {
                if(x instanceof HttpConnectionFactory) {
                    //Removes Server Header
                    ((HttpConnectionFactory)x).getHttpConfiguration().setSendServerVersion(false);
                }
            }
        }

        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath(contextPath);

        System.out.println("Setting context path to : " + contextPath);

        Path warPath = Paths.get(warFilePath);
        boolean fExists = Files.exists(warPath);
        System.out.println("=============================");
        System.out.println("Ware File: " + warFilePath);
        System.out.println("War File Exists: " + fExists);
        System.out.println("=============================");
        if (!fExists)
        {
            System.out.println("War file does not exist.  Please try again and make sure to provide a valid war file.");
            System.exit(1);
        }
        // Configure JSP support.

        webapp.setWar(warFilePath);
        server.setHandler(webapp);

        webapp.setAttribute("api_details_file_path", apiFileDetailsPath);
        //This code is to make sure JSP's work.
        webapp.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",".*/[^/]*jstl.*\\.jar$");
        webapp.setExtractWAR( true );  
        org.eclipse.jetty.webapp.Configuration.ClassList classlist = org.eclipse.jetty.webapp.Configuration.ClassList.setServerDefault(server);
        classlist.addAfter("org.eclipse.jetty.webapp.FragmentConfiguration", "org.eclipse.jetty.plus.webapp.EnvConfiguration", "org.eclipse.jetty.plus.webapp.PlusConfiguration");
        classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration", "org.eclipse.jetty.annotations.AnnotationConfiguration");

        // Start the server!
        server.start();
        server.join();
    }
}