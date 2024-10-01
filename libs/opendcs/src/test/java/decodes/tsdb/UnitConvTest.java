package decodes.tsdb;

import java.util.List;

import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.EngineeringUnit;
import decodes.db.UnitConverter;
import decodes.db.UnitConverterDb;
import mil.army.usace.hec.metadata.UnitUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UnitConvTest
{
	public static final String module = "UnitConvTest";

	@BeforeAll
	public static void setupTestUnitConversions() throws Exception
	{
		//Uses standard HEC library of units as a mock DB datasource for unit conversions
		//For an integration test, convert logic to connect to integration database
		Database database = new Database();
		Database.setDb(database);
		String[] availableUnits = UnitUtil.getAvailableUnits();
		for(String unit : availableUnits)
		{
			List<String> allConvertTo = UnitUtil.getAllUnitsThatCanConvertTo(unit, UnitUtil.ENGLISH);
			for(String convertTo : allConvertTo)
			{
				UnitConverterDb unitConverterDb = new UnitConverterDb(unit, convertTo);
				unitConverterDb.algorithm = Constants.eucvt_linear;
				double scalarFactor = UnitUtil.getScalarFactor(unit, convertTo);
				unitConverterDb.coefficients = new double[]{scalarFactor, 0.0};
				database.unitConverterSet.addDbConverter(unitConverterDb);
			}
			allConvertTo = UnitUtil.getAllUnitsThatCanConvertTo(unit, UnitUtil.SI);
			for(String convertTo : allConvertTo)
			{
				UnitConverterDb unitConverterDb = new UnitConverterDb(unit, convertTo);
				unitConverterDb.algorithm = Constants.eucvt_linear;
				database.unitConverterSet.addDbConverter(unitConverterDb);
			}
		}
	}

	@ParameterizedTest
	@CsvSource({"123.456, ft, m, 37.6293888"})
	public void testLinearUnitConverter(double input, String fromUnits, String toUnits, double expect) throws Exception
	{
		EngineeringUnit euFrom = EngineeringUnit.getEngineeringUnit(fromUnits);
		EngineeringUnit euTo = EngineeringUnit.getEngineeringUnit(toUnits);
		UnitConverter unitConverter = Database.getDb().unitConverterSet.get(euFrom, euTo);
		assertEquals(expect, unitConverter.convert(input), 0.0);
	}
}
