package org.opendcs.database.impl.opendcs.dao.computation;

import static org.opendcs.utils.sql.SqlQueries.COLLATE_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.LEFT_OUTER;
import static org.opendcs.utils.sql.SqlQueries.WHERE_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.collateClauseFor;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.stringtemplate4.StringTemplateSqlLocator;
import org.opendcs.database.DatabaseKey;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.ComputationDao;
import org.opendcs.database.impl.opendcs.jdbi.column.text.Base64ColumnMapper;
import org.opendcs.database.impl.opendcs.jdbi.column.text.Base64String;
import org.opendcs.database.impl.opendcs.jdbi.mapper.computations.AlgorithmMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.computations.AlgorithmMappers;
import org.opendcs.database.impl.opendcs.jdbi.mapper.computations.AlgorithmScriptMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.computations.AlgorithmTsParameterMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.computations.ComputationMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.computations.ComputationMappers;
import org.opendcs.database.impl.opendcs.jdbi.mapper.computations.ComputationReducer;
import org.opendcs.database.impl.opendcs.jdbi.mapper.computations.ComputationTsParameterMapper;
import org.opendcs.database.model.mappers.properties.PropertiesMapper;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.openide.util.lookup.ServiceProvider;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import decodes.tsdb.DbComputation;

@ServiceProvider(service = ComputationDao.class)
public class ComputationDaoImpl implements ComputationDao
{
    private static final ComputationMappers allData =
        new ComputationMappers(
            ComputationMapper.withPrefix("c"),
            ComputationTsParameterMapper.withPrefix("csp"),
            PropertiesMapper.withPrefix("cprops", true),
            new AlgorithmMappers(AlgorithmMapper.withPrefix("a"),
                                 AlgorithmTsParameterMapper.withPrefix("atsp"),
                                 AlgorithmScriptMapper.withPrefix("ascript"),
                                 PropertiesMapper.withPrefix("ap", true)));
    private static final ComputationMappers refData = new ComputationMappers(
        allData.comp(), allData.compParms(), null, null
    );
    private static final Base64ColumnMapper base64Mapper = new Base64ColumnMapper();

    private static final String SELECT = "select";

    private final STGroup queries;

    public ComputationDaoImpl()
    {
        queries = StringTemplateSqlLocator.findStringTemplateGroup(ComputationDaoImpl.class);
    }

    @Override
    public Optional<DbComputation> getById(DataTransaction tx, DatabaseKey id) throws OpenDcsDataException
    {
        return getBy(tx, allData.comp().getPrefix() + ".computation_id = :id", "id", id);
    }

    @Override
    public Optional<DbComputation> getByName(DataTransaction tx, String name) throws OpenDcsDataException
    {
        return getBy(tx, "upper("+ allData.comp().getPrefix() + ".computation_name) = upper(:name)",
                     "name", name);
    }

    private Optional<DbComputation> getBy(DataTransaction tx, String whereClause,
                                          String whereBindKey, Object whereBindValue) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                            .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabaseEngine();

        var selectTemplate = getTemplate(SELECT);

        selectTemplate.add(WHERE_CLAUSE, whereClause)
                      .add("comp_columns", allData.comp().columnsForSelect())
                      .add("parm_columns", allData.compParms().columnsForSelect())
                      .add("prop_columns", allData.compProps().columnsForSelect())
                      .add("algo_columns", allData.algoMappers().algoMapper().columnsForSelect())
                      .add("algo_parm_columns", allData.algoMappers().parmMapper().columnsForSelect())
                      .add("script_columns", allData.algoMappers().scriptMapper().columnsForSelect())
                      .add("algo_prop_columns", allData.algoMappers().propertiesMapper().columnsForSelect())

                      .add("parm_join", allData.compParms()
                        .joinStatement(LEFT_OUTER, ComputationTsParameterMapper.Columns.COMPUTATION_ID,
                                       allData.comp().getPrefix(), ComputationMapper.Columns.ID.column()))
                      .add("prop_join",
                            String.format("left outer join cp_comp_property %s on %s.computation_id = %s.computation_id",
                                            allData.algoMappers().propertiesMapper().getPrefix(),
                                            allData.algoMappers().algoMapper().getPrefix(),
                                            allData.algoMappers().propertiesMapper().getPrefix()
                            )
                      )
                      .add("algo_parm_join", allData.algoMappers().parmMapper()
                                .joinStatement(LEFT_OUTER, AlgorithmTsParameterMapper.Columns.ALGORITHM_ID,
                                               allData.algoMappers().algoMapper().getPrefix(), AlgorithmMapper.Columns.ID.column()))
                      .add("script_join", allData.algoMappers().scriptMapper()
                                .joinStatement(LEFT_OUTER, AlgorithmScriptMapper.Columns.ALGORITHM_ID,
                                               allData.algoMappers().algoMapper().getPrefix(), AlgorithmMapper.Columns.ID.column()))
                      .add("algo_prop_join",
                            String.format("left outer join cp_algo_property %s on %s.algorithm_id = %s.algorithm_id",
                                            allData.algoMappers().propertiesMapper().getPrefix(),
                                            allData.algoMappers().algoMapper().getPrefix(),
                                            allData.algoMappers().propertiesMapper().getPrefix()
                            )
                      )
                      .add("algo_prefix", allData.algoMappers().algoMapper().getPrefix())
                      .add("algo_parm_prefix", allData.algoMappers().parmMapper().getPrefix())
                      .add("script_prefix", allData.algoMappers().scriptMapper().getPrefix())
                      .add("algo_prop_prefix", allData.algoMappers().propertiesMapper().getPrefix())
                      .add(COLLATE_CLAUSE, collateClauseFor(dbEngine));

        try (var select = handle.createQuery(selectTemplate.render()))
        {
            return select.registerColumnMapper(Base64String.class, base64Mapper)
                         .registerRowMapper(allData.algoMappers().algoMapper())
                         .registerRowMapper(allData.algoMappers().parmMapper())
                         .registerRowMapper(allData.algoMappers().scriptMapper())
                         .registerRowMapper(allData.algoMappers().propertiesMapper())
                         .bind(whereBindKey, whereBindValue)
                         .reduceRows(new ComputationReducer(allData))
                         .findFirst();
        }
    }

    @Override
    public DbComputation save(DataTransaction tx, DbComputation entry) throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'save'");
    }

    @Override
    public void delete(DataTransaction tx, DatabaseKey id) throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public List<DbComputation> getAll(DataTransaction tx, int limit, int offset, boolean expand)
            throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                            .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabaseEngine();

        var selectTemplate = getTemplate(SELECT);

        selectTemplate.add("comp_columns", allData.comp().columnsForSelect())
                      .add("parm_columns", allData.compParms().columnsForSelect())
                      .add("prop_columns", allData.compProps().columnsForSelect())
                      .add("algo_columns", allData.algoMappers().algoMapper().columnsForSelect())
                      .add("algo_parm_columns", allData.algoMappers().parmMapper().columnsForSelect())
                      .add("script_columns", allData.algoMappers().scriptMapper().columnsForSelect())
                      .add("algo_prop_columns", allData.algoMappers().propertiesMapper().columnsForSelect())

                      .add("parm_join", allData.compParms()
                        .joinStatement(LEFT_OUTER, ComputationTsParameterMapper.Columns.COMPUTATION_ID,
                                       allData.comp().getPrefix(), ComputationMapper.Columns.ID.column()))
                      .add("prop_join",
                            String.format("left outer join cp_comp_property %s on %s.computation_id = %s.computation_id",
                                            allData.algoMappers().propertiesMapper().getPrefix(),
                                            allData.algoMappers().algoMapper().getPrefix(),
                                            allData.algoMappers().propertiesMapper().getPrefix()
                            )
                      )
                      .add("algo_parm_join", allData.algoMappers().parmMapper()
                                .joinStatement(LEFT_OUTER, AlgorithmTsParameterMapper.Columns.ALGORITHM_ID,
                                               allData.algoMappers().algoMapper().getPrefix(), AlgorithmMapper.Columns.ID.column()))
                      .add("script_join", allData.algoMappers().scriptMapper()
                                .joinStatement(LEFT_OUTER, AlgorithmScriptMapper.Columns.ALGORITHM_ID,
                                               allData.algoMappers().algoMapper().getPrefix(), AlgorithmMapper.Columns.ID.column()))
                      .add("algo_prop_join",
                            String.format("left outer join cp_algo_property %s on %s.algorithm_id = %s.algorithm_id",
                                            allData.algoMappers().propertiesMapper().getPrefix(),
                                            allData.algoMappers().algoMapper().getPrefix(),
                                            allData.algoMappers().propertiesMapper().getPrefix()
                            )
                      )
                      .add("algo_prefix", allData.algoMappers().algoMapper().getPrefix())
                      .add("algo_parm_prefix", allData.algoMappers().parmMapper().getPrefix())
                      .add("script_prefix", allData.algoMappers().scriptMapper().getPrefix())
                      .add("algo_prop_prefix", allData.algoMappers().propertiesMapper().getPrefix())
                      .add(COLLATE_CLAUSE, collateClauseFor(dbEngine));

        try (var select = handle.createQuery(selectTemplate.render()))
        {
            return select.registerColumnMapper(Base64String.class, base64Mapper)
                         .registerRowMapper(allData.algoMappers().algoMapper())
                         .registerRowMapper(allData.algoMappers().parmMapper())
                         .registerRowMapper(allData.algoMappers().scriptMapper())
                         .registerRowMapper(allData.algoMappers().propertiesMapper())
                         .reduceRows(new ComputationReducer(allData))
                         .toList();
        }
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
