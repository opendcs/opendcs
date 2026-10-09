package org.opendcs.algorithms.update;

import decodes.util.CmdLineArgs;
import ilex.cmdline.BooleanToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import org.opendcs.database.ManageDatabaseApp;
import java.util.Set;

/** Standalone preview/apply command for sites that upgrade their schema separately. */
public final class AlgorithmUpdateApp
{
    private AlgorithmUpdateApp() {}

    public static void main(String[] args) throws Exception
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
        Set<Long> ids = Set.of();
        if (algorithm.getValue() != null)
        {
            try (var connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(
                         "SELECT ALGORITHM_ID FROM CP_ALGORITHM WHERE LOWER(ALGORITHM_NAME) = LOWER(?)"))
            {
                statement.setString(1, algorithm.getValue());
                try (var rows = statement.executeQuery())
                {
                    if (!rows.next()) throw new IllegalArgumentException("Unknown algorithm: " + algorithm.getValue());
                    ids = Set.of(rows.getLong(1));
                }
            }
        }
        for (AlgorithmUpdater.Summary summary : AlgorithmUpdater.synchronize(dataSource, !apply.getValue(), ids))
        {
            if (summary.renamedParameters() + summary.renamedProperties() + summary.updatedTypes()
                    + summary.addedAlgorithmParameters() + summary.addedAlgorithmProperties()
                    + summary.removedAlgorithmParameters()
                    + summary.needsReview() > 0)
                System.out.println(summary);
        }
    }
}
