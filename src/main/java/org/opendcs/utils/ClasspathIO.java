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

public class ClasspathIO {
    /**
     * Utility to go through the classpath to find sets of resources.
     * Code intentionally ignores .class files
     * @param name starting directory.
     * @return list of URLs that were found.
     * @throws IOException
     */
    public static List<URL> getAllResourcesIn(String name) throws IOException
    {
        return getAllResourcesIn(name, ClasspathIO.class.getClassLoader());
    }

    private static List<URL> getAllResourcesIn(String name, String root, ClassLoader loader) throws IOException
    {
        List<URL> ret = new ArrayList<>();
        String path = name;
        if (path.startsWith("/"))
        {
            path = path.replaceFirst("/","");
        }
        Enumeration<URL> resources = loader.getResources(path);
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
