/*
 *  Copyright 2025 OpenDCS Consortium and its Contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
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

package org.opendcs.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import decodes.tsdb.CompMetaData;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.xml.CompXio;
import decodes.tsdb.xml.DbXmlException;
import ilex.util.EnvExpander;
import opendcs.dai.AlgorithmDAI;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;

/**
 * Scans $DCSTOOL_HOME, $DCSTOOL_USERDIR, and the classpath 'algorithms/' folder
 * for algorithm XML definitions. Used by both the Swing GUI ("Check for New" dialog)
 * and the REST API algorithm catalog endpoints.
 */
public final class AlgorithmCatalogScanner
{
    private static final org.slf4j.Logger log = OpenDcsLoggerFactory.getLogger();

    private AlgorithmCatalogScanner()
    {
    }

    /**
     * Scans all known locations for algorithm XML definitions and returns a
     * deduplicated list of {@link DbCompAlgorithm} objects (one per unique exec class).
     *
     * @return list of discovered algorithms
     */
    public static List<DbCompAlgorithm> scanAvailableAlgorithms()
    {
        Path toolHome = Paths.get(EnvExpander.expand("$DCSTOOL_HOME"));
        Path userDir = Paths.get(EnvExpander.expand("$DCSTOOL_USERDIR"));
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.xml");
        final CompXio reader = new CompXio("algoreader", null);

        Function<URL, Stream<DbCompAlgorithm>> readAlgos = url ->
        {
            if (url == null)
            {
                return Stream.empty();
            }
            try (InputStream stream = url.openStream())
            {
                ArrayList<CompMetaData> data = reader.readStream(stream);
                return data.stream()
                        .filter(cmd -> cmd instanceof DbCompAlgorithm)
                        .map(cmd -> (DbCompAlgorithm) cmd);
            }
            catch (DbXmlException | IOException ex)
            {
                // We're looking at every XML, we only care about issues
                // with files we actually want.
                if (!ex.getMessage().contains("Root element is not 'CompMetaData'"))
                {
                    log.atWarn()
                       .setCause(ex)
                       .log("Unable to process file {}", url.toString());
                }
                return Stream.empty();
            }
        };

        Set<String> seen = new HashSet<>();
        Predicate<DbCompAlgorithm> distinctByExec = algo -> seen.add(algo.getExecClass());

        List<DbCompAlgorithm> result = new ArrayList<>();

        try (Stream<Path> toolHomeStream = Files.find(toolHome, 5, (path, attrs) -> matcher.matches(path));
             Stream<Path> userDirStream = Files.find(userDir, 5, (path, attrs) -> matcher.matches(path)))
        {
            result.addAll(
                Stream.concat(toolHomeStream, userDirStream)
                .map(path ->
                {
                    try
                    {
                        return path.toUri().toURL();
                    }
                    catch (MalformedURLException ex)
                    {
                        return null;
                    }
                })
                .flatMap(readAlgos)
                .filter(distinctByExec)
                .collect(Collectors.toList())
            );
        }
        catch (IOException ex)
        {
            log.atError()
               .setCause(ex)
               .log("Unable to process DCSTOOL_HOME or DCSTOOL_USERDIR directory.");
        }

        try
        {
            result.addAll(
                ClasspathIO.getAllResourcesIn("algorithms", AlgorithmCatalogScanner.class.getClassLoader())
                    .stream()
                    .filter(u -> u.toExternalForm().endsWith(".xml"))
                    .flatMap(readAlgos)
                    .filter(distinctByExec)
                    .collect(Collectors.toList())
            );
        }
        catch (IOException ex)
        {
            log.atError()
               .setCause(ex)
               .log("Unable to scan classpath for algorithm definitions.");
        }

        return result;
    }

    /**
     * Returns the set of exec class names for algorithms already stored in the database.
     *
     * @param tsDb the time series database to query
     * @return set of exec class names present in the database
     * @throws DbIoException if the database query fails
     */
    public static Set<String> getImportedExecClasses(TimeSeriesDb tsDb) throws DbIoException
    {
        Set<String> importedExecClasses = new HashSet<>();
        try (AlgorithmDAI dai = tsDb.makeAlgorithmDAO())
        {
            for (DbCompAlgorithm a : dai.listAlgorithms())
            {
                importedExecClasses.add(a.getExecClass());
            }
        }
        return importedExecClasses;
    }
}
