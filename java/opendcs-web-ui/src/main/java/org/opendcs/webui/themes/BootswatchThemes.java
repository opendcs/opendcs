package org.opendcs.webui.themes;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.auto.service.AutoService;

@AutoService(ThemeSet.class)
public class BootswatchThemes implements ThemeSet
{
    @Override
    public Stream<Theme> getThemes()
    { 
        final ArrayList<Theme> themes = new ArrayList<>();
        ClassLoader loader = this.getClass().getClassLoader();
        if (loader instanceof URLClassLoader)
        {
            @SuppressWarnings("resource") // We didn't open this classloader.
            URLClassLoader urlLoader = (URLClassLoader)loader;
            for(URL url: urlLoader.getURLs())
            {
                if (url.toString().contains("bootswatch"))
                {
                    try
                    {
                        try (JarFile bsJar = new JarFile(java.net.URLDecoder.decode(url.getPath().replace("!",""), "UTF-8")))
                        {
                            bsJar.stream()
                                 .filter(je -> !je.isDirectory() && je.getName().endsWith("min.css") && !je.getName().contains("rtl"))
                                 .map(je ->
                                 {
                                    String fullName = je.getName();
                                    String link = fullName.replace("META-INF/resources", "").replaceFirst("\\/\\d+\\.\\d+\\.\\d+","");
                                    String name = link.replace("/webjars/bootswatch/dist/","").replaceFirst("\\/bootstrap\\.min\\.css", "");
                                    return new Theme(name, link);
                                 })
                                 .collect(Collectors.toCollection(() -> themes));
                        }
                    }
                    catch (IOException ex)
                    {
                        throw new RuntimeException("Unable to processes bootswatch jar.", ex);
                    }
                }
            }
        }
        return themes.stream();
    }
    
}
