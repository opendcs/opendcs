package decodes.tsdb.compedit.algotab;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.table.AbstractTableModel;

import org.slf4j.LoggerFactory;

import decodes.tsdb.CompMetaData;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.xml.CompXio;
import decodes.tsdb.xml.DbXmlException;
import ilex.util.EnvExpander;

public final class ExecClassTableModel extends AbstractTableModel
{
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ExecClassTableModel.class);
    private String colNames[] = {"Name", "Exec Class", "Description"};

    private final ArrayList<DbCompAlgorithm> classlist = new ArrayList<>();

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
            case 0: return String.class;
            case 1: return String.class;
            case 2: return String.class;
            default: return Object.class;
        }
    }

    public int indexOf(String selection)
    {
        for(int i=0; i<classlist.size(); i++)
            if (selection.equals(classlist.get(i).getName()))
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
        DbCompAlgorithm algo = classlist.get(rowIndex);
        switch (columnIndex)
        {
            case 0: return algo.getName();
            case 1: return algo.getExecClass();
            case 2: return algo.getComment();
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
            return classlist.get(rowIndex);
        }
    }

    public void load() throws NoSuchObjectException
    {
        Path toolHome = Paths.get(EnvExpander.expand("$DCSTOOL_HOME"));
        Path userDir = Paths.get(EnvExpander.expand("$DCSTOOL_USERDIR"));
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.xml");
        final CompXio reader = new CompXio("algoreader", null);
        Function<Path,Stream<DbCompAlgorithm>> readAlgos = (Path path) ->
        {
            try
            {
                ArrayList<CompMetaData> data = reader.readFile(path.toFile().getAbsolutePath());
                return data.stream()
                        .filter(cmd -> cmd instanceof DbCompAlgorithm)
                        .map(cmd ->
                        {
                            return (DbCompAlgorithm)cmd;
                        });
            }
            catch (DbXmlException ex)
            {
                // We're looking at every XML, we only care about issues
                // with files we actually want.
                if (!ex.getMessage().contains("Root element is not 'CompMetaData'"))
                {
                    log.atWarn()
                    .setCause(ex)
                    .log("Unable to process file {}", path.toString());
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
            Stream.concat(Files.find(toolHome, 5, (path, attributes) -> matcher.matches(path)),
                          Files.find(userDir, 5, (path, attributes) -> matcher.matches(path)))
                    .flatMap(readAlgos)
                    .filter(districtByExec)
                    .peek(algo -> System.out.println(algo.getName()))
                    .collect(Collectors.toCollection(() -> this.classlist));

            try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("algorithms.xml"))
            {
                reader.readStream(in)
                      .stream()
                      .filter(cmd -> cmd instanceof DbCompAlgorithm)
                      .map(cmd ->
                      {
                        return (DbCompAlgorithm)cmd;
                      })
                      .filter(districtByExec)
                      .collect(Collectors.toCollection(() -> this.classlist));
            }
        }
        catch (DbXmlException | IOException ex)
        {
            log.atError()
               .setCause(ex)
               .log("Unable to process DCSTOOL_HOME or DCSTOOL_USER directory.");
        }

    }
}
