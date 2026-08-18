package org.opendcs.decodes.datasources;

import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;

import org.opendcs.decodes.api.DataMessage;
import org.opendcs.util.Result;

import decodes.datasource.DataSourceEndException;
import decodes.datasource.DataSourceException;
import decodes.datasource.DataSourceExec;

/**
 * Default data source spliterator. Advances from one message to the next mapping errors or end of stream
 * as appropriate.
 */
public class DataSourceSpliterator extends AbstractSpliterator<Result<DataMessage, DataSourceException>>
{

    private final DataSourceExec dataSource;


    public DataSourceSpliterator(DataSourceExec dataSource)
    {
        super(Long.MAX_VALUE, Spliterator.IMMUTABLE | Spliterator.NONNULL);
        this.dataSource = dataSource;
    }

    @Override
    public boolean tryAdvance(Consumer<? super Result<DataMessage, DataSourceException>> action)
    {
        boolean ret = true;
        Result<DataMessage, DataSourceException> msgRet;
        try
        {
            var msg = dataSource.getDataMessage();
            if (msg == null)
            {
                ret = false;
                msgRet = Result.failure(new DataSourceEndException("End of Data Source."));
            }
            else
            {
                msgRet = Result.success(msg);
            }
        }
        catch (DataSourceEndException ex)
        {
            ret = false;
            msgRet = Result.failure(ex);
        }
        catch (DataSourceException ex)
        {
            msgRet = Result.failure(ex);
        }

        action.accept(msgRet);
        return ret;
    }
}
