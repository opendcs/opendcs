package decodes.polling.pakbus;

import ilex.util.FileUtil;

import java.io.File;

import com.campbellsci.pakbus.ColumnDef;
import com.campbellsci.pakbus.Datalogger;
import com.campbellsci.pakbus.Network;
import com.campbellsci.pakbus.Packet;
import com.campbellsci.pakbus.TableDef;

import decodes.tsdb.IntervalCodes;
import decodes.tsdb.IntervalIncrement;
import decodes.util.DecodesSettings;

public class PrintTableDefs
{

	public static void main(String args[])
		throws Exception
	{
		if (args.length == 0)
		{
			System.err.println("Usage java .... PrintTableDefs <filename>");
			System.exit(1);
		}
		Network pbNetwork = new Network((short) 1, null, null);
		Datalogger pbLogger = new Datalogger((short)0);
		pbNetwork.add_station(pbLogger);
		
		File cf = new File(args[0]);
		if (!cf.canRead())
		{
			String msg = "Cached table def '" + cf.getPath() + "' doesn't exist or is not readable.";
			System.err.println(msg);
			System.exit(1);
		}
		
		// Else file exists and is recent enough to use.
		byte[] contents = FileUtil.getfileBytes(cf);
		Packet packet = new Packet();
		packet.add_bytes(contents, contents.length);
		pbLogger.set_raw_table_defs(packet);
		
		int ntables = pbLogger.get_tables_count();
		System.out.println("There are " + ntables + " tables.");
		for(int idx = 0; idx < ntables; idx++)
		{
			int tableNum = idx + 1;
			System.out.print("\tTable " + tableNum + "");
			TableDef tableDef = pbLogger.get_table(tableNum);
			System.out.println(" name=" + tableDef.name
				+ ", interval=" + tableDef.interval + ", numCols=" + tableDef.columns.size());
			for(ColumnDef cd : tableDef.columns)
			{
				System.out.println("\t\t" + cd.column_no + ": " + cd.name
					+ " dataType=" + cd.data_type + ", units=" + cd.units
					+ ", #values=" + cd.get_values_count());
			}
		}
	}
}
