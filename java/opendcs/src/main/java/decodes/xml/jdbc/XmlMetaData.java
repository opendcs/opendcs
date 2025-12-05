package decodes.xml.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;

/**
 * Implement methods as required, but note that we are planning to drop the XML Database concepts
 * except for export/import.
 */
public class XmlMetaData implements DatabaseMetaData
{

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'unwrap'");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'isWrapperFor'");
    }

    @Override
    public boolean allProceduresAreCallable() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'allProceduresAreCallable'");
    }

    @Override
    public boolean allTablesAreSelectable() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'allTablesAreSelectable'");
    }

    @Override
    public String getURL() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getURL'");
    }

    @Override
    public String getUserName() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getUserName'");
    }

    @Override
    public boolean isReadOnly() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'isReadOnly'");
    }

    @Override
    public boolean nullsAreSortedHigh() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'nullsAreSortedHigh'");
    }

    @Override
    public boolean nullsAreSortedLow() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'nullsAreSortedLow'");
    }

    @Override
    public boolean nullsAreSortedAtStart() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'nullsAreSortedAtStart'");
    }

    @Override
    public boolean nullsAreSortedAtEnd() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'nullsAreSortedAtEnd'");
    }

    @Override
    public String getDatabaseProductName() throws SQLException

    {
        return "OpenDCS-XML";
    }

    @Override
    public String getDatabaseProductVersion() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getDatabaseProductVersion'");
    }

    @Override
    public String getDriverName() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getDriverName'");
    }

    @Override
    public String getDriverVersion() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getDriverVersion'");
    }

    @Override
    public int getDriverMajorVersion()
    {
        throw new UnsupportedOperationException("Unimplemented method 'getDriverMajorVersion'");
    }

    @Override
    public int getDriverMinorVersion()
    {
        throw new UnsupportedOperationException("Unimplemented method 'getDriverMinorVersion'");
    }

    @Override
    public boolean usesLocalFiles() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'usesLocalFiles'");
    }

    @Override
    public boolean usesLocalFilePerTable() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'usesLocalFilePerTable'");
    }

    @Override
    public boolean supportsMixedCaseIdentifiers() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsMixedCaseIdentifiers'");
    }

    @Override
    public boolean storesUpperCaseIdentifiers() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'storesUpperCaseIdentifiers'");
    }

    @Override
    public boolean storesLowerCaseIdentifiers() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'storesLowerCaseIdentifiers'");
    }

    @Override
    public boolean storesMixedCaseIdentifiers() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'storesMixedCaseIdentifiers'");
    }

    @Override
    public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsMixedCaseQuotedIdentifiers'");
    }

    @Override
    public boolean storesUpperCaseQuotedIdentifiers() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'storesUpperCaseQuotedIdentifiers'");
    }

    @Override
    public boolean storesLowerCaseQuotedIdentifiers() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'storesLowerCaseQuotedIdentifiers'");
    }

    @Override
    public boolean storesMixedCaseQuotedIdentifiers() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'storesMixedCaseQuotedIdentifiers'");
    }

    @Override
    public String getIdentifierQuoteString() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getIdentifierQuoteString'");
    }

    @Override
    public String getSQLKeywords() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getSQLKeywords'");
    }

    @Override
    public String getNumericFunctions() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getNumericFunctions'");
    }

    @Override
    public String getStringFunctions() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getStringFunctions'");
    }

    @Override
    public String getSystemFunctions() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getSystemFunctions'");
    }

    @Override
    public String getTimeDateFunctions() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getTimeDateFunctions'");
    }

    @Override
    public String getSearchStringEscape() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getSearchStringEscape'");
    }

    @Override
    public String getExtraNameCharacters() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getExtraNameCharacters'");
    }

    @Override
    public boolean supportsAlterTableWithAddColumn() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsAlterTableWithAddColumn'");
    }

    @Override
    public boolean supportsAlterTableWithDropColumn() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsAlterTableWithDropColumn'");
    }

    @Override
    public boolean supportsColumnAliasing() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsColumnAliasing'");
    }

    @Override
    public boolean nullPlusNonNullIsNull() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'nullPlusNonNullIsNull'");
    }

    @Override
    public boolean supportsConvert() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsConvert'");
    }

    @Override
    public boolean supportsConvert(int fromType, int toType) throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsConvert'");
    }

    @Override
    public boolean supportsTableCorrelationNames() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsTableCorrelationNames'");
    }

    @Override
    public boolean supportsDifferentTableCorrelationNames() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsDifferentTableCorrelationNames'");
    }

    @Override
    public boolean supportsExpressionsInOrderBy() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsExpressionsInOrderBy'");
    }

    @Override
    public boolean supportsOrderByUnrelated() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsOrderByUnrelated'");
    }

    @Override
    public boolean supportsGroupBy() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsGroupBy'");
    }

    @Override
    public boolean supportsGroupByUnrelated() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsGroupByUnrelated'");
    }

    @Override
    public boolean supportsGroupByBeyondSelect() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsGroupByBeyondSelect'");
    }

    @Override
    public boolean supportsLikeEscapeClause() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsLikeEscapeClause'");
    }

    @Override
    public boolean supportsMultipleResultSets() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsMultipleResultSets'");
    }

    @Override
    public boolean supportsMultipleTransactions() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsMultipleTransactions'");
    }

    @Override
    public boolean supportsNonNullableColumns() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsNonNullableColumns'");
    }

    @Override
    public boolean supportsMinimumSQLGrammar() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsMinimumSQLGrammar'");
    }

    @Override
    public boolean supportsCoreSQLGrammar() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsCoreSQLGrammar'");
    }

    @Override
    public boolean supportsExtendedSQLGrammar() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsExtendedSQLGrammar'");
    }

    @Override
    public boolean supportsANSI92EntryLevelSQL() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsANSI92EntryLevelSQL'");
    }

    @Override
    public boolean supportsANSI92IntermediateSQL() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsANSI92IntermediateSQL'");
    }

    @Override
    public boolean supportsANSI92FullSQL() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsANSI92FullSQL'");
    }

    @Override
    public boolean supportsIntegrityEnhancementFacility() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsIntegrityEnhancementFacility'");
    }

    @Override
    public boolean supportsOuterJoins() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsOuterJoins'");
    }

    @Override
    public boolean supportsFullOuterJoins() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsFullOuterJoins'");
    }

    @Override
    public boolean supportsLimitedOuterJoins() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsLimitedOuterJoins'");
    }

    @Override
    public String getSchemaTerm() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getSchemaTerm'");
    }

    @Override
    public String getProcedureTerm() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getProcedureTerm'");
    }

    @Override
    public String getCatalogTerm() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getCatalogTerm'");
    }

    @Override
    public boolean isCatalogAtStart() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'isCatalogAtStart'");
    }

    @Override
    public String getCatalogSeparator() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getCatalogSeparator'");
    }

    @Override
    public boolean supportsSchemasInDataManipulation() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsSchemasInDataManipulation'");
    }

    @Override
    public boolean supportsSchemasInProcedureCalls() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsSchemasInProcedureCalls'");
    }

    @Override
    public boolean supportsSchemasInTableDefinitions() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsSchemasInTableDefinitions'");
    }

    @Override
    public boolean supportsSchemasInIndexDefinitions() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsSchemasInIndexDefinitions'");
    }

    @Override
    public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsSchemasInPrivilegeDefinitions'");
    }

    @Override
    public boolean supportsCatalogsInDataManipulation() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsCatalogsInDataManipulation'");
    }

    @Override
    public boolean supportsCatalogsInProcedureCalls() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsCatalogsInProcedureCalls'");
    }

    @Override
    public boolean supportsCatalogsInTableDefinitions() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsCatalogsInTableDefinitions'");
    }

    @Override
    public boolean supportsCatalogsInIndexDefinitions() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsCatalogsInIndexDefinitions'");
    }

    @Override
    public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsCatalogsInPrivilegeDefinitions'");
    }

    @Override
    public boolean supportsPositionedDelete() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsPositionedDelete'");
    }

    @Override
    public boolean supportsPositionedUpdate() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsPositionedUpdate'");
    }

    @Override
    public boolean supportsSelectForUpdate() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsSelectForUpdate'");
    }

    @Override
    public boolean supportsStoredProcedures() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsStoredProcedures'");
    }

    @Override
    public boolean supportsSubqueriesInComparisons() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsSubqueriesInComparisons'");
    }

    @Override
    public boolean supportsSubqueriesInExists() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsSubqueriesInExists'");
    }

    @Override
    public boolean supportsSubqueriesInIns() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsSubqueriesInIns'");
    }

    @Override
    public boolean supportsSubqueriesInQuantifieds() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsSubqueriesInQuantifieds'");
    }

    @Override
    public boolean supportsCorrelatedSubqueries() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsCorrelatedSubqueries'");
    }

    @Override
    public boolean supportsUnion() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsUnion'");
    }

    @Override
    public boolean supportsUnionAll() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsUnionAll'");
    }

    @Override
    public boolean supportsOpenCursorsAcrossCommit() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsOpenCursorsAcrossCommit'");
    }

    @Override
    public boolean supportsOpenCursorsAcrossRollback() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsOpenCursorsAcrossRollback'");
    }

    @Override
    public boolean supportsOpenStatementsAcrossCommit() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsOpenStatementsAcrossCommit'");
    }

    @Override
    public boolean supportsOpenStatementsAcrossRollback() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsOpenStatementsAcrossRollback'");
    }

    @Override
    public int getMaxBinaryLiteralLength() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getMaxBinaryLiteralLength'");
    }

    @Override
    public int getMaxCharLiteralLength() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getMaxCharLiteralLength'");
    }

    @Override
    public int getMaxColumnNameLength() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getMaxColumnNameLength'");
    }

    @Override
    public int getMaxColumnsInGroupBy() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getMaxColumnsInGroupBy'");
    }

    @Override
    public int getMaxColumnsInIndex() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getMaxColumnsInIndex'");
    }

    @Override
    public int getMaxColumnsInOrderBy() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getMaxColumnsInOrderBy'");
    }

    @Override
    public int getMaxColumnsInSelect() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getMaxColumnsInSelect'");
    }

    @Override
    public int getMaxColumnsInTable() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getMaxColumnsInTable'");
}

    @Override
    public int getMaxConnections() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getMaxConnections'");
    }

    @Override
    public int getMaxCursorNameLength() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getMaxCursorNameLength'");
    }

    @Override
    public int getMaxIndexLength() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getMaxIndexLength'");
    }

    @Override
    public int getMaxSchemaNameLength() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getMaxSchemaNameLength'");
    }

    @Override
    public int getMaxProcedureNameLength() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getMaxProcedureNameLength'");
    }

    @Override
    public int getMaxCatalogNameLength() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getMaxCatalogNameLength'");
    }

    @Override
    public int getMaxRowSize() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getMaxRowSize'");
    }

    @Override
    public boolean doesMaxRowSizeIncludeBlobs() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'doesMaxRowSizeIncludeBlobs'");
    }

    @Override
    public int getMaxStatementLength() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getMaxStatementLength'");
    }

    @Override
    public int getMaxStatements() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getMaxStatements'");
    }

    @Override
    public int getMaxTableNameLength() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getMaxTableNameLength'");
    }

    @Override
    public int getMaxTablesInSelect() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getMaxTablesInSelect'");
    }

    @Override
    public int getMaxUserNameLength() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getMaxUserNameLength'");
    }

    @Override
    public int getDefaultTransactionIsolation() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getDefaultTransactionIsolation'");
    }

    @Override
    public boolean supportsTransactions() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsTransactions'");
    }

    @Override
    public boolean supportsTransactionIsolationLevel(int level) throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsTransactionIsolationLevel'");
    }

    @Override
    public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsDataDefinitionAndDataManipulationTransactions'");
    }

    @Override
    public boolean supportsDataManipulationTransactionsOnly() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsDataManipulationTransactionsOnly'");
    }

    @Override
    public boolean dataDefinitionCausesTransactionCommit() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'dataDefinitionCausesTransactionCommit'");
    }

    @Override
    public boolean dataDefinitionIgnoredInTransactions() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'dataDefinitionIgnoredInTransactions'");
    }

    @Override
    public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern)
            throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getProcedures'");
    }

    @Override
    public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern,
            String columnNamePattern) throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getProcedureColumns'");
    }

    @Override
    public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types)
            throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getTables'");
    }

    @Override
    public ResultSet getSchemas() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getSchemas'");
    }

    @Override
    public ResultSet getCatalogs() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getCatalogs'");
    }

    @Override
    public ResultSet getTableTypes() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getTableTypes'");
    }

    @Override
    public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern)
            throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getColumns'");
    }

    @Override
    public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern)
            throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getColumnPrivileges'");
    }

    @Override
    public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern)
            throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getTablePrivileges'");
    }

    @Override
    public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable)
            throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getBestRowIdentifier'");
    }

    @Override
    public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getVersionColumns'");
    }

    @Override
    public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getPrimaryKeys'");
    }

    @Override
    public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getImportedKeys'");
    }

    @Override
    public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getExportedKeys'");
    }

    @Override
    public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable,
            String foreignCatalog, String foreignSchema, String foreignTable) throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getCrossReference'");
    }

    @Override
    public ResultSet getTypeInfo() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getTypeInfo'");
    }

    @Override
    public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate)
            throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getIndexInfo'");
    }

    @Override
    public boolean supportsResultSetType(int type) throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsResultSetType'");
    }

    @Override
    public boolean supportsResultSetConcurrency(int type, int concurrency) throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsResultSetConcurrency'");
    }

    @Override
    public boolean ownUpdatesAreVisible(int type) throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'ownUpdatesAreVisible'");
    }

    @Override
    public boolean ownDeletesAreVisible(int type) throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'ownDeletesAreVisible'");
    }

    @Override
    public boolean ownInsertsAreVisible(int type) throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'ownInsertsAreVisible'");
    }

    @Override
    public boolean othersUpdatesAreVisible(int type) throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'othersUpdatesAreVisible'");
    }

    @Override
    public boolean othersDeletesAreVisible(int type) throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'othersDeletesAreVisible'");
    }

    @Override
    public boolean othersInsertsAreVisible(int type) throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'othersInsertsAreVisible'");
    }

    @Override
    public boolean updatesAreDetected(int type) throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'updatesAreDetected'");
    }

    @Override
    public boolean deletesAreDetected(int type) throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'deletesAreDetected'");
    }

    @Override
    public boolean insertsAreDetected(int type) throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'insertsAreDetected'");
    }

    @Override
    public boolean supportsBatchUpdates() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsBatchUpdates'");
    }

    @Override
    public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types)
            throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getUDTs'");
    }

    @Override
    public Connection getConnection() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getConnection'");
    }

    @Override
    public boolean supportsSavepoints() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsSavepoints'");
    }

    @Override
    public boolean supportsNamedParameters() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsNamedParameters'");
    }

    @Override
    public boolean supportsMultipleOpenResults() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsMultipleOpenResults'");
    }

    @Override
    public boolean supportsGetGeneratedKeys() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsGetGeneratedKeys'");
    }

    @Override
    public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getSuperTypes'");
    }

    @Override
    public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getSuperTables'");
    }

    @Override
    public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern,
            String attributeNamePattern) throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getAttributes'");
    }

    @Override
    public boolean supportsResultSetHoldability(int holdability) throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsResultSetHoldability'");
    }

    @Override
    public int getResultSetHoldability() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getResultSetHoldability'");
    }

    @Override
    public int getDatabaseMajorVersion() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getDatabaseMajorVersion'");
    }

    @Override
    public int getDatabaseMinorVersion() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getDatabaseMinorVersion'");
    }

    @Override
    public int getJDBCMajorVersion() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getJDBCMajorVersion'");
    }

    @Override
    public int getJDBCMinorVersion() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getJDBCMinorVersion'");
    }

    @Override
    public int getSQLStateType() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getSQLStateType'");
    }

    @Override
    public boolean locatorsUpdateCopy() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'locatorsUpdateCopy'");
    }

    @Override
    public boolean supportsStatementPooling() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsStatementPooling'");
    }

    @Override
    public RowIdLifetime getRowIdLifetime() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getRowIdLifetime'");
    }

    @Override
    public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getSchemas'");
    }

    @Override
    public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'supportsStoredFunctionsUsingCallSyntax'");
    }

    @Override
    public boolean autoCommitFailureClosesAllResultSets() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'autoCommitFailureClosesAllResultSets'");
    }

    @Override
    public ResultSet getClientInfoProperties() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getClientInfoProperties'");
    }

    @Override
    public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern)
            throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getFunctions'");
    }

    @Override
    public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern,
            String columnNamePattern) throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getFunctionColumns'");
    }

    @Override
    public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern,
            String columnNamePattern) throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'getPseudoColumns'");
    }

    @Override
    public boolean generatedKeyAlwaysReturned() throws SQLException
    {
        throw new UnsupportedOperationException("Unimplemented method 'generatedKeyAlwaysReturned'");
    }

}
