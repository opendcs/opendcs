package decodes.tsdb.test;

import java.util.Iterator;
import java.util.List;
import java.util.Enumeration;

import opendcs.dai.AlgorithmDAI;
import decodes.tsdb.*;
import decodes.util.CmdLineArgs;

public class ListAlgo extends TestProg
{
	public static void main(String args[])
		throws Exception
	{
		TestProg tp = new ListAlgo();
		tp.execute(args);
	}

	public ListAlgo()
	{
		super(null);
	}

	protected void runTest()
		throws Exception
	{
		List<String> algos = theDb.listAlgorithms();
		System.out.println("" + algos.size() + " Algorithm Names Retrieved:");
		for(String algo : algos)
			System.out.println("\t" + algo);

		System.out.println("Retrieving & Displaying Algorithm Data:");
	
		AlgorithmDAI algorithmDAO = theDb.makeAlgorithmDAO();
		for(String algo : algos)
		{
			DbCompAlgorithm dca = algorithmDAO.getAlgorithm(algo);
			System.out.println("Algo '" + dca.getName() + "' id="
				+ dca.getId() + " class=" + dca.getExecClass());
			System.out.println("\t comment: " + dca.getComment());
			Iterator<DbAlgoParm> parms = dca.getParms();
			while(parms.hasNext())
			{
				DbAlgoParm parm = parms.next();
				System.out.println("\tParm: " + parm.getRoleName()
					+ " : " + parm.getParmType());
			}
			for(Enumeration pe = dca.getPropertyNames();
				pe.hasMoreElements(); )
			{
				String nm = (String)pe.nextElement();
				System.out.println("\tProp: " + nm + "="
					+ dca.getProperty(nm));
			}
		}
		algorithmDAO.close();
	}
}
