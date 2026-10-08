package org.opendcs.algorithms.update;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Synchronizes annotated Java algorithms without replacing comments, scripts, or property values. */
public final class AlgorithmUpdater
{
    private static final Logger log = LoggerFactory.getLogger(AlgorithmUpdater.class);
    private static final List<String> ROLE_PROPERTY_SUFFIXES = List.of("_MISSING", "_EU", "_tsname");

    private AlgorithmUpdater() {}

    public record Summary(String algorithm, int renamedParameters, int renamedProperties, int updatedTypes,
            int addedAlgorithmParameters, int removedAlgorithmParameters,
            int addedAlgorithmProperties, int needsReview) {}

    /** Each algorithm is updated in its own transaction. This operation is safe to repeat. */
    public static List<Summary> synchronize(DataSource dataSource, boolean dryRun) throws SQLException
    {
        return synchronize(dataSource, dryRun, Set.of());
    }

    /** An empty ID set synchronizes every installed annotated algorithm. */
    public static List<Summary> synchronize(DataSource dataSource, boolean dryRun, Set<Long> algorithmIds) throws SQLException
    {
        List<Summary> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection())
        {
            List<AlgorithmRow> algorithms = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT ALGORITHM_ID, ALGORITHM_NAME, EXEC_CLASS FROM CP_ALGORITHM");
                 ResultSet rows = statement.executeQuery())
            {
                while (rows.next())
                {
                    algorithms.add(new AlgorithmRow(rows.getLong(1), rows.getString(2), rows.getString(3)));
                }
            }
            for (AlgorithmRow algorithm : algorithms)
            {
                if (!algorithmIds.isEmpty() && !algorithmIds.contains(algorithm.id())) continue;
                if (algorithm.execClass() == null || algorithm.execClass().isBlank())
                {
                    continue;
                }
                AlgorithmSpec spec;
                try
                {
                    spec = AlgorithmSpec.read(algorithm.execClass());
                }
                catch (ClassNotFoundException | LinkageError | IllegalArgumentException ex)
                {
                    // Script algorithms and classes not installed on this host cannot be synchronized.
                    log.debug("Skipping algorithm '{}' ({}) during update: {}",
                            algorithm.name(), algorithm.execClass(), ex.toString());
                    continue;
                }
                boolean oldAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try
                {
                    result.add(synchronizeOne(connection, algorithm, spec, dryRun));
                    if (dryRun) connection.rollback();
                    else connection.commit();
                }
                catch (SQLException | RuntimeException ex)
                {
                    connection.rollback();
                    throw ex;
                }
                finally
                {
                    connection.setAutoCommit(oldAutoCommit);
                }
            }
        }
        return result;
    }

    private record AlgorithmRow(long id, String name, String execClass) {}

    private static Summary synchronizeOne(Connection connection, AlgorithmRow algorithm,
            AlgorithmSpec spec, boolean dryRun) throws SQLException
    {
        try (PreparedStatement lock = connection.prepareStatement(
                "SELECT ALGORITHM_ID FROM CP_ALGORITHM WHERE ALGORITHM_ID = ? FOR UPDATE"))
        {
            lock.setLong(1, algorithm.id());
            try (ResultSet ignored = lock.executeQuery())
            {
                if (!ignored.next()) throw new SQLException("Algorithm disappeared during update: " + algorithm.id());
            }
        }
        Map<String, String> oldTypes = readNames(connection,
                "SELECT ALGO_ROLE_NAME, PARM_TYPE FROM CP_ALGO_TS_PARM WHERE ALGORITHM_ID = ?", algorithm.id());
        Map<String, String> algoProps = readNames(connection,
                "SELECT PROP_NAME, PROP_VALUE FROM CP_ALGO_PROPERTY WHERE ALGORITHM_ID = ?", algorithm.id());
        int renamedParameters = 0, renamedProperties = 0, updatedTypes = 0;
        int addedRoles = 0, removedRoles = 0, addedProps = 0, review = 0;
        Set<Long> touched = new HashSet<>();
        for (AlgorithmSpec.Role role : spec.roles())
        {
            for (String former : role.formerNames())
            {
                String formerType = oldTypes.get(key(former));
                if (formerType != null && !role.type().equalsIgnoreCase(formerType)) continue;
                for (long computationId : computationIds(connection, algorithm.id()))
                {
                    Map<String, String> parms = readNames(connection,
                            "SELECT ALGO_ROLE_NAME, ALGO_ROLE_NAME FROM CP_COMP_TS_PARM WHERE COMPUTATION_ID = ?",
                            computationId);
                    if (!parms.containsKey(key(former))) continue;
                    if (parms.containsKey(key(role.name())) || count(connection,
                            "SELECT COUNT(*) FROM CP_COMP_TS_PARM WHERE COMPUTATION_ID = ? AND LOWER(ALGO_ROLE_NAME) = LOWER(?)",
                            computationId, former) != 1)
                    {
                        review++;
                        continue;
                    }
                    renamedParameters++;
                    touched.add(computationId);
                    if (!dryRun)
                    {
                        modify(connection, "UPDATE CP_COMP_TS_PARM SET ALGO_ROLE_NAME = ? WHERE COMPUTATION_ID = ? AND LOWER(ALGO_ROLE_NAME) = LOWER(?)",
                                role.name(), computationId, former);
                        for (String suffix : ROLE_PROPERTY_SUFFIXES)
                        {
                            renameProperty(connection, "CP_COMP_PROPERTY", "COMPUTATION_ID",
                                    computationId, former + suffix, role.name() + suffix);
                        }
                    }
                }
                if (oldTypes.containsKey(key(former)) && !oldTypes.containsKey(key(role.name())))
                {
                    if (!dryRun)
                    {
                        modify(connection, "UPDATE CP_ALGO_TS_PARM SET ALGO_ROLE_NAME = ? WHERE ALGORITHM_ID = ? AND LOWER(ALGO_ROLE_NAME) = LOWER(?)",
                                role.name(), algorithm.id(), former);
                    }
                    oldTypes.remove(key(former));
                    oldTypes.put(key(role.name()), role.type());
                }
                else if (oldTypes.containsKey(key(former)))
                {
                    // Existing computations with both names still wait for a user's choice.
                    removedRoles++;
                    if (!dryRun)
                        modify(connection, "DELETE FROM CP_ALGO_TS_PARM WHERE ALGORITHM_ID = ? AND LOWER(ALGO_ROLE_NAME) = LOWER(?)",
                                algorithm.id(), former);
                    oldTypes.remove(key(former));
                }
            }
            if (!oldTypes.containsKey(key(role.name())))
            {
                addedRoles++;
                if (!dryRun)
                    modify(connection, "INSERT INTO CP_ALGO_TS_PARM (ALGORITHM_ID, ALGO_ROLE_NAME, PARM_TYPE) VALUES (?, ?, ?)",
                            algorithm.id(), role.name(), role.type());
                oldTypes.put(key(role.name()), role.type());
            }
            else if (!role.type().equalsIgnoreCase(oldTypes.get(key(role.name()))))
            {
                updatedTypes++;
                if (!dryRun)
                    modify(connection, "UPDATE CP_ALGO_TS_PARM SET PARM_TYPE = ? WHERE ALGORITHM_ID = ? AND LOWER(ALGO_ROLE_NAME) = LOWER(?)",
                            role.type(), algorithm.id(), role.name());
                oldTypes.put(key(role.name()), role.type());
            }
        }
        for (AlgorithmSpec.Property property : spec.properties())
        {
            for (String former : property.formerNames())
            {
                if (algoProps.containsKey(key(former)) && !algoProps.containsKey(key(property.name())))
                {
                    renamedProperties++;
                    if (!dryRun)
                        renameProperty(connection, "CP_ALGO_PROPERTY", "ALGORITHM_ID", algorithm.id(), former, property.name());
                    algoProps.put(key(property.name()), algoProps.remove(key(former)));
                }
                for (long computationId : computationIds(connection, algorithm.id()))
                {
                    Map<String, String> props = readNames(connection,
                            "SELECT PROP_NAME, PROP_VALUE FROM CP_COMP_PROPERTY WHERE COMPUTATION_ID = ?", computationId);
                    if (props.containsKey(key(former)) && !props.containsKey(key(property.name())))
                    {
                        renamedProperties++;
                        touched.add(computationId);
                        if (!dryRun)
                            renameProperty(connection, "CP_COMP_PROPERTY", "COMPUTATION_ID", computationId, former, property.name());
                    }
                    else if (props.containsKey(key(former)) && props.containsKey(key(property.name()))) review++;
                }
            }
            if (!algoProps.containsKey(key(property.name())))
            {
                addedProps++;
                if (!dryRun)
                    modify(connection, "INSERT INTO CP_ALGO_PROPERTY (ALGORITHM_ID, PROP_NAME, PROP_VALUE) VALUES (?, ?, ?)",
                            algorithm.id(), property.name(), property.defaultValue());
                algoProps.put(key(property.name()), property.defaultValue());
            }
        }
        if (!dryRun && (renamedParameters + renamedProperties + updatedTypes + addedRoles + removedRoles + addedProps) > 0)
        {
            // Computation processes watch this timestamp to reload changed definitions.
            touched.addAll(computationIds(connection, algorithm.id()));
            int sqlType = loadedDateType(connection);
            for (long id : touched)
            {
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE CP_COMPUTATION SET DATE_TIME_LOADED = ? WHERE COMPUTATION_ID = ?"))
                {
                    if (sqlType == Types.BIGINT || sqlType == Types.INTEGER)
                        statement.setLong(1, System.currentTimeMillis());
                    else statement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                    statement.setLong(2, id);
                    statement.executeUpdate();
                }
            }
        }
        return new Summary(algorithm.name(), renamedParameters, renamedProperties,
                updatedTypes, addedRoles, removedRoles, addedProps, review);
    }

    private static int loadedDateType(Connection connection) throws SQLException
    {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT DATE_TIME_LOADED FROM CP_COMPUTATION WHERE 1 = 0"))
        {
            return statement.getMetaData().getColumnType(1);
        }
    }

    private static void renameProperty(Connection connection, String table, String idColumn,
            long id, String former, String current) throws SQLException
    {
        if (count(connection, "SELECT COUNT(*) FROM " + table + " WHERE " + idColumn + " = ? AND LOWER(PROP_NAME) = LOWER(?)",
                id, current) == 0)
            modify(connection, "UPDATE " + table + " SET PROP_NAME = ? WHERE " + idColumn + " = ? AND LOWER(PROP_NAME) = LOWER(?)",
                    current, id, former);
    }

    private static Map<String, String> readNames(Connection connection, String sql, long id) throws SQLException
    {
        Map<String, String> result = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setLong(1, id);
            try (ResultSet rows = statement.executeQuery())
            {
                while (rows.next()) result.put(key(rows.getString(1)), rows.getString(2));
            }
        }
        return result;
    }

    private static List<Long> computationIds(Connection connection, long algorithmId) throws SQLException
    {
        List<Long> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COMPUTATION_ID FROM CP_COMPUTATION WHERE ALGORITHM_ID = ?"))
        {
            statement.setLong(1, algorithmId);
            try (ResultSet rows = statement.executeQuery())
            {
                while (rows.next()) result.add(rows.getLong(1));
            }
        }
        return result;
    }

    private static int count(Connection connection, String sql, long id, String name) throws SQLException
    {
        try (PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setLong(1, id);
            statement.setString(2, name);
            try (ResultSet rows = statement.executeQuery())
            {
                rows.next();
                return rows.getInt(1);
            }
        }
    }

    private static void modify(Connection connection, String sql, Object... values) throws SQLException
    {
        try (PreparedStatement statement = connection.prepareStatement(sql))
        {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            statement.executeUpdate();
        }
    }

    private static String key(String name) { return name.toLowerCase(Locale.ROOT); }
}
