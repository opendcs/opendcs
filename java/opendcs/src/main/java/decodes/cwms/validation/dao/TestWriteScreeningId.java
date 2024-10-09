package decodes.cwms.validation.dao;

import decodes.cwms.validation.Screening;
import decodes.sql.DbKey;
import decodes.tsdb.TsdbAppTemplate;

public class TestWriteScreeningId extends TsdbAppTemplate
{

	public TestWriteScreeningId()
	{
		super("TestWriteScreeningId");
	}

	public static void main(String[] args)
		throws Exception
	{
		TestWriteScreeningId app = new TestWriteScreeningId();
		app.execute(args);
	}

	@Override
	protected void runApp() throws Exception
	{
		System.out.print("Enter unique Screening ID: ");
		String id = System.console().readLine();
		
		System.out.print("Enter description: ");
		String desc = System.console().readLine();
		
		System.out.print("Enter units: ");
		String units = System.console().readLine();

		Screening screening = new Screening(DbKey.NullKey, id, desc, units);
		
		System.out.print("Enter param: ");
		screening.setParamId(System.console().readLine());
		
		System.out.print("Enter param type: ");
		screening.setParamTypeId(System.console().readLine());
		
		System.out.print("Enter duration: ");
		screening.setDurationId(System.console().readLine());
		
		ScreeningDAO dao = new ScreeningDAO(theDb);
		dao.writeScreening(screening);
	}

}
