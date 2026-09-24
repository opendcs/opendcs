package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.cwms.validation.AbsCheck;
import decodes.cwms.validation.Screening;
import decodes.cwms.validation.ScreeningCriteria;
import decodes.cwms.validation.ScreeningImport;
import decodes.cwms.validation.dao.ScreeningDAI;
import decodes.sql.DbKey;
import decodes.tsdb.TimeSeriesDb;

/**
 * Runs the ScreeningImport program against each time series database. Only CWMS stores screenings,
 * so the other databases must fail with a clear error instead of a ClassCastException.
 */
class ScreeningImportTestIT extends AppTestBase
{
    /** CWMS screening ids are limited to 16 characters. Upper case so id normalization cannot affect the compare. */
    private static final String SCREENING_NAME = "IMPORT-IT.STAGE";

    private static final String SCREENING_DESC = "Created by ScreeningImportTestIT";

    private static final String SCREENING_FILE = String.join("\n",
        "SCREENING " + SCREENING_NAME,
        "DESC " + SCREENING_DESC,
        "PARAM Stage",
        "PARAMTYPE Inst",
        "DURATION 0",
        "UNITS m",
        "RANGE_ACTIVE true",
        "ROC_ACTIVE false",
        "CONST_ACTIVE false",
        "DURMAG_ACTIVE false",
        "",
        "CRITERIA_SET",
        "CRITERIA ABS R 0 45",
        "CRITERIA ABS Q 2.6 16.4",
        "CRITERIA_SET_END",
        "SCREENING_END",
        "");

    @ConfiguredField
    private TimeSeriesDb tsDb;

    @TempDir
    private File tempDir;

    @Test
    @EnableIfTsDb({"CWMS-Oracle"})
    void test_import_writes_screening() throws Exception
    {
        try (ScreeningDAI screeningDao = tsDb.makeScreeningDAO())
        {
            try
            {
                runImport(writeScreeningFile());
                assertExitNullOrZero();

                // ScreeningImport logs write failures instead of exiting non-zero, so read the screening back.
                screeningDao.clearCache();
                final DbKey key = screeningDao.getKeyForId(SCREENING_NAME);
                assertFalse(DbKey.isNull(key), "Imported screening was not saved.");

                final Screening screening = screeningDao.getByKey(key);
                assertEquals(SCREENING_NAME, screening.getScreeningName());
                // DESC used to be parsed and discarded, so the description never reached the database.
                assertEquals(SCREENING_DESC, screening.getScreeningDesc());
                assertEquals("Stage", screening.getParamId());
                assertTrue(screening.isRangeActive());
                assertFalse(screening.isRocActive());
                assertEquals(1, screening.getCriteriaSeasons().size());

                final ScreeningCriteria criteria = screening.getCriteriaSeasons().get(0);
                final AbsCheck reject = criteria.getAbsCheckFor('R');
                assertNotNull(reject, "Reject range check was not saved.");
                assertEquals(0.0, reject.getLow(), 1e-6);
                assertEquals(45.0, reject.getHigh(), 1e-6);
                final AbsCheck question = criteria.getAbsCheckFor('Q');
                assertNotNull(question, "Questionable range check was not saved.");
                assertEquals(2.6, question.getLow(), 1e-6);
                assertEquals(16.4, question.getHigh(), 1e-6);
            }
            finally
            {
                screeningDao.clearCache();
                final DbKey key = screeningDao.getKeyForId(SCREENING_NAME);
                if (!DbKey.isNull(key))
                {
                    screeningDao.deleteScreening(screeningDao.getByKey(key));
                }
            }
        }
    }

    @Test
    @EnableIfTsDb({"OpenDCS-Postgres", "OpenDCS-Oracle"})
    void test_import_unsupported_database() throws Exception
    {
        final File screeningFile = writeScreeningFile();
        final UnsupportedOperationException ex =
            assertThrows(UnsupportedOperationException.class, () -> runImport(screeningFile));
        assertTrue(ex.getMessage().contains("not supported"), "Unexpected message: " + ex.getMessage());
        assertExitNullOrZero();
    }

    private File writeScreeningFile() throws Exception
    {
        final File file = new File(tempDir, "import-it.screening");
        Files.writeString(file.toPath(), SCREENING_FILE, StandardCharsets.UTF_8);
        return file;
    }

    /** -y skips the confirmation prompt and -T skips time series assignments, which would need existing TSIDs. */
    private void runImport(File screeningFile) throws Exception
    {
        final File log = new File(configuration.getUserDir().getParentFile(), "screening-import-it.log");
        environment.execute(() ->
            exit.execute(() ->
                ScreeningImport.main(args(
                    "-l", log.getAbsolutePath(),
                    "-P", configuration.getPropertiesFile().getAbsolutePath(),
                    "-y", "-T", "-d3",
                    screeningFile.getAbsolutePath()))
            )
        );
    }
}
