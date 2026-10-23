package org.opendcs.database.impl.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import org.junit.jupiter.api.Test;

import decodes.db.EngineeringUnit;
import decodes.db.LinearConverter;
import decodes.db.UnitConverter;

class UnitConverterDaoImplTest
{
    @Test
    void cycleWithUnreachableTargetShouldStop()
    {
        var meters = new EngineeringUnit("m", "meters", "Metric", "Length");
        var feet = new EngineeringUnit("ft", "feet", "English", "Length");
        var seconds = new EngineeringUnit("s", "seconds", "Metric", "Time");
        var converters = List.<UnitConverter>of(new LinearConverter(meters, feet), new LinearConverter(feet, meters));
        var solutions = new ArrayList<UnitConverter>();

        // Meters -> feet -> meters must stop when there is no route to seconds.
        // Before the fix, the search repeats this cycle until it throws StackOverflowError.
        assertDoesNotThrow(() -> UnitConverterDaoImpl.search(solutions, converters,
            meters, seconds, new Stack<UnitConverter>(), new HashSet<String>()));
        assertTrue(solutions.isEmpty());
    }
}
