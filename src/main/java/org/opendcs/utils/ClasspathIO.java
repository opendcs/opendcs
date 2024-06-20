/**
 * Copyright 2024 The OpenDCS Consortium and contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.opendcs.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClasspathIO {
    public static List<URL> getAllResourcesIn(String name) throws IOException
    {
        return getAllResourcesIn(name, ClasspathIO.class.getClassLoader());
    }

    private static List<URL> getAllResourcesIn(String name, String root, ClassLoader loader) throws IOException
    {        
        List<URL> ret = new ArrayList<>();
        Enumeration<URL> resources = loader.getResources(name);
        while(resources.hasMoreElements())
        {
            URL url = resources.nextElement();
            if (url != null && !url.toString().endsWith(".class"))
            {
                try
                {
                    if("jar".equalsIgnoreCase(url.getProtocol()))
                    {
                        JarURLConnection juc = (JarURLConnection)url.openConnection();
                        Enumeration<JarEntry> entries = juc.getJarFile().entries();
                        while(entries.hasMoreElements())
                        {
                            JarEntry je = entries.nextElement();
                            if(!je.isDirectory() && !je.getName().endsWith(".class"))
                            {
                                URL jUrl = loader.getResource(je.getName());
                                if (jUrl != null)
                                {
                                    ret.add(jUrl);
                                }
                            }
                        }
                        continue;
                    }
                    File f =  new File(url.toURI());
                    if(f.isFile())
                    {
                        ret.add(url);
                    }
                    else
                    {   
                        List<String> nextLevel = childDirectory(url);
                        for(String nextName: nextLevel)
                        {
                            String nextDir = root+"/"+nextName;
                            ret.addAll(getAllResourcesIn(nextDir,nextDir,loader)); // recourse into directories
                        }
                        
                    }
                }
                catch (URISyntaxException use)
                {
                    throw new IOException("Unable to convert a classpath URL to URI",use);
                }
            }
        }
        return ret;
    }

    public static List<URL> getAllResourcesIn(String name, ClassLoader loader) throws IOException
    {        
        return getAllResourcesIn(name,name,loader);
    }

    private static List<String> childDirectory(URL url) throws IOException
    {
        ArrayList<String> ret = new ArrayList<>();
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream())))
        {
            String line = null;
            while ((line=reader.readLine()) != null)
            {
                ret.add(line);
            }
        }
        return ret;
    }
}
