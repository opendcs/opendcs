package org.opendcs.algorithms.update;

import decodes.util.CmdLineArgs;
import ilex.cmdline.BooleanToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import org.opendcs.database.ManageDatabaseApp;
import org.opendcs.database.DatabaseService;
import org.opendcs.database.api.OpenDcsDataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import decodes.db.DatabaseException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

/** Standalone preview/apply command for sites that upgrade their schema separately. */
public final class AlgorithmUpdateApp
{
    private static final Logger log = LoggerFactory.getLogger(AlgorithmUpdateApp.class);

    private AlgorithmUpdateApp()
    {
    }

    public static void main(String[] args)
    {
        try
        {
            run(args);
        }
        catch (IOException | SQLException | DatabaseException | OpenDcsDataException ex)
        {
            log.error("Unable to update algorithms", ex);
            throw new IllegalStateException("Unable to update algorithms", ex);
        }
    }

    private static void run(String[] args) throws IOException, SQLException, DatabaseException, OpenDcsDataException
    {
        CmdLineArgs cli = new CmdLineArgs(true, "algoupdate.log");
        BooleanToken apply = new BooleanToken("apply", "Apply updates (default is preview only)",
                "", TokenOptions.optSwitch, false);
        StringToken username = new StringToken("username", "Database username", "",
                TokenOptions.optRequired, null);
        StringToken password = new StringToken("password", "Database password", "",
                TokenOptions.optRequired, null);
        StringToken algorithm = new StringToken("algorithm", "Limit update to an algorithm name", "",
                TokenOptions.optRequired, null);
        cli.addToken(apply);
        cli.addToken(username);
        cli.addToken(password);
        cli.addToken(algorithm);
        cli.parseArgs(args);
        var dataSource = ManageDatabaseApp.getDataSourceFromProfileAndUserInfo(
                cli.getProfile(), System.console(), username.getValue(), password.getValue());
        var database = DatabaseService.getDatabaseFor(dataSource);
        try (var transaction = database.newTransaction())
        {
            try
            {
                Set<Long> ids = Set.of();
                if (algorithm.getValue() != null)
                {
                    Connection connection = transaction.connection(Connection.class).orElseThrow(
                            () -> new OpenDcsDataException("A JDBC connection is required for algorithm updates"));
                    try (var statement = connection.prepareStatement(
                            "SELECT ALGORITHM_ID FROM CP_ALGORITHM WHERE LOWER(ALGORITHM_NAME) = LOWER(?)"))
                    {
                        statement.setString(1, algorithm.getValue());
                        try (var rows = statement.executeQuery())
                        {
                            if (!rows.next())
                            {
                                throw new IllegalArgumentException("Unknown algorithm: " + algorithm.getValue());
                            }
                            ids = Set.of(rows.getLong(1));
                        }
                    }
                }
                for (AlgorithmUpdater.Summary summary : AlgorithmUpdater.synchronize(transaction, !apply.getValue(), ids))
                {
                    if (summary.hasChanges())
                    {
                        log.info("Algorithm update: {}", summary);
                    }
                }
                if (apply.getValue()) transaction.commit();
                else transaction.rollback();
            }
            catch (SQLException | OpenDcsDataException | RuntimeException ex)
            {
                try
                {
                    transaction.rollback();
                }
                catch (OpenDcsDataException rollbackFailure)
                {
                    ex.addSuppressed(rollbackFailure);
                }
                throw ex;
            }
        }
    }
}
