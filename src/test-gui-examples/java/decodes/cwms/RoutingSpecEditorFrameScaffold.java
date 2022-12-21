package decodes.cwms;

import java.util.Properties;
import java.util.jar.JarFile;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import decodes.cwms.CwmsTimeSeriesDb;
import decodes.db.Database;
import decodes.db.DatabaseIO;
import decodes.dbeditor.DbEditorFrame;
import decodes.dbeditor.DecodesDbEditor;
import decodes.dbeditor.RoutingSpecRunGuiFrame;
import decodes.routmon2.RoutingMonitor;
import decodes.routmon2.RoutingMonitorFrame;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.DecodesSettings;
import ilex.util.LoadResourceBundle;
import ilex.util.Logger;
import lrgs.gui.DecodesInterface;

final class RoutingSpecEditorFrameScaffold
{
	public static void main(String[] args) throws Exception
	{
		DecodesInterface.setGUI(true);
		System.setProperty("DCSTOOL_HOME", "J:/OpenDCS/DevInstaller/github");
		CwmsTimeSeriesDb cwmsTimeSeriesDb = new CwmsTimeSeriesDb();
		String dbUri = "jdbc:oracle:thin:@10.0.0.36:1539:V122SWT1811CCPT";
		cwmsTimeSeriesDb.setDbUri(dbUri);
		Properties properties = new Properties();
		properties.setProperty("username", "M5HECTEST_CCP_M");
		properties.setProperty("password", "swt1811db");
		DecodesSettings.instance().CwmsOfficeId = "SWT";
		cwmsTimeSeriesDb.connect("compproc_regtest", properties);
		Database db = new Database();
		Database.setDb(db);
		DecodesSettings settings = DecodesSettings.instance();
		DatabaseIO editDbio = DatabaseIO.makeDatabaseIO(DecodesSettings.DB_CWMS, dbUri);
		db.setDbIo(editDbio);
		db.read();
		TsdbAppTemplate.theDb = cwmsTimeSeriesDb;
//		DecodesDbEditor decodesDbEditor = new DecodesDbEditor();
		RoutingSpecRunGuiFrame routingSpecRunGuiFrame = new RoutingSpecRunGuiFrame();
		routingSpecRunGuiFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		SwingUtilities.invokeLater(()->
		{
			routingSpecRunGuiFrame.setVisible(true);
		});

		//		RoutingMonitor routmon = new RoutingMonitor();
//		try
//		{
//			routmon.execute(new String[0]);
//			routmon.setExitOnClose(true);
//		}
//		catch(Exception e)
//		{
//			throw new RuntimeException(e);
//		}
	}
}
