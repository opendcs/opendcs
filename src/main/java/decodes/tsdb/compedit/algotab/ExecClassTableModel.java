package decodes.tsdb.compedit.algotab;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.table.AbstractTableModel;

import org.opendcs.utils.ClasspathIO;
import org.slf4j.LoggerFactory;

import decodes.tsdb.CompMetaData;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.xml.CompXio;
import decodes.tsdb.xml.DbXmlException;
import ilex.util.EnvExpander;
import ilex.util.Pair;
import opendcs.dai.AlgorithmDAI;

public final class ExecClassTableModel extends AbstractTableModel
{
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ExecClassTableModel.class);
    private String colNames[] = {"Already Imported", "Name", "Exec Class", "Description"};

    private final ArrayList<Pair<Boolean,DbCompAlgorithm>> classlist = new ArrayList<>();
    private final TimeSeriesDb tsDb;


    public ExecClassTableModel(TimeSeriesDb tsDb)
    {
        this.tsDb = tsDb;
    }

    @Override
    public int getColumnCount()
    {
        int r = colNames.length;
        return r;
    }

    @Override
    public Class<?> getColumnClass(int index)
    {
        switch(index)
        {
            
            case 0: return Boolean.class;
            case 1: return String.class;
            case 2: return String.class;
            case 3: return String.class;
            default: return Object.class;
        }
    }

    public int indexOf(String selection)
    {
        for(int i=0; i<classlist.size(); i++)
            if (selection.equals(classlist.get(i).second.getName()))
                return i;
        return -1;
    }

    @Override
    public String getColumnName(int c)
    {
        return colNames[c];
    }

    @Override
    public int getRowCount()
    {
        return classlist.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        if (rowIndex < 0 || rowIndex >= classlist.size())
            return null;
        DbCompAlgorithm algo = classlist.get(rowIndex).second;
        switch (columnIndex)
        {
            case 0: return classlist.get(rowIndex).first;
            case 1: return algo.getName();
            case 2: return algo.getExecClass();
            case 3: return algo.getComment();
            default: return null;
        }
    }

    public DbCompAlgorithm getAlgoAt(int rowIndex)
    {
        if (rowIndex < 0 || rowIndex >= classlist.size())
        {
            return null;
        }
        else
        {
            return classlist.get(rowIndex).second;
        }
    }

    public Boolean getLoaded(int rowIndex)
    {
        if (rowIndex < 0 || rowIndex >= classlist.size())
        {
            return null;
        }
        else
        {
            return classlist.get(rowIndex).first;
        }
    }

    public void load() throws NoSuchObjectException
    {
        load(false);
    }

    public void load(boolean newOnly) throws NoSuchObjectException
    {
        Path toolHome = Paths.get(EnvExpander.expand("$DCSTOOL_HOME"));
        Path userDir = Paths.get(EnvExpander.expand("$DCSTOOL_USERDIR"));
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.xml");
        final CompXio reader = new CompXio("algoreader", null);
        Function<URL,Stream<DbCompAlgorithm>> readAlgos = (URL url) ->
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
                        .map(cmd ->
                        {
                            return (DbCompAlgorithm)cmd;
                        });
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
        try
        {
            Predicate<DbCompAlgorithm> districtByExec = new Predicate<DbCompAlgorithm>()
            {
                Set<String> execNames = new HashSet<>();

                @Override
                public boolean test(DbCompAlgorithm t)
                {
                    return execNames.add(t.getExecClass());
                }
                
            };
            
            final Function<DbCompAlgorithm,Boolean> presentInDb = new Function<DbCompAlgorithm,Boolean>()
            {
                Set<String> execNames = new HashSet<>();

                @Override
                public Boolean apply(DbCompAlgorithm algo)
                {
                    if (execNames.contains(algo.getExecClass()))
                    {
                        return true;
                    }
                    try(AlgorithmDAI dai = tsDb.makeAlgorithmDAO())
                    {
                        boolean found = false;
                        ArrayList<DbCompAlgorithm> algos = dai.listAlgorithms();
                        for (DbCompAlgorithm a: algos)
                        {
                            if (execNames.add(a.getExecClass()))
                            {
                                found = true;
                            }
                        }
                        return found;
                    }
                    catch (DbIoException ex)
                    {
                        log.atError()
                           .setCause(ex)
                           .log("Unable to search database for current algorithms.");
                    }

                    return false;
                }
            };
            Stream.concat(Files.find(toolHome, 5, (path, attributes) -> matcher.matches(path)),
                          Files.find(userDir, 5, (path, attributes) -> matcher.matches(path)))
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
                    .filter(districtByExec)
                    .peek(algo -> System.out.println(algo.getName()))
                    .map(a -> new Pair<>(presentInDb.apply(a),a))
                    .collect(Collectors.toCollection(() -> this.classlist));

            ClasspathIO.getAllResourcesIn("algorithms", this.getClass().getClassLoader())
                       .stream()
                       .filter(u -> u.toExternalForm().endsWith(".xml"))
                       .flatMap(readAlgos)
                       .filter(districtByExec)
                       .peek(algo -> System.out.println("From Jars:" + algo.getName()))
                       .map(a -> new Pair<>(presentInDb.apply(a),a))
                       .collect(Collectors.toCollection(() -> this.classlist));
            this.fireTableDataChanged();
        }
        catch (IOException ex)
        {
            log.atError()
               .setCause(ex)
               .log("Unable to process DCSTOOL_HOME or DCSTOOL_USER directory.");
        }

    }
}
