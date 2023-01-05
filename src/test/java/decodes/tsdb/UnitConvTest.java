package decodes.tsdb;

import decodes.db.CompositeConverter;
import decodes.db.Database;
import decodes.db.EngineeringUnit;
import decodes.db.UnitConverter;

public class UnitConvTest 
	extends TsdbAppTemplate
{
	public static final String module = "UnitConvTest";
	
	public UnitConvTest()
	{
		super("util.log");
		appNameArg.setDefaultValue("utility");
	}

	@Override
	protected void runApp()
		throws Exception
	{
		while(true)
		{
			System.out.print("Enter Value fromUnits toUnits: ");
			String line = System.console().readLine();
			if (line == null)
				break;
			String words[] = line.split(" ");
			if (words.length != 3)
				continue;
			Double d = Double.parseDouble(words[0]);
			EngineeringUnit euFrom = EngineeringUnit.getEngineeringUnit(words[1]);
			EngineeringUnit euTo = EngineeringUnit.getEngineeringUnit(words[2]);
			UnitConverter uc = Database.getDb().unitConverterSet.get(euFrom, euTo);
			if (uc == null)
			{
				System.out.println("No conversion");
				continue;
			}
			String cvt = uc.getClass().getName();
			if (uc instanceof CompositeConverter)
				cvt = cvt + "(" + ((CompositeConverter)uc).getWeight() + ")";
			System.out.println("Converter: " + cvt);
			System.out.println("" + d + " " + euFrom.getAbbr() + " = " 
				+ uc.convert(d) + " " + euTo.getAbbr());
		}
	}

	public static void main(String args[])
		throws Exception
	{
		UnitConvTest tp = new UnitConvTest();
		tp.execute(args);
	}
	
	@Override
	public void createDatabase() {}
	
	@Override
	public void tryConnect() {}



}
