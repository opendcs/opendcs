package org.opendcs.database.impl.opendcs.jdbi.arguments.decodes;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.NamedArgumentFinder;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.platforms.TransportMediumMapper;
import org.opendcs.database.sql.TableColumnDefinition;

import decodes.db.TransportMedium;
import decodes.sql.DbKey;

import static org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.platforms.TransportMediumMapper.Columns.*;

/**
 * Handles mapping of requested column names to the stored TransportMedium instance.
 * TransportMediumArgumentFinder
 */
public class TransportMediumArgumentFinder implements NamedArgumentFinder
{
    public static final List<String> COLUMNS = Arrays.asList(TransportMediumMapper.Columns.values())
                                                     .stream()
                                                     .map(e -> e.column())
                                                     .toList();

    private final TransportMedium tm;

    public TransportMediumArgumentFinder(TransportMedium tm)
    {
        this.tm = tm;
    }


    @SuppressWarnings("java:S1142") // otherwise it just looks odd
    @Override
    public Optional<Argument> find(String name, StatementContext ctx)
    {
        if (tm == null)
        {
            return Optional.empty();
        }

        final var column = TableColumnDefinition
                                .fromString(TransportMediumMapper.Columns.class, name).orElse(null);
        if (column == null)
        {
            return Optional.empty();
        }
        switch (column)
        {
            case PLATFORM_ID:
                return ctx.findArgumentFor(DbKey.class, tm.platform.getId());
            case ASSIGNED_TIME:
                return ctx.findArgumentFor(int.class, tm.assignedTime);
            case BAUD:
                return ctx.findArgumentFor(int.class, tm.getBaud());
            case CHANNEL_NUMBER:
                return ctx.findArgumentFor(int.class, tm.channelNum);
            case DATABITS:
                return ctx.findArgumentFor(int.class, tm.getDataBits());
            case DO_LOGIN:
                return ctx.findArgumentFor(boolean.class, tm.isDoLogin());
            case EQUIPMENT_ID:
                return ctx.findArgumentFor(DbKey.class,
                                           tm.equipmentModel != null ? tm.equipmentModel.getId() : null);
            case LOGGER_TYPE:
                return ctx.findArgumentFor(String.class, tm.getLoggerType());
            case MEDIUM_ID:
                return ctx.findArgumentFor(String.class, tm.getMediumId());
            case MEDIUM_TYPE:
                return ctx.findArgumentFor(String.class, tm.getMediumType());
            case PARITY:
                return ctx.findArgumentFor(char.class, tm.getParity());
            case PASSWORD:
                return ctx.findArgumentFor(String.class, tm.getPassword());
<<<<<<< HEAD
            case PREAMBLE:
=======
            case PREAMPLE:
>>>>>>> 2dd56139 (Additional platform data attempting to store.)
                return ctx.findArgumentFor(char.class, tm.getPreamble());
            case SCRIPT_NAME:
                return ctx.findArgumentFor(String.class, tm.scriptName);
            case STOP_BITS:
                return ctx.findArgumentFor(int.class, tm.getStopBits());
            case TIME_ADJUSTMENT:
                return ctx.findArgumentFor(int.class, tm.getTimeAdjustment());
            case TIME_ZONE:
                return ctx.findArgumentFor(String.class, tm.getTimeZone());
            case TRANSMIT_INTERVAL:
                return ctx.findArgumentFor(int.class, tm.transmitInterval);
            case TRANSMIT_WINDOW:
                return ctx.findArgumentFor(int.class, tm.transmitWindow);
            case USERNAME:
                return ctx.findArgumentFor(String.class, tm.getUsername());
            default:
                return Optional.empty();
        }
    }

    @Override
    public Collection<String> getNames()
    {
        return COLUMNS;
    }
}
