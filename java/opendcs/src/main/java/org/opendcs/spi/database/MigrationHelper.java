package org.opendcs.spi.database;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import ilex.util.EnvExpander;


public class MigrationHelper  {
    

    public static List<File> getDecodesData(Logger log)
    {
        String decodesData[] = {
            "${DCSTOOL_HOME}/edit-db/enum",
            "${DCSTOOL_HOME}/edit-db/eu/EngineeringUnitList.xml",
            "${DCSTOOL_HOME}/edit-db/datatype/DataTypeEquivalenceList.xml",
            "${DCSTOOL_HOME}/edit-db/presentation",
            "${DCSTOOL_HOME}/edit-db/loading-app",
            "${DCSTOOL_HOME}/edit-db/datasource"};
        List<File> files = MigrationHelper.getUsableFiles(decodesData, ".xml",log);
        if (log.isTraceEnabled())
        {
            for (File f: files)
            {
                log.trace("Importing '{}'", f.getAbsolutePath());
            }
        }
        return files;
    }

    public static List<File> getComputationData(Logger log)
    {
        String computationData[] = {
            "${DCSTOOL_HOME}/imports/comp-standard/algorithms.xml",
            "${DCSTOOL_HOME}/imports/comp-standard/Division.xml",
            "${DCSTOOL_HOME}/imports/comp-standard/Multiplication.xml"
            };
        return MigrationHelper.getUsableFiles(computationData, ".xml",log);
    }

    /**
        for the input array of fileNames[] (can include directories) expand and 
        return absolute paths of files (that are usable) 
    */
    public static List<File> getUsableFiles(String fileNames[], String suffix, Logger log)
    {
        List<File> files = new ArrayList<>(); 
        for (String filename: fileNames)
        {
            File f = new File(EnvExpander.expand(filename, System.getProperties()));
            if (f.exists() && f.canRead() && f.isFile())
            {
                files.add(f);
            }
            else if (f.exists() && f.canRead() && f.isDirectory())
            {
                try
                {
                    files.addAll(Files.walk(f.toPath())
                                      .map(p -> p.toFile())
                                      .filter(file -> file.isFile())
                                      .filter(file -> file.getAbsolutePath().endsWith(suffix))
                                      .collect(Collectors.toList())
                        );
                }
                catch (IOException ex)
                {
                    log.atError()
                       .setCause(ex)
                       .log("Unable to process directory '{}'", f.getAbsolutePath());
                }
            }
            else if(f.exists() && !f.canRead())
            {
                log.error("File '{}' is not readable. Skipping.", f.getAbsolutePath());
            }
            else
            {
                log.error("File '{}' is not present. Skipping.", f.getAbsolutePath());
            }
        }
        
        return files;
    }

}
