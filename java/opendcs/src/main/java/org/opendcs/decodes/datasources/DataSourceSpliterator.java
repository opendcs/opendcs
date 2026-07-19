package org.opendcs.decodes.datasources;

import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;

import org.opendcs.decodes.api.DataMessage;
import org.opendcs.utils.FailableResult;

import decodes.datasource.DataSourceEndException;
import decodes.datasource.DataSourceException;
import decodes.datasource.DataSourceExec;

/**
 * Default data source spliterator. Advances from one message to the next mapping errors or end of stream
 * as appropriate.
 */
public class DataSourceSpliterator extends AbstractSpliterator<FailableResult<DataMessage, DataSourceException>>
{

    private final DataSourceExec dataSource;


    public DataSourceSpliterator(DataSourceExec dataSource)
    {
        super(Long.MAX_VALUE, Spliterator.IMMUTABLE | Spliterator.NONNULL);
        this.dataSource = dataSource;
    }

    @Override
    public boolean tryAdvance(Consumer<? super FailableResult<DataMessage, DataSourceException>> action)
    {
        boolean ret = true;
        FailableResult<DataMessage, DataSourceException> msgRet;
        try
        {
            var msg = dataSource.getDataMessage();
            if (msg == null)
            {
                ret = false;
                msgRet = FailableResult.failure(new DataSourceEndException("End of Data Source."));
            }
            else
            {
                msgRet = FailableResult.success(msg);
            }
        }
        catch (DataSourceEndException ex)
        {
            ret = false;
            msgRet = FailableResult.failure(ex);
        }
        catch (DataSourceException ex)
        {
            msgRet = FailableResult.failure(ex);
        }

        action.accept(msgRet);
        return ret;
    }
}
