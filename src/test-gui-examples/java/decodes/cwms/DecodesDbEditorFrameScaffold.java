package decodes.cwms;

import java.util.Properties;

import decodes.cwms.CwmsTimeSeriesDb;
import decodes.db.Database;
import decodes.db.DatabaseIO;
import decodes.dbeditor.DecodesDbEditor;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.DecodesSettings;
import lrgs.gui.DecodesInterface;

final class DecodesDbEditorFrameScaffold
{
	public static void main(String[] args) throws Exception
	{
		DecodesInterface.setGUI(true);
		System.setProperty("DCSTOOL_HOME", "");
		CwmsTimeSeriesDb cwmsTimeSeriesDb = new CwmsTimeSeriesDb();
		String dbUri = "";
		cwmsTimeSeriesDb.setDbUri(dbUri);
		Properties properties = new Properties();
		properties.setProperty("username", "");
		properties.setProperty("password", "");
		DecodesSettings.instance().CwmsOfficeId = "";
		cwmsTimeSeriesDb.connect("compproc_regtest", properties);
		Database db = new Database();
		Database.setDb(db);
		DecodesSettings settings = DecodesSettings.instance();
		DatabaseIO editDbio = DatabaseIO.makeDatabaseIO(DecodesSettings.DB_CWMS, dbUri);
		db.setDbIo(editDbio);
		db.read();
		TsdbAppTemplate.theDb = cwmsTimeSeriesDb;
		DecodesDbEditor decodesDbEditor = new DecodesDbEditor();
	}
}
