package decodes.polling.pakbus;

import java.net.Socket;
import java.util.List;

import com.campbellsci.pakbus.ColumnDef;
import com.campbellsci.pakbus.DataCollectClient;
import com.campbellsci.pakbus.DataCollectModeMostRecent;
import com.campbellsci.pakbus.DataCollectTran;
import com.campbellsci.pakbus.Datalogger;
import com.campbellsci.pakbus.GetTableDefsClient;
import com.campbellsci.pakbus.GetTableDefsTran;
import com.campbellsci.pakbus.Network;
import com.campbellsci.pakbus.Record;
import com.campbellsci.pakbus.TableDef;
import com.campbellsci.pakbus.ValueBase;

public class TestDataCollect1
	implements DataCollectClient, GetTableDefsClient
{
	private Network network = null;
	private Socket socket = null;
	private boolean complete = false;
	private Datalogger logger = null;
	private String tableName = null;
	
	private String host = null;
	private int port = 0;
	private int pakbusId = 0;
	private long tableDefsStart = 0L;
	private long getDataStart = 0L;

	public void run()
		throws Exception
	{
		// create the connection and the network
		socket = new Socket(host, port);
		network = new Network((short)4079, socket.getInputStream(), socket.getOutputStream());

		// create the station
		logger = new Datalogger((short)pakbusId);
		network.add_station(logger);

		// we first need the table definitions. We'll wait to query
		// until the table definitions have been read
		tableDefsStart = System.currentTimeMillis();
		logger.add_transaction(new GetTableDefsTran(this));

		// now drive the network
		int active_links = 0;
		complete = false;
		while (!complete || active_links > 0)
		{
			active_links = network.check_state();
			Thread.sleep(100);
		}

	}

	public static void main(String args[])
		throws Exception
	{
		if (args.length < 4)
		{
			System.err.println("Usage: ... host port pakbusId tableName [skip]");
			System.exit(1);
		}
		
		TestDataCollect1 theApp = new TestDataCollect1();
		theApp.host = args[0];
		theApp.port = Integer.parseInt(args[1]);
		theApp.pakbusId = Integer.parseInt(args[2]);
		theApp.tableName = args[3];
		
		theApp.run();
		
	}

	@Override
	public void on_complete(GetTableDefsTran transaction, int outcome)
		throws Exception
	{
		System.out.println("on_complete(GetTableDefsTran) outcome=" + outcome
			+ " " + GetTableDefsTran.describe_outcome(outcome));
		if (outcome == GetTableDefsTran.outcome_success)
		{
			System.out.println("GetTableDefs success, elapsed ms=" 
				+ (System.currentTimeMillis() - tableDefsStart));
			printTableDefs();
			// start the data collection transaction
			getDataStart = System.currentTimeMillis();
			logger.add_transaction(
				new DataCollectTran(tableName, this, new DataCollectModeMostRecent(1)));
		}
		else
		{
			System.out.println("get table defs failed");
			complete = true;
		}
	}

	@Override
	public void on_complete(DataCollectTran transaction, int outcome)
		throws Exception
	{
		if (outcome == DataCollectTran.outcome_success)
		{
			System.out.println("Data collection succeeded, elapsed ms=" +
				(System.currentTimeMillis()-getDataStart));
		}
		else
			System.out.println("data collection failed");
		complete = true;
	}

	@Override
	public boolean on_records(DataCollectTran transaction, List<Record> records)
	{
		for (Record record : records)
		{
			System.out.print(record.get_time_stamp().format("\"%y-%m-%d %H:%M:%S%x\","));
			System.out.print(record.get_record_no());
			for (ValueBase value : record.get_values())
				System.out.print("," + value.format());
			System.out.println("");
		}
		return true; // a value of false would cause the transaction to abort
	}
	
	private void printTableDefs()
	{
		int ntables = logger.get_tables_count();
		System.out.println("There are " + ntables + " tables.");
		for(int idx = 0; idx < ntables; idx++)
		{
			int tableNum = idx + 1;
			System.out.print("\tTable " + tableNum + "");
			TableDef tableDef = logger.get_table(tableNum);
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
