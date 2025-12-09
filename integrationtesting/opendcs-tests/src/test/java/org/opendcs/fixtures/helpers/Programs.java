package org.opendcs.fixtures.helpers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import decodes.dbimport.DbImport;
import decodes.tsdb.ImportCompApp;
import decodes.tsdb.TsImport;
import decodes.util.ExportTimeSeries;
import uk.org.webcompere.systemstubs.SystemStubs;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.properties.SystemProperties;
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
                                EnvironmentVariables env, SystemExit exit, SystemProperties properties,
                                String... filesOrDirectories) throws Exception
    {
        final String extensions[] = {"xml"};
        final ArrayList<File> files = gatherFiles(extensions, filesOrDirectories);

        properties.execute(() ->
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
            )
        );
        assertTrue(exit.getExitCode() == null || exit.getExitCode()==0,
                   "System.exit called with unexpected code.");
    }

    /**
     * Call CompImport on a set of files or directories. Execution is wrapped
     * in the SystemStubs EnvironmentVariables and SystemExit stubs.
     *
     * @param log Log file to use for this import
     * @param propertiesFile userProperties file for this execution
     * @param env SystemStubs environment instance
     * @param exit SystemStubs exit instance
     * @param filesOrDirectories list of specific files or directories. Will be expanded recursively.
     * @throws Exception If DbImport exists with a code other than 0 (or null)
     */
    public static void CompImport(File log, File propertiesFile,
                                  EnvironmentVariables env, SystemExit exit,
                                  String... filesOrDirectories) throws Exception
    {
        final String extensions[] = {"xml"};
        final ArrayList<File> files = gatherFiles(extensions, filesOrDirectories);

        env.execute(() ->
                exit.execute(() ->
                    SystemStubs.tapSystemErrAndOut(() ->
                    {
                        ArrayList<String> theArgs = new ArrayList<>();
                        theArgs.add("-l"); theArgs.add(log.getAbsolutePath());
                        theArgs.add("-P"); theArgs.add(propertiesFile.getAbsolutePath());
                        theArgs.add("-d3");
                        theArgs.add("-C");
                        theArgs.addAll(
                            files.stream()
                                 .map(f->f.getAbsolutePath())
                                 .collect(Collectors.toList())
                                    );
                        ImportCompApp.main(theArgs.toArray(new String[0]));
                    })
                )
            );
        assertTrue(exit.getExitCode() == null || exit.getExitCode()==0,
                   "System.exit called with unexpected code.");
    }

    /**
     * Call TsImport on a set of files. Execution is wrapped
     * in the SystemStubs EnvironmentVariables and SystemExit stubs.
     *
     * @param log Log file to use for this import
     * @param propertiesFile userProperties file for this execution
     * @param env SystemStubs environment instance
     * @param exit SystemStubs exit instance
     * @param tsImportFiles list of specific files or directories. Will be expanded recursively.
     * @throws Exception If DbImport exists with a code other than 0 (or null)
     */
    public static void ImportTs(File log, File propertiesFile,
                                  EnvironmentVariables env, SystemExit exit,
                                  String... tsImportFiles) throws Exception
    {
        final String extensions[] = {"tsimport"};
        final ArrayList<File> files = gatherFiles(extensions, tsImportFiles);

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
                        TsImport.main(theArgs.toArray(new String[0]));
                    })
                )
            );
        assertTrue(exit.getExitCode() == null || exit.getExitCode()==0,
                   "System.exit called with unexpected code.");
    }

    /**
     * Call DeleteTs on a set of names. Execution is wrapped
     * in the SystemStubs EnvironmentVariables and SystemExit stubs.
     *
     * @param log Log file to use for this import
     * @param propertiesFile userProperties file for this execution
     * @param env SystemStubs environment instance
     * @param exit SystemStubs exit instance
     * @param start start date in "dd-mmm-yyyy/HH:MM"
     * @param end end date in same format as start
     * @param tz time zone name
     * @param tsNames list of specific timeseries to delete
     * @throws Exception If DbImport exists with a code other than 0 (or null)
     */
    public static void DeleteTs(File log, File propertiesFile,
                                  EnvironmentVariables env, SystemExit exit, String start,
                                  String end, String tz,
                                  String... tsNames) throws Exception
    {
        env.execute(() ->
                exit.execute(() ->
                    SystemStubs.tapSystemErrAndOut(() ->
                    {
                        ArrayList<String> theArgs = new ArrayList<>();
                        theArgs.add("-l"); theArgs.add(log.getAbsolutePath());
                        theArgs.add("-P"); theArgs.add(propertiesFile.getAbsolutePath());
                        theArgs.add("-S"); theArgs.add(start);
                        theArgs.add("-U"); theArgs.add(end);
                        theArgs.add("-Z"); theArgs.add(tz);

                        theArgs.add("-d3");
                        for (String tsName: tsNames)
                        {
                            theArgs.add(tsName);
                        }

                        decodes.tsdb.DeleteTs.main(theArgs.toArray(new String[0]));
                    })
                )
            );
        assertTrue(exit.getExitCode() == null || exit.getExitCode()==0,
                   "System.exit called with unexpected code.");
    }


    /**
     * Call OutputTs on a set of names. Execution is wrapped
     * in the SystemStubs EnvironmentVariables and SystemExit stubs.
     *
     * @param log Log file to use for this import
     * @param propertiesFile userProperties file for this execution
     * @param env SystemStubs environment instance
     * @param exit SystemStubs exit instance
     * @param start start date in "dd-mmm-yyyy/HH:MM"
     * @param end end date in same format as start
     * @param tz time zone name
     * @param presentationGroup configuration for output format
     * @param tsNames list of specific timeseries to delete
     * @throws Exception If DbImport exists with a code other than 0 (or null)
     */
    public static String OutputTs(File log, File propertiesFile,
                                  EnvironmentVariables env, SystemExit exit, String start,
                                  String end, String tz, String presentationGroup,
                                  String... tsNames) throws Exception
    {
        final ByteArrayOutputStream bao = new ByteArrayOutputStream();
        final PrintStream ps = new PrintStream(bao);
        env.execute(() ->
            exit.execute(() ->
            {
                ExportTimeSeries ets = new ExportTimeSeries();
                ets.setOutputStream(ps);
                ArrayList<String> theArgs = new ArrayList<>();
                theArgs.add("-l"); theArgs.add(log.getAbsolutePath());
                theArgs.add("-P"); theArgs.add(propertiesFile.getAbsolutePath());
                theArgs.add("-d3");
                theArgs.add("-S"); theArgs.add(start);
                theArgs.add("-U"); theArgs.add(end);
                theArgs.add("-Z"); theArgs.add(tz);
                theArgs.add("-G"); theArgs.add(presentationGroup);
                for (String tsName: tsNames)
                {
                    theArgs.add(tsName);
                }
                ets.execute(theArgs.toArray(new String[0]));
            })
        );
        String output = bao.toString("UTF8");
        assertTrue(exit.getExitCode() == null || exit.getExitCode()==0,
                () -> "System.exit called with unexpected code. Output = " + output);
        return output;
    }



    /**
     * Call CpCompDependsUpdater for full eval. Execution is wrapped
     * in the SystemStubs EnvironmentVariables and SystemExit stubs.
     *
     * @param log Log file to use for this import
     * @param propertiesFile userProperties file for this execution
     * @param env SystemStubs environment instance
     * @param exit SystemStubs exit instance
     * @throws Exception If DbImport exists with a code other than 0 (or null)
     */
    public static void UpdateComputationDependencies(File log, File propertiesFile,
                                  EnvironmentVariables env, SystemExit exit) throws Exception
    {
        env.execute(() ->
            exit.execute(() ->
                SystemStubs.tapSystemErrAndOut(() ->
                {
                    ArrayList<String> theArgs = new ArrayList<>();
                    theArgs.add("-l"); theArgs.add(log.getAbsolutePath());
                    theArgs.add("-P"); theArgs.add(propertiesFile.getAbsolutePath());
                    theArgs.add("-d3");
                    theArgs.add("-O");
                    decodes.tsdb.CpCompDependsUpdater.main(theArgs.toArray(new String[0]));
                })
            )
        );

        assertTrue(exit.getExitCode() == null || exit.getExitCode()==0,
                   "System.exit called with unexpected code.");
    }

    /**
     * Call CompExec on a set of comps, timeseries, or groups. Execution is wrapped
     * in the SystemStubs EnvironmentVariables and SystemExit stubs.
     *
     * @param log Log file to use for this import
     * @param propertiesFile userProperties file for this execution
     * @param env SystemStubs environment instance
     * @param exit SystemStubs exit instance
     * @param start start date in "dd-mmm-yyyy/HH:MM"
     * @param end end date in same format as start
     * @param tz time zone name
     * @param outputFormat valid DECODES output formatter
     * @param presentationGroup DECODES Presentation Group to use
     * @param controlFileName control file to specify which computations, groups and/or time series to execute
     * @param quiet quiet mode. No prompts or statistics will be written to the standard output
     * @param comps list of specific computation ids to run
     * @param groups list of specific timeseries to run compute for
     * @param tsNames list of specific timeseries groups to run compute for
     * @throws Exception If DbImport exists with a code other than 0 (or null)
     */
    public static void RunCompExec(File log, File propertiesFile,
                                  EnvironmentVariables env, SystemExit exit, String start,
                                  String end, String tz, String outputFormat, String presentationGroup,
                                  String controlFileName, boolean quiet, String[] comps, String[] groups,
                                  String[] tsNames) throws Exception
    {
        env.execute(() ->
                exit.execute(() ->
                    SystemStubs.tapSystemErrAndOut(() ->
                    {
                        ArrayList<String> theArgs = new ArrayList<>();
                        theArgs.add("-l"); theArgs.add(log.getAbsolutePath());
                        theArgs.add("-P"); theArgs.add(propertiesFile.getAbsolutePath());
                        theArgs.add("-S"); theArgs.add(start);
                        theArgs.add("-U"); theArgs.add(end);
                        theArgs.add("-Z"); theArgs.add(tz);
                        theArgs.add("-o"); theArgs.add(outputFormat);
                        theArgs.add("-R"); theArgs.add(presentationGroup);
                        theArgs.add("-f"); theArgs.add(controlFileName);

                        if(quiet){
                            theArgs.add("-q");
                        }

                        theArgs.add("-d3");
                        if(comps != null){
                            for (String compName: comps)
                            {
                                theArgs.add("-C");
                                theArgs.add(compName);
                            }
                        }
                        if(groups != null){
                            for (String groupName: groups)
                            {
                                theArgs.add("-G");
                                theArgs.add(groupName);
                            }
                        }
                        if(tsNames != null){
                            for (String tsName: tsNames)
                            {
                                theArgs.add("-T");
                                theArgs.add(tsName);
                            }
                        }

                        decodes.tsdb.comprungui.CompExec.main(theArgs.toArray(new String[0]));
                    })
                )
            );
        assertTrue(exit.getExitCode() == null || exit.getExitCode()==0,
                   "System.exit called with unexpected code.");
    }

    private static ArrayList<File> gatherFiles(String[] extensions, String... filesOrDirectories)
    {
        ArrayList<File> files = new ArrayList<>();
        for (String fileOrDir: filesOrDirectories)
        {
            File file = new File(fileOrDir);
            if (file.isFile())
            {
                files.add(file);
            }
            else if (file.isDirectory())
            {
                files.addAll(FileUtils.listFiles(file,extensions,true));
            }
        }
        return files;
    }
}
