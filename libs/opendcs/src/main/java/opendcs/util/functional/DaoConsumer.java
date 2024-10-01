package opendcs.util.functional;

import opendcs.dao.DaoBase;

/**
 * Helper to wrap a DAO for some sort of work.
 */
@FunctionalInterface
public interface DaoConsumer extends ThrowingConsumer<DaoBase, Exception>
{
    /**
     * To avoid too many nested exception handlers this declares a throwing of "Exception".
     * @throws Exception any exception
     */
    @Override
    public void accept(DaoBase dao) throws Exception;
}
