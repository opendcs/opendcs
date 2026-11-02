package decodes.cwms.rating;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import decodes.cwms.CwmsTsId;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbCompException;
import decodes.tsdb.ParmRef;

/**
 * Tests how CwmsRatingMultIndep decides which independent parameters take part in a rating.
 */
class CwmsRatingMultIndepTest
{
    /**
     * The algorithm with its parameter map replaced by a role-name lookup, so that no
     * database is needed.
     */
    private static class TestAlgorithm extends CwmsRatingMultIndep
    {
        private final Map<String, ParmRef> parmRefs = new HashMap<>();

        @Override
        public ParmRef getParmRef(String role)
        {
            return parmRefs.get(role);
        }

        void map(String role, String locationId, String param)
        {
            CwmsTsId tsId = new CwmsTsId();
            tsId.setSiteName(locationId);
            tsId.setDataType(new DataType(Constants.datatype_CWMS, param));
            parmRefs.put(role, new ParmRef(role, null, new CTimeSeries(tsId)));
        }

        /** Stands in for a parameter row that's defined in the computation but unused. */
        void mapUnused(String role)
        {
            parmRefs.put(role, new ParmRef(role, null, null));
        }
    }

    @Test
    void independentCountIsNotInheritedFromThePreviousRun()
        throws DbCompException
    {
        TestAlgorithm algo = new TestAlgorithm();
        algo.map("indep1", "Loc", "Stage");
        algo.map("indep2", "Loc", "Invert");
        algo.map("indep3", "Loc", "Storage");

        assertEquals("Stage,Invert,Storage", algo.buildIndepSpec());
        assertEquals(3, algo.numIndeps);

        // The executive instance is reused for each run of a computation, so a later run
        // with fewer mapped inputs must not keep the wider parameter list.
        algo.parmRefs.remove("indep2");
        algo.parmRefs.remove("indep3");

        assertEquals("Stage", algo.buildIndepSpec());
        assertEquals(1, algo.numIndeps);
    }

    @Test
    void unusedParametersAreNotAddedToTheSpec()
        throws DbCompException
    {
        TestAlgorithm algo = new TestAlgorithm();
        algo.map("indep1", "Loc", "Stage");
        algo.map("indep2", "Loc", "Invert");
        algo.mapUnused("indep3");

        assertEquals("Stage,Invert", algo.buildIndepSpec());
        assertEquals(2, algo.numIndeps);
    }

    @Test
    void unmappedFirstParameterFails()
    {
        TestAlgorithm algo = new TestAlgorithm();
        algo.mapUnused("indep1");

        assertThrows(DbCompException.class, () -> algo.buildIndepSpec());
    }

    @Test
    void ratingParameterCountWinsOverMappedInputs()
        throws DbCompException
    {
        assertEquals(2, CwmsRatingMultIndep.validateIndepCount("Loc.Stage,Invert;Flow.T.US",
                                                               "MyComp", 2, 2));
    }

    @Test
    void extraMappedInputsFailInsteadOfWideningTheValueSets()
    {
        DbCompException ex = assertThrows(DbCompException.class,
                () -> CwmsRatingMultIndep.validateIndepCount("Loc.Stage;Flow.T.US", "MyComp", 3, 1));

        assertTrue(ex.getMessage().contains("takes 1"), ex.getMessage());
        assertTrue(ex.getMessage().contains("maps 3"), ex.getMessage());
    }

    @Test
    void unknownRatingParameterCountKeepsTheMappedInputs()
        throws DbCompException
    {
        assertEquals(3, CwmsRatingMultIndep.validateIndepCount("Loc.Stage;Flow.T.US",
                                                               "MyComp", 3, -1));
    }
}
