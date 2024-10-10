package decodes.tsdb.groupedit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.swing.SwingUtilities;

import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.CwmsTsId;
import decodes.db.Database;
import decodes.db.DatabaseIO;
import decodes.tsdb.BadConnectException;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.DecodesSettings;

final class TsDbGrpEditorFrameScaffold
{
	public static void main(String[] args) throws Exception
	{
		final CwmsTimeSeriesDb cwmsTimeSeriesDb = new CwmsTimeSeriesDb();
		cwmsTimeSeriesDb.setDbUri("");
		Properties properties = new Properties();
		properties.setProperty("username", "");
		properties.setProperty("password", "");
		DecodesSettings.instance().CwmsOfficeId = "SWT";
		cwmsTimeSeriesDb.connect("compproc_regtest", properties);
		Database db = new decodes.db.Database();
		Database.setDb(db);
		DecodesSettings settings = DecodesSettings.instance();
		DatabaseIO editDbio = DatabaseIO.makeDatabaseIO(settings.editDatabaseTypeCode, settings.editDatabaseLocation);
		db.setDbIo(editDbio);
		db.read();
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				TsdbAppTemplate.theDb = cwmsTimeSeriesDb;
				TsDbGrpEditorFrame tsDbGrpEditorFrame = new TsDbGrpEditorFrame(TsdbAppTemplate.theDb);
				tsDbGrpEditorFrame.setVisible(true);
			}
		});
	}
}
