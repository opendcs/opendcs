package org.opendcs.algorithms.update;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Synchronizes annotated Java algorithms without replacing comments, scripts, or property values. */
public final class AlgorithmUpdater
{
    private static final Logger log = LoggerFactory.getLogger(AlgorithmUpdater.class);
    private static final List<String> ROLE_PROPERTY_SUFFIXES = List.of("_MISSING", "_EU", "_tsname");

    private AlgorithmUpdater()
    {
    }

    public record Summary(String algorithm, int renamedParameters, int renamedProperties, int updatedTypes,
            int addedAlgorithmParameters, int addedComputationParameters, int removedAlgorithmParameters,
            int addedAlgorithmProperties, int needsReview)
    {
        public boolean hasChanges()
        {
            return hasAppliedChanges() || needsReview > 0;
        }

        public boolean hasAppliedChanges()
        {
            return renamedParameters + renamedProperties + updatedTypes + addedAlgorithmParameters
                    + addedComputationParameters
                    + removedAlgorithmParameters + addedAlgorithmProperties > 0;
        }
    }

    /** Uses only the supplied transaction and never commits or closes it. */
    public static List<Summary> synchronize(DataTransaction transaction, boolean dryRun)
            throws SQLException, OpenDcsDataException
    {
        return synchronize(transaction, dryRun, Set.of());
    }

    /** An empty ID set synchronizes every installed annotated algorithm. */
    public static List<Summary> synchronize(DataTransaction transaction, boolean dryRun, Set<Long> algorithmIds)
            throws SQLException, OpenDcsDataException
    {
        Connection connection = transaction.connection(Connection.class)
                .orElseThrow(() -> new OpenDcsDataException("A JDBC connection is required for algorithm updates"));
        List<Summary> result = new ArrayList<>();
        for (AlgorithmRow algorithm : readAlgorithms(connection))
        {
            if ((algorithmIds.isEmpty() || algorithmIds.contains(algorithm.id()))
                    && algorithm.execClass() != null && !algorithm.execClass().isBlank())
            {
                AlgorithmSpec spec = installedSpec(algorithm);
                if (spec != null)
                {
                    result.add(synchronizeOne(connection, algorithm, spec, dryRun));
                }
            }
        }
        return result;
    }

    private static AlgorithmSpec installedSpec(AlgorithmRow algorithm)
    {
        try
        {
            return AlgorithmSpec.read(algorithm.execClass());
        }
        catch (ClassNotFoundException | LinkageError | IllegalArgumentException ex)
        {
            // Script algorithms and classes not installed on this host cannot be synchronized.
            log.debug("Skipping algorithm '{}' ({}) during update", algorithm.name(), algorithm.execClass(), ex);
            return null;
        }
    }

    private static List<AlgorithmRow> readAlgorithms(Connection connection) throws SQLException
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
        return algorithms;
    }

    private record AlgorithmRow(long id, String name, String execClass)
    {
    }

    private static final class UpdateState
    {
        private final Map<String, String> oldTypes;
        private final Map<String, String> algorithmProperties;
        private int renamedParameters;
        private int renamedProperties;
        private int updatedTypes;
        private int addedRoles;
        private int addedComputationParameters;
        private int removedRoles;
        private int addedProperties;
        private int needsReview;

        private UpdateState(Map<String, String> oldTypes, Map<String, String> algorithmProperties)
        {
            this.oldTypes = oldTypes;
            this.algorithmProperties = algorithmProperties;
        }

        private Summary summary(String algorithmName)
        {
            return new Summary(algorithmName, renamedParameters, renamedProperties, updatedTypes,
                    addedRoles, addedComputationParameters, removedRoles, addedProperties, needsReview);
        }
    }

    private static Summary synchronizeOne(Connection connection, AlgorithmRow algorithm,
            AlgorithmSpec spec, boolean dryRun) throws SQLException
    {
        try (PreparedStatement lock = connection.prepareStatement(
                "SELECT ALGORITHM_ID FROM CP_ALGORITHM WHERE ALGORITHM_ID = ? FOR UPDATE"))
        {
            lock.setLong(1, algorithm.id());
            try (ResultSet ignored = lock.executeQuery())
            {
                if (!ignored.next())
                {
                    throw new SQLException("Algorithm disappeared during update: " + algorithm.id());
                }
            }
        }
        UpdateState state = new UpdateState(
                readNames(connection, "SELECT ALGO_ROLE_NAME, PARM_TYPE FROM CP_ALGO_TS_PARM WHERE ALGORITHM_ID = ?", algorithm.id()),
                readNames(connection, "SELECT PROP_NAME, PROP_VALUE FROM CP_ALGO_PROPERTY WHERE ALGORITHM_ID = ?", algorithm.id()));
        List<Long> computationIds = computationIds(connection, algorithm.id());
        for (AlgorithmSpec.Role role : spec.roles())
        {
            synchronizeRole(connection, algorithm.id(), computationIds, role, state, dryRun);
        }
        for (AlgorithmSpec.Property property : spec.properties())
        {
            synchronizeProperty(connection, algorithm.id(), computationIds, property, state, dryRun);
        }
        if (!dryRun && state.summary(algorithm.name()).hasAppliedChanges())
        {
            touchComputations(connection, computationIds);
        }
        return state.summary(algorithm.name());
    }

    private static void synchronizeRole(Connection connection, long algorithmId, List<Long> computationIds,
            AlgorithmSpec.Role role, UpdateState state, boolean dryRun) throws SQLException
    {
        for (String former : role.formerNames())
        {
            String formerType = state.oldTypes.get(key(former));
            if (formerType == null || role.type().equalsIgnoreCase(formerType))
            {
                for (long computationId : computationIds)
                {
                    renameComputationRole(connection, computationId, former, role.name(), state, dryRun);
                }
                renameAlgorithmRole(connection, algorithmId, former, role, state, dryRun);
            }
        }
        ensureAlgorithmRole(connection, algorithmId, role, state, dryRun);
        for (long computationId : computationIds)
        {
            ensureComputationRole(connection, computationId, role, state, dryRun);
        }
    }

    private static void ensureComputationRole(Connection connection, long computationId,
            AlgorithmSpec.Role role, UpdateState state, boolean dryRun) throws SQLException
    {
        Map<String, String> parms = readNames(connection,
                "SELECT ALGO_ROLE_NAME, ALGO_ROLE_NAME FROM CP_COMP_TS_PARM WHERE COMPUTATION_ID = ?", computationId);
        if (parms.containsKey(key(role.name())))
        {
            return;
        }
        // An old role that could not be renamed needs a user's choice, not a competing blank row.
        if (role.formerNames().stream().anyMatch(former -> parms.containsKey(key(former))))
        {
            return;
        }
        state.addedComputationParameters++;
        if (!dryRun)
        {
            modify(connection,
                    "INSERT INTO CP_COMP_TS_PARM (COMPUTATION_ID, ALGO_ROLE_NAME) VALUES (?, ?)",
                    computationId, role.name());
        }
    }

    private static void renameComputationRole(Connection connection, long computationId, String former,
            String current, UpdateState state, boolean dryRun) throws SQLException
    {
        Map<String, String> parms = readNames(connection,
                "SELECT ALGO_ROLE_NAME, ALGO_ROLE_NAME FROM CP_COMP_TS_PARM WHERE COMPUTATION_ID = ?", computationId);
        if (!parms.containsKey(key(former)))
        {
            return;
        }
        if (parms.containsKey(key(current)) || count(connection,
                "SELECT COUNT(*) FROM CP_COMP_TS_PARM WHERE COMPUTATION_ID = ? AND LOWER(ALGO_ROLE_NAME) = LOWER(?)",
                computationId, former) != 1)
        {
            state.needsReview++;
            return;
        }
        state.renamedParameters++;
        if (!dryRun)
        {
            modify(connection, "UPDATE CP_COMP_TS_PARM SET ALGO_ROLE_NAME = ? WHERE COMPUTATION_ID = ? AND LOWER(ALGO_ROLE_NAME) = LOWER(?)",
                    current, computationId, former);
            for (String suffix : ROLE_PROPERTY_SUFFIXES)
            {
                renameProperty(connection, "CP_COMP_PROPERTY", "COMPUTATION_ID", computationId,
                        former + suffix, current + suffix);
            }
        }
    }

    private static void renameAlgorithmRole(Connection connection, long algorithmId, String former,
            AlgorithmSpec.Role role, UpdateState state, boolean dryRun) throws SQLException
    {
        if (!state.oldTypes.containsKey(key(former)))
        {
            return;
        }
        if (!state.oldTypes.containsKey(key(role.name())))
        {
            if (!dryRun)
            {
                modify(connection, "UPDATE CP_ALGO_TS_PARM SET ALGO_ROLE_NAME = ? WHERE ALGORITHM_ID = ? AND LOWER(ALGO_ROLE_NAME) = LOWER(?)",
                        role.name(), algorithmId, former);
            }
            state.oldTypes.put(key(role.name()), role.type());
        }
        else
        {
            // Existing computations with both names still wait for a user's choice.
            state.removedRoles++;
            if (!dryRun)
            {
                modify(connection, "DELETE FROM CP_ALGO_TS_PARM WHERE ALGORITHM_ID = ? AND LOWER(ALGO_ROLE_NAME) = LOWER(?)",
                        algorithmId, former);
            }
        }
        state.oldTypes.remove(key(former));
    }

    private static void ensureAlgorithmRole(Connection connection, long algorithmId, AlgorithmSpec.Role role,
            UpdateState state, boolean dryRun) throws SQLException
    {
        String storedType = state.oldTypes.get(key(role.name()));
        if (storedType == null)
        {
            state.addedRoles++;
            if (!dryRun)
            {
                modify(connection, "INSERT INTO CP_ALGO_TS_PARM (ALGORITHM_ID, ALGO_ROLE_NAME, PARM_TYPE) VALUES (?, ?, ?)",
                        algorithmId, role.name(), role.type());
            }
            state.oldTypes.put(key(role.name()), role.type());
        }
        else if (!role.type().equalsIgnoreCase(storedType))
        {
            state.updatedTypes++;
            if (!dryRun)
            {
                modify(connection, "UPDATE CP_ALGO_TS_PARM SET PARM_TYPE = ? WHERE ALGORITHM_ID = ? AND LOWER(ALGO_ROLE_NAME) = LOWER(?)",
                        role.type(), algorithmId, role.name());
            }
            state.oldTypes.put(key(role.name()), role.type());
        }
    }

    private static void synchronizeProperty(Connection connection, long algorithmId, List<Long> computationIds,
            AlgorithmSpec.Property property, UpdateState state, boolean dryRun) throws SQLException
    {
        for (String former : property.formerNames())
        {
            if (state.algorithmProperties.containsKey(key(former))
                    && !state.algorithmProperties.containsKey(key(property.name())))
            {
                state.renamedProperties++;
                if (!dryRun)
                {
                    renameProperty(connection, "CP_ALGO_PROPERTY", "ALGORITHM_ID", algorithmId, former, property.name());
                }
                state.algorithmProperties.put(key(property.name()), state.algorithmProperties.remove(key(former)));
            }
            for (long computationId : computationIds)
            {
                renameComputationProperty(connection, computationId, former, property.name(), state, dryRun);
            }
        }
        if (!state.algorithmProperties.containsKey(key(property.name())))
        {
            state.addedProperties++;
            if (!dryRun)
            {
                modify(connection, "INSERT INTO CP_ALGO_PROPERTY (ALGORITHM_ID, PROP_NAME, PROP_VALUE) VALUES (?, ?, ?)",
                        algorithmId, property.name(), property.defaultValue());
            }
            state.algorithmProperties.put(key(property.name()), property.defaultValue());
        }
    }

    private static void renameComputationProperty(Connection connection, long computationId, String former,
            String current, UpdateState state, boolean dryRun) throws SQLException
    {
        Map<String, String> props = readNames(connection,
                "SELECT PROP_NAME, PROP_VALUE FROM CP_COMP_PROPERTY WHERE COMPUTATION_ID = ?", computationId);
        if (props.containsKey(key(former)) && !props.containsKey(key(current)))
        {
            state.renamedProperties++;
            if (!dryRun)
            {
                renameProperty(connection, "CP_COMP_PROPERTY", "COMPUTATION_ID", computationId, former, current);
            }
        }
        else if (props.containsKey(key(former)) && props.containsKey(key(current)))
        {
            state.needsReview++;
        }
    }

    private static void touchComputations(Connection connection, List<Long> computationIds) throws SQLException
    {
        // Computation processes watch this timestamp to reload changed definitions.
        int sqlType = loadedDateType(connection);
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE CP_COMPUTATION SET DATE_TIME_LOADED = ? WHERE COMPUTATION_ID = ?"))
        {
            for (long id : computationIds)
            {
                if (sqlType == Types.BIGINT || sqlType == Types.INTEGER)
                {
                    statement.setLong(1, System.currentTimeMillis());
                }
                else
                {
                    statement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                }
                statement.setLong(2, id);
                statement.executeUpdate();
            }
        }
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
        {
            modify(connection, "UPDATE " + table + " SET PROP_NAME = ? WHERE " + idColumn + " = ? AND LOWER(PROP_NAME) = LOWER(?)",
                    current, id, former);
        }
    }

    private static Map<String, String> readNames(Connection connection, String sql, long id) throws SQLException
    {
        Map<String, String> result = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setLong(1, id);
            try (ResultSet rows = statement.executeQuery())
            {
                while (rows.next())
                {
                    result.put(key(rows.getString(1)), rows.getString(2));
                }
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
                while (rows.next())
                {
                    result.add(rows.getLong(1));
                }
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
            for (int i = 0; i < values.length; i++)
            {
                statement.setObject(i + 1, values[i]);
            }
            statement.executeUpdate();
        }
    }

    private static String key(String name)
    {
        return name.toLowerCase(Locale.ROOT);
    }
}
