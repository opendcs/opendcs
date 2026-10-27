package org.opendcs.database.impl.opendcs.dao.computation;

import static org.opendcs.utils.sql.SqlQueries.COLLATE_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.LEFT_OUTER;
import static org.opendcs.utils.sql.SqlQueries.WHERE_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.collateClauseFor;
import static org.opendcs.utils.sql.SqlQueries.fromDual;

import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.stringtemplate4.StringTemplateSqlLocator;
import org.opendcs.database.DatabaseKey;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.AlgorithmDao;
import org.opendcs.database.impl.opendcs.jdbi.logging.DetailSqlLogger;
import org.opendcs.database.impl.opendcs.jdbi.mapper.computations.AlgorithmMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.computations.AlgorithmMappers;
import org.opendcs.database.impl.opendcs.jdbi.mapper.computations.AlgorithmReducer;
import org.opendcs.database.impl.opendcs.jdbi.mapper.computations.AlgorithmScriptMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.computations.AlgorithmTsParameterMapper;
import org.opendcs.database.model.mappers.properties.PropertiesMapper;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import decodes.db.DatabaseException;
import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;
import decodes.tsdb.DbCompAlgorithm;

@ServiceProvider(service = AlgorithmDao.class)
public class AlgorithmDaoImpl implements AlgorithmDao
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    private AlgorithmMappers allData;
    private AlgorithmMappers refData;
    private STGroup queries;

    private static final String SELECT = "select";
    private static final String MERGE = "merge";
    private static final String DELETE = "deleteAlgo";

    public AlgorithmDaoImpl()
    {
        allData = new AlgorithmMappers(AlgorithmMapper.withPrefix("a"),
                                       AlgorithmTsParameterMapper.withPrefix("astp"),
                                       AlgorithmScriptMapper.withPrefix("ascript"),
                                       PropertiesMapper.withPrefix("ap", true));
        refData = new AlgorithmMappers(AlgorithmMapper.withPrefix("a"),
                                       AlgorithmTsParameterMapper.withPrefix("astp"),
                                       null,
                                       null);
        queries = StringTemplateSqlLocator.findStringTemplateGroup(AlgorithmDaoImpl.class);
    }

    @Override
    public Optional<DbCompAlgorithm> getById(DataTransaction tx, DatabaseKey id) throws OpenDcsDataException
    {
        return getBy(tx, String.format("%s.algorithm_id = :id", allData.algoMapper().getPrefix()), "id", id);
    }

    @Override
    public Optional<DbCompAlgorithm> getByName(DataTransaction tx, String name) throws OpenDcsDataException
    {
        return getBy(tx, String.format("%s.algorithm_name = :name", allData.algoMapper().getPrefix()), "name", name);
    }

    private final Optional<DbCompAlgorithm> getBy(DataTransaction tx, String whereClause, String whereBindKey, Object whereBindValue) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                            .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabaseEngine();

        var selectTemplate = getTemplate(SELECT);        

        selectTemplate.add(WHERE_CLAUSE, whereClause)
                      .add("algo_columns", allData.algoMapper().columnsForSelect())
                      .add("parm_columns", allData.parmMapper().columnsForSelect())
                      .add("script_columns", allData.scriptMapper().columnsForSelect())
                      .add("prop_columns", allData.propertiesMapper().columnsForSelect())
                      .add("parm_join", allData.parmMapper()
                                .joinStatement(LEFT_OUTER, AlgorithmTsParameterMapper.Columns.ALGORITHM_ID,
                                               allData.algoMapper().getPrefix(), AlgorithmMapper.Columns.ID.column()))
                      .add("script_join", allData.scriptMapper()
                                .joinStatement(LEFT_OUTER, AlgorithmScriptMapper.Columns.ALGORITHM_ID,
                                               allData.algoMapper().getPrefix(), AlgorithmMapper.Columns.ID.column()))
                      .add("prop_join",
                            String.format("left outer join cp_algo_property %s on %s.algorithm_id = %s.algorithm_id",
                                            allData.propertiesMapper().getPrefix(),
                                            allData.algoMapper().getPrefix(),
                                            allData.propertiesMapper().getPrefix()
                            )
                      )
                      .add("algo_prefix", allData.algoMapper().getPrefix())
                      .add("parm_prefix", allData.parmMapper().getPrefix())
                      .add("script_prefix", allData.scriptMapper().getPrefix())
                      .add("prop_prefix", allData.propertiesMapper().getPrefix())
                      .add(COLLATE_CLAUSE, collateClauseFor(dbEngine));

        try (var select = handle.createQuery(selectTemplate.render()))
        {
            return select.registerRowMapper(allData.algoMapper())
                         .registerRowMapper(allData.parmMapper())
                         .registerRowMapper(allData.scriptMapper())
                         .registerRowMapper(allData.propertiesMapper())
                         .bind(whereBindKey, whereBindValue)
                         .reduceRows(new AlgorithmReducer(allData))
                         .findFirst();
        }
    }

    @Override
    public DbCompAlgorithm save(DataTransaction tx, DbCompAlgorithm algo) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                            .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabaseEngine();
        var keyGen = ctx.getGenerator(KeyGenerator.class)
                        .orElseThrow(() -> new OpenDcsDataException("No key generator configured."));
        var mergeTemplate = queries.getInstanceOf(MERGE);

        if (mergeTemplate == null)
        {
            throw new OpenDcsDataException("Could not find template");
        }
        mergeTemplate.add("dual", fromDual(dbEngine));
        try (var merge = handle.createUpdate(mergeTemplate.render());
             var insertParm = handle.prepareBatch(getTemplate("insertParm").render());
             var insertProp = handle.prepareBatch(getTemplate("insertProp").render()))
        {
            DbKey id = (DbKey)algo.getId();
            if (DbKey.isNull(id)) // if we have an id, we'll assume it's correct.
            {
                var existing = getByName(tx, algo.getName());
                if (existing.isPresent())
                {
                    // If there's an existing app with this name, we'll just assume the provided id, if any, was in error
                    id = (DbKey)existing.get().getId();
                    log.trace("""
                        Using ID from existing LrgsDataInterface, id={}, that was found. Provided ID was {}.
                        """,
                        id, algo.getId());
                }
            }
            final var comment = algo.getComment() != null 
                              ? algo.getComment()
                                    .substring(0,
                                               // The column currently only allows 1000 characters
                                               Math.min(1000, algo.getComment().length()))
                              : null;
            final var bindKey = DbKey.isNull(id) ? keyGen.getKey("cp_algorithm", handle.getConnection()) : id;
            merge.bind(AlgorithmMapper.Columns.ID.column(), bindKey)
                 .bind(AlgorithmMapper.Columns.ALGORITHM_NAME.column(), algo.getName())
                 .bind(AlgorithmMapper.Columns.EXEC_CLASS.column(), algo.getExecClass())
                 .bind(AlgorithmMapper.Columns.COMMENT.column(), comment)
                 .execute();
            deleteParms(handle, bindKey);
            deleteScripts(handle, bindKey);
            deleteProps(handle, bindKey);
            
            algo.getParms()
                .forEachRemaining(p ->
                {
                    insertParm.bind(AlgorithmTsParameterMapper.Columns.ALGORITHM_ID.column(), bindKey)
                              .bind(AlgorithmTsParameterMapper.Columns.ALGO_ROLE_NAME.column(), p.getRoleName())
                              .bind(AlgorithmTsParameterMapper.Columns.PARM_TYPE.column(), p.getParmType())
                              .add();
                });
            insertParm.execute();

            algo.getProperties()
                .forEach((k,v) ->
                {
                    insertProp.bind(AlgorithmMapper.Columns.ID.column(), bindKey)
                              .bind(PropertiesMapper.Columns.NAME.column(), k)
                              .bind(PropertiesMapper.Columns.VALUE.column(), v)
                              .add();
                }); 
            insertProp.execute();

            // other bits
            return getById(tx, bindKey)
                .orElseThrow(() -> new OpenDcsDataException("Could not retrieve algorithm we just saved."));
        }
        catch (DatabaseException ex)
        {
            throw new OpenDcsDataException("Unable to generator new key for algorithm", ex);
        }
    }

    private void deleteParms(Handle handle, DatabaseKey id) throws OpenDcsDataException
    {
        doDelete(handle, "deleteParms", id);
        
    }

    private void deleteScripts(Handle handle, DatabaseKey id) throws OpenDcsDataException
    {
        doDelete(handle,"deleteScripts", id);
    }

    private void deleteProps(Handle handle, DatabaseKey id) throws OpenDcsDataException
    {
        doDelete(handle, "deleteProps", id);
    }

    private void doDelete(Handle handle, String templateName, DatabaseKey id) throws OpenDcsDataException
    {
        var template = getTemplate(templateName).render();
        try (var delete = handle.createUpdate(template))
        {
            delete.setSqlLogger(new DetailSqlLogger(log));
            delete.bind("id", id).execute();
        }
    }

    @Override
    public void delete(DataTransaction tx, DatabaseKey id) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                            .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));

        deleteProps(handle, id);
        deleteParms(handle, id);
        deleteScripts(handle, id);
        doDelete(handle, DELETE, id);
    }

    @Override
    public List<DbCompAlgorithm> getAll(DataTransaction tx, int limit, int offset, boolean expand)
            throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAll'");
    }
    

    private ST getTemplate(String templateName) throws OpenDcsDataException
    {
        var template = queries.getInstanceOf(templateName);
        if (template == null)
        {
            throw new OpenDcsDataException("Could not find template");
        }
        return template;
    }
}
