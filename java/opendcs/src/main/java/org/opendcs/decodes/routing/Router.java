package org.opendcs.decodes.routing;

import org.opendcs.database.DatabaseKey;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDataRuntimeException;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.PresentationGroupDao;
import org.opendcs.database.dai.RoutingSpecDao;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.MDC;

import decodes.consumer.DataConsumer;
import decodes.consumer.DataConsumerException;
import decodes.consumer.OutputFormatter;
import decodes.consumer.OutputFormatterException;
import decodes.datasource.DataSourceException;
import decodes.datasource.DataSourceExec;
import decodes.db.InvalidDatabaseException;
import decodes.db.RoutingSpec;
import decodes.sql.DbKey;

public class Router implements Runnable
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    private final RoutingSpec spec;
    private final DataConsumer consumer;
    private final OutputFormatter formatter;
    private final OpenDcsDatabase db;
    private final DataSourceExec dataSource;

    private Router(RoutingSpec spec, DataConsumer consumer, OutputFormatter formatter, DataSourceExec dataSource,
                   OpenDcsDatabase db)
    {
        this.spec = spec;
        this.consumer = consumer;
        this.formatter = formatter;
        this.db = db;
        this.dataSource = dataSource;
    }

    @Override
    public void run()
    {
        try (var mdcSpecName = MDC.putCloseable("RoutingSpec", spec.getName()))
        {
            this.dataSource
                .stream()
                .map(m -> m.orElse(ex ->
                {
                    log.atWarn().setCause(ex).log("failed to retrieve message.");
                    return null;
                }))
                .filter(m -> m != null)
                .map(new MessageProcessor(formatter.attemptDecode()))
                .map(m -> m.orElse(ex ->
                {
                    log.atWarn().setCause(ex).log("failed to decode message.");
                    return null;
                }))
                .forEach(m ->
                {
                    try
                    {
                        formatter.formatMessage(m, consumer);
                    }
                    catch (DataConsumerException | OutputFormatterException ex)
                    {
                        log.atWarn().setCause(ex).log("Unable to output message.");
                    }

                });
        }
    }

    @SuppressWarnings("null") // incorrect null warning on Optional
    public static Router createRouter(OpenDcsDatabase db, DatabaseKey specId)
    {
        var specDao = db.getDao(RoutingSpecDao.class)
                        .orElseThrow(() -> new OpenDcsDataRuntimeException("No Routing Spec Dao available."));
        var presentationGroupDao = db.getDao(PresentationGroupDao.class)
                        .orElseThrow(() -> new OpenDcsDataRuntimeException("No Presentation group DAO available."));
        try (var tx = db.newTransaction())
        {
            var spec = specDao.getById(tx, (DbKey)specId)
                              .orElseThrow(() -> new OpenDcsDataRuntimeException("No routing spec with id " + specId));
            log.info("creating Route on thread {}/{}",
                     Thread.currentThread().getName(), Thread.currentThread().threadId());
            var consumer = DataConsumer.makeDataConsumer(spec.consumerType);
            consumer.open(spec.consumerArg, spec.getProperties());

            var presentationGroup = presentationGroupDao.getByName(tx, spec.presentationGroupName);
            var formatter = OutputFormatter.makeOutputFormatter(spec.outputFormat, spec.outputTimeZone,
                                                presentationGroup.orElse(null),
                                                spec.getProperties(), null);
            var dataSource = spec.dataSource.makeDelegate();
            dataSource.init(spec.getProperties(), spec.sinceTime, spec.untilTime, spec.networkLists);
            return new Router(spec, consumer, formatter, dataSource, db);
        }
        catch (OpenDcsDataException ex)
        {
            throw new OpenDcsDataRuntimeException(ex);
        }
        catch (DataConsumerException ex)
        {
            throw new OpenDcsDataRuntimeException("Unable to create consumer", ex);
        }
        catch (OutputFormatterException ex)
        {
            throw new OpenDcsDataRuntimeException("Unable to create formatter", ex);
        }
        catch (DataSourceException | InvalidDatabaseException ex)
        {
            throw new OpenDcsDataRuntimeException("Unable to initialized data source", ex);
        }
    }

}