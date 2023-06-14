package org.opendcs.fixtures.helpers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import decodes.dbimport.DbImport;
import uk.org.webcompere.systemstubs.SystemStubs;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.security.SystemExit;

/**
 * Helper functions to reduce excessive code duplication
 * and increase readability of tests
 */
public class Programs
{    
    /**
     * Call DbImport on a set of files or directories. Execution is wrapped 
     * in the SystemStubs EnvironmentVariables and SystemExit stubs.
     * 
     * @param log Log file to use for this import
     * @param propertiesFile userProperties file for this execution
     * @param env SystemStubs environment instance
     * @param exit SystemStubs exit instance
     * @param filesOrDirectories list of specific files or directories. Will be expanded recursively.
     * @throws Exception If DbImport exists with a code other than 0 (or null)
     */
    public static void DbImport(File log, File propertiesFile,
                                EnvironmentVariables env, SystemExit exit,
                                String... filesOrDirectories) throws Exception
    {
        final String extensions[] = {"xml"};
        final ArrayList<File> files = new ArrayList<>();
        for(String f: filesOrDirectories)
        {
            files.addAll(FileUtils.listFiles(new File(f),extensions,true));
        }

        env.execute(() ->
                exit.execute(() ->
                    SystemStubs.tapSystemErrAndOut(() ->
                    {
                        ArrayList<String> theArgs = new ArrayList<>();
                        theArgs.add("-l"); theArgs.add(log.getAbsolutePath());
                        theArgs.add("-P"); theArgs.add(propertiesFile.getAbsolutePath());
                        theArgs.add("-d3");
                        theArgs.addAll(
                            files.stream()
                                 .map(f->f.getAbsolutePath())
                                 .collect(Collectors.toList())
                                    );
                        DbImport.main(theArgs.toArray(new String[0]));
                    })
                )
            );
        assertTrue(exit.getExitCode() == null || exit.getExitCode()==0, 
                   "System.exit called with unexpected code.");
    }
}
